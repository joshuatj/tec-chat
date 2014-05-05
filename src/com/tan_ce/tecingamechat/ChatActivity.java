package com.tan_ce.tecingamechat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.R.color;
import android.app.Activity;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class ChatActivity extends Activity {
	protected static SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aaa", Locale.getDefault());
	protected static SimpleDateFormat dateFormat = new SimpleDateFormat("d LLL yyyy", Locale.getDefault());

	protected ChatHistory history;
	protected ChatServer server;

	protected int scrollTo = -1;
	/*
	 * When we need to scroll to a particular index, but don't know where,
	 * use this. If we are being restored it returns the index where we should
	 * scroll to. Otherwise, it returns the last index.
	 * 
	 * Hackish yes, but I couldn't think of any other way on short notice...
	 */
	protected int getRestoredIdx() {
		if (scrollTo != -1) {
			int ret = scrollTo;
			scrollTo = -1;
			return ret;
		} else {
			return history.nextIdx - 1;
		}
	}

	protected void showToast(String msg) {
		Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	public void loadPrev(View view) {
		if (history.earliestIdx == Integer.MAX_VALUE) {
			showToast("Loading...");
			return;
		}

		(new HistoryUpdator<Integer>(history.earliestIdx) {
			@Override
			protected Boolean doInBackground(Integer... params) {
				try {
					List<ChatMessage> ret = server.getHistory(history.earliestIdx - 50, 50);
					history.mergeHistory(ret);
				} catch (Exception e) {
					e.printStackTrace();
					return true;
				}

				return false;
			}
		}).execute();
	}

	/**
	 * Run when the user clicks "Send"
	 * 
	 * @param view
	 */
	public void sendMsg(View view) {
		EditText edit_msg = (EditText) findViewById(R.id.edit_msg);
		String msg = edit_msg.getText().toString();

		if (msg.isEmpty()) return;

		new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				try {
					server.sendMsg(params[0]);
				} catch (Exception e) {
					return e.getMessage();
				}
				return null;
			}

			@Override
			protected void onPostExecute(final String errorMsg) {
				if (errorMsg != null) {
					Log.e("ChatServer", errorMsg);
					showToast("Could not send message: " + errorMsg);
				}
			}

		}.execute(msg);

		edit_msg.setText("");
	}

	protected int getChatScrollOffset() {
		return findViewById(R.id.chat_container).getTop() +
				findViewById(R.id.chat_layout).getTop();
	}

	/**
	 * Prime the history store with the latest 50 messages
	 */
	protected void primeHistory() {
		// Prime the history with the latest entries
		(new AsyncTask<Integer, Void, Boolean>() {
			boolean needRegistration = false;

			@Override
			protected Boolean doInBackground(Integer... params) {
				try {
					List<ChatMessage> ret = server.getHistory();
					history.mergeHistory(ret);
				} catch (NeedRegistrationException e) {
					needRegistration = true;
					Log.w("ChatActivity", "Current auth_key not accepted");
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					return true;
				}

				return false;
			}

			@Override
			protected void onPostExecute(Boolean failed) {
				if (failed.booleanValue()) {
					if (needRegistration) {
						doLogin();
						return;
					}
					showToast("Failed to retrieve history from server");
				} else {
					// Update the view and show the most recent messages
					updateChatView();
					new Handler().post(new ChatScroller(history.nextIdx - 1));
				}
			}
		}).execute();
	}

	/**
	 * Run when the user forces a manual refresh
	 * 
	 * @param menu
	 */
	public void refreshHistory(MenuItem menu) {
		if (history.nextIdx == 0) {
			showToast("Loading...");
			return;
		}

		(new HistoryRefresher(getRestoredIdx())).execute();
	}

	protected ChatMessage viewToChatMessage(View v) {
		if (v instanceof ChatMessageUI) {
			return ((ChatMessageUI) v).chatMessage;
		} else if (v instanceof TextView) {
			TextView tv = (TextView) v;
			if ((tv.getTag() instanceof ChatMessage)) {
				return (ChatMessage) tv.getTag();
			}
		}
		return null;
	}

	/**
	 * Get the topmost chat message in view
	 */
	protected ChatMessage getVisibleChat() {
		ScrollView sl = (ScrollView) findViewById(R.id.chat_scroller);
		int scrollPos = sl.getScrollY()
				- getChatScrollOffset() - 5 /* margin for error */;

		LinearLayout cc = (LinearLayout) findViewById(R.id.chat_container);
		for (int i = 0; i < cc.getChildCount(); i++) {
			View v = cc.getChildAt(i);
			ChatMessage cm = viewToChatMessage(v);
			if (v.getTop() > scrollPos && cm != null) {
				return cm;
			}
		}

		return null;
	}

	/**
	 * Given a chat index, get its position in the scrollable region
	 */
	protected int getChatTop(int idx) {
		int offset = getChatScrollOffset();

		LinearLayout cc = (LinearLayout) findViewById(R.id.chat_container);
		for (int i = 0; i < cc.getChildCount(); i++) {
			View v = cc.getChildAt(i);
			ChatMessage cm = viewToChatMessage(v);
			if (cm != null && cm.getIdx() == idx) {
				return v.getTop() + offset;
			}
		}

		return -1;
	}

	/**
	 * Scrolls the chat view to the message with the given index
	 * @param cm
	 */
	protected class ChatScroller implements Runnable {
		protected int idx;

		ChatScroller(int chatIdx) {
			idx = chatIdx;
		}

		@Override
		public void run() {
			if (idx < 0) {
				Log.w("ChatScroller", "Invoked with index < 0");
				return;
			}

			int offset = getChatScrollOffset();

			LinearLayout cc = (LinearLayout) findViewById(R.id.chat_container);
			for (int i = 0; i < cc.getChildCount(); i++) {
				View v = cc.getChildAt(i);
				ChatMessage cm = viewToChatMessage(v);

				if (cm != null && cm.getIdx() == idx) {
					ScrollView sl = (ScrollView) findViewById(R.id.chat_scroller);
					sl.scrollTo(sl.getScrollX(), v.getTop() + offset);
					return;
				}
			}
		}

	}

	/**
	 * Updates the chat view
	 */
	protected void updateChatView() {
		LinearLayout layout_msg = (LinearLayout) findViewById(R.id.chat_container);
		layout_msg.removeAllViews();

		Calendar lastMsgTs = Calendar.getInstance();
		lastMsgTs.setTimeInMillis(0);

		for (ChatMessage cm : history.getIterable()) {
			// Check if it's a different day
			Calendar curMsgTs = Calendar.getInstance();
			curMsgTs.setTimeInMillis(cm.getTimestamp());
			if (	(curMsgTs.get(Calendar.YEAR) != lastMsgTs.get(Calendar.YEAR)) ||
					(curMsgTs.get(Calendar.DAY_OF_YEAR) != lastMsgTs.get(Calendar.DAY_OF_YEAR))) {
				addNaked(dateFormat.format(cm.getTimestamp()) + ":");
			}
			lastMsgTs = curMsgTs;

			// Show the message
			if (cm.getUser().equals("<system>")) {
				addEvent(cm);
			} else {
				addMsg(cm);
			}
		}
	}

	/**
	 * Adds a chat bubble to the UI
	 * 
	 * @param user
	 * @param msg
	 */
	@SuppressWarnings("deprecation")
	protected void addMsg(ChatMessage cm) {
		// Username
		TextView tv_user = new TextView(this);
		tv_user.setText(cm.getUser() + ":");
		tv_user.setTextColor(getResources().getColor(color.holo_blue_dark));
		tv_user.setTextSize(11);
		LinearLayout.LayoutParams tv_user_layout = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,	// Width
				LinearLayout.LayoutParams.WRAP_CONTENT, // Height
				1f);									// Weight
		tv_user.setLayoutParams(tv_user_layout);

		// Date
		TextView tv_date = new TextView(this);
		tv_date.setText(timeFormat.format(cm.getTimestamp()));
		tv_date.setTextColor(getResources().getColor(R.color.timestamp_color));
		tv_date.setTextSize(11);

		// Username and date
		LinearLayout msg_header = new LinearLayout(this);
		msg_header.setOrientation(LinearLayout.HORIZONTAL);
		msg_header.addView(tv_user);
		msg_header.addView(tv_date);
		LinearLayout.LayoutParams hdr_lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,		// Width
				LinearLayout.LayoutParams.WRAP_CONTENT);	// Height
		hdr_lp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
				getResources().getDisplayMetrics());
		msg_header.setLayoutParams(hdr_lp);

		// Message
		TextView tv_msg = new TextView(this);
		tv_msg.setText(cm.getMessage());
		tv_msg.setTextSize(15);

		// The parent layout
		LinearLayout msg_layout = (new ChatMessageUI(cm, this));
		msg_layout.setOrientation(LinearLayout.VERTICAL);
		msg_layout.setBackgroundDrawable(getResources().getDrawable(R.drawable.msg_bubble));
		msg_layout.addView(msg_header);
		msg_layout.addView(tv_msg);

		// Set layout
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,		// Width
				LinearLayout.LayoutParams.WRAP_CONTENT);	// Height
		lp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
				getResources().getDisplayMetrics());
		msg_layout.setLayoutParams(lp);

		// Add 'em in
		LinearLayout chat_container = (LinearLayout) findViewById(R.id.chat_container);
		chat_container.addView(msg_layout);
	}

	/**
	 * Adds a event to the chat stream
	 * 
	 * @param msg
	 */
	@SuppressWarnings("deprecation")
	protected void addEvent(ChatMessage cm) {
		// Message
		TextView tv_msg = new TextView(this);
		String ts = timeFormat.format(cm.getTimestamp());
		tv_msg.setText(cm.getMessage() + " at " + ts);
		tv_msg.setTextColor(getResources().getColor(color.secondary_text_light));
		tv_msg.setTextSize(11);

		// Set background
		tv_msg.setBackgroundDrawable(getResources().getDrawable(R.drawable.event_bubble));

		// Set layout
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,		// Width
				LinearLayout.LayoutParams.WRAP_CONTENT);	// Height
		lp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
				getResources().getDisplayMetrics());
		tv_msg.setLayoutParams(lp);

		// Create reference to the original ChatMessage
		tv_msg.setTag(cm);

		// Add 'em in
		((LinearLayout) findViewById(R.id.chat_container)).addView(tv_msg);
	}

	/**
	 * Add un-styled text to the chat view
	 */
	protected void addNaked(String msg) {
		TextView tv_msg = new TextView(this);
		tv_msg.setText(msg);
		tv_msg.setTextSize(13);

		// Set layout
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,		// Width
				LinearLayout.LayoutParams.WRAP_CONTENT);	// Height
		lp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
				getResources().getDisplayMetrics());
		tv_msg.setLayoutParams(lp);

		// Add 'em in
		((LinearLayout) findViewById(R.id.chat_container)).addView(tv_msg);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		if (!checkPlayServices()) {
			showToast("This app requires Google Play Services");
			return;
		}

		try {
			server = new ChatServer(this);
		} catch (NeedRegistrationException e) {
			doLogin();
			return;
		}

		if (savedInstanceState == null) {
			history = new ChatHistory();

			getFragmentManager().beginTransaction()
			.add(R.id.container, new PlaceholderFragment()).commit();
		} else {
			history = savedInstanceState.getParcelable("history");
		} // */
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putParcelable("history", history);

		ChatMessage cm = getVisibleChat();
		int curIdx = -1;
		if (cm == null) {
			Log.w("ChatActivity.onSaveInstanceState", "No topmost?");
		} else {
			curIdx = cm.getIdx();
		}
		savedInstanceState.putInt("curIdx", curIdx);

		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		history = savedInstanceState.getParcelable("history");
		try {
			server = new ChatServer(this);
		} catch (NeedRegistrationException e) {
			doLogin();
			return;
		}

		LinearLayout layout_msg = (LinearLayout) findViewById(R.id.chat_container);
		if (layout_msg.getChildCount() == 0) {
			updateChatView();
		}

		// Indicate where we should be
		scrollTo = savedInstanceState.getInt("curIdx");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(chatReceiver);
	}

	@Override
	public void onResume() {
		super.onResume();

		// Register a receiver for chat push messages
		IntentFilter intf = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
		intf.addCategory("com.tan_ce.tecingamechat");
		registerReceiver(chatReceiver, intf);

		// Clear all notifications
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(ChatIntentService.NOTIFICATION_ID);

		// Make sure our history is up to date
		if (history.nextIdx == 0) {
			primeHistory();
		} else {
			refreshHistory(null);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_chat, container,
					false);
			return rootView;
		}
	}

	/**
	 * Receives new chat notifications
	 */
	public BroadcastReceiver chatReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// If we're still waiting for first sync, then ignore this notification
			if (history.nextIdx == 0) {
				return;
			}

			// Get the pushed message
			Bundle extras = intent.getExtras();
			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
			String messageType = gcm.getMessageType(intent);
			if (	!extras.isEmpty() &&
					GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

				ChatMessage cm;
				try {
					cm = new ChatMessage(extras);
				} catch (Exception e) {
					Log.w("ChatActivity", "Server pushed invalid message: " + e.getMessage());
					return;
				}

				if (history.nextIdx == cm.getIdx()) {
					// We received exactly the next message we're expecting.
					Log.i("ChatActivity", "Updating by injection");
					history.add(cm);
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							// Update chat view
							updateChatView();

							// Chat positions won't be updated until the UI loop
							// has had time to update the children
							new Handler().post(new Runnable() {
								@Override
								public void run() {
									// Scroll down by one chat
									ChatMessage topmost = getVisibleChat();
									if (topmost == null) {
										Log.w("ChatActivity.chatReceiver", "No topmost?");
									} else {
										int idx = topmost.getIdx() + 1;
										new ChatScroller(idx).run();
									}

									// Clear notifications
									NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
									notificationManager.cancel(ChatIntentService.NOTIFICATION_ID);
								}
							});
						}
					});
				} else {
					Log.i("ChatActivity", "Updating by refresh");
					// We might have gotten out of sync. Better do a full refresh
					(new HistoryRefresher(history.nextIdx - 1) {
						@Override
						protected void onPostExecute2() {
							// Clear notifications
							NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
							notificationManager.cancel(ChatIntentService.NOTIFICATION_ID);
						}
					}).execute();
				}
			}
		}
	};

	/**
	 * Abstract class used to update the history and subsequently the UI
	 */
	protected abstract class HistoryUpdator<T> extends AsyncTask<T, Void, Boolean> {
		int scrollIdx;

		/**
		 * Constructor
		 * 
		 * @param scrollIdx If greater than zero, will scroll to that message upon completion
		 */
		HistoryUpdator(int scrollIdx) {
			this.scrollIdx = scrollIdx;
		}

		@Override
		protected void onPostExecute(Boolean failed) {
			if (failed.booleanValue()) {
				showToast("Failed to retrieve history from server");
			} else {
				updateChatView();
				if (scrollIdx >= 0) {
					new Handler().post(new ChatScroller(scrollIdx));
				}
			}

			onPostExecute2();
		}

		protected void onPostExecute2() {
			// Stub that may be overwritten by subclasses
		}
	}

	/**
	 * Class used to update the history with the latest chat messages
	 */
	protected class HistoryRefresher extends HistoryUpdator<Void> {
		HistoryRefresher(int scrollIdx) {
			super(scrollIdx);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			int curCount;
			do {
				curCount = history.size();
				try {
					List<ChatMessage> ret = server.getHistory(history.nextIdx, 500);
					history.mergeHistory(ret);
				} catch (Exception e) {
					e.printStackTrace();
					return true;
				}
			} while (history.size() > curCount);

			return false;
		}
	}

	/**
	 * A LinearLayout with a reference to the original chat message
	 */
	protected class ChatMessageUI extends LinearLayout {
		public ChatMessage chatMessage;
		ChatMessageUI(ChatMessage cm, Context context) {
			super(context);
			chatMessage = cm;
		}
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	protected boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						9000).show();
			} else {
				Log.w("GCM Check", "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	protected void doLogin() {
		Intent loginIntent = new Intent(this, LoginActivity.class);
		startActivity(loginIntent);
		finish();
	}

}
