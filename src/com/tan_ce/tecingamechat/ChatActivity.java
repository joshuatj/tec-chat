package com.tan_ce.tecingamechat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.R.color;
import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends Activity {
	protected static SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aaa", Locale.getDefault());
	protected static SimpleDateFormat dateFormat = new SimpleDateFormat("d LLL", Locale.getDefault());
	protected ChatHistory history;

	protected void showToast(String msg) {
		Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		toast.show();
	}
	
	public void loadPrev(View view) {
		showToast("!!");
	}
	
	/**
	 * Run when the user clicks "Send"
	 * 
	 * @param view
	 */
	public void sendMsg(View view) {
		showToast("Not ready to send message");
		/* EditText edit_msg = (EditText) findViewById(R.id.edit_msg);
		CharSequence msg = edit_msg.getText().toString();
		
		addMsg("some_user", msg);
		history.add(new ChatMessage("some_user", msg));
		
		edit_msg.setText(""); */
	}
	
	/**
	 * Run when the user forces a manual refresh
	 * 
	 * @param menu
	 */
	public void refreshHistory(MenuItem menu) {
		(new HistoryRetriever()).execute();
	}
	
	/**
	 * Updates the chat view
	 */
	protected void updateChatView() {
		LinearLayout layout_msg = (LinearLayout) findViewById(R.id.layout_msg);
		layout_msg.removeAllViews();
		
		Calendar lastMsgTs = Calendar.getInstance();
		lastMsgTs.setTimeInMillis(0);
		
		for (ChatMessage cm : history.getIterable()) {
			// Check if it's a different day
			Calendar curMsgTs = Calendar.getInstance();
			curMsgTs.setTimeInMillis(cm.getTimestamp());
			if (	(curMsgTs.get(Calendar.YEAR) != lastMsgTs.get(Calendar.YEAR)) &&
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
		LinearLayout msg_layout = new LinearLayout(this);
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
		((LinearLayout) findViewById(R.id.layout_msg)).addView(msg_layout);
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
		tv_msg.setText(cm.getMessage());
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
		
		// Add 'em in
		((LinearLayout) findViewById(R.id.layout_msg)).addView(tv_msg);
	}
	
	protected void addNaked(String msg) {
		TextView tv_msg = new TextView(this);
		tv_msg.setText(msg);
		tv_msg.setTextSize(11);
		
		// Set layout
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,		// Width 
				LinearLayout.LayoutParams.WRAP_CONTENT);	// Height
		lp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, 
				getResources().getDisplayMetrics());
		tv_msg.setLayoutParams(lp);
		
		// Add 'em in
		((LinearLayout) findViewById(R.id.layout_msg)).addView(tv_msg);
	}
	
	/**
	 * doInBackground should be called with the index and number of entries to retrieve.
	 * If omitted, then the last 50 messages are retrieved.
	 *  
	 * It will then retrieve those lines from the server and update the history object.
	 * 
	 * On completion, the chat history is updated.
	 * 
	 * @author tan-ce
	 *
	 */
	private class HistoryRetriever extends AsyncTask<Integer, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Integer... params) {
			try {
				ChatServer cs = new ChatServer();
				List<ChatMessage> ret = cs.getHistory();
				history.mergeHistory(ret);
			} catch (Exception e) {
				e.printStackTrace();
				return true;
			}
			
			return false;
		}
		
		@Override
		protected void onPostExecute(Boolean failed) {
			if (failed.booleanValue()) {
				showToast("Failed to retrieve history from server");
			} else {
				updateChatView();
			}
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		if (savedInstanceState == null) {
			// We need to initialize the char history. First find out the 
			// next chat index to prime the ChatHistory
			// TODO
			// AsyncTask lastIndexGetter = new AsyncTask
			
			history = new ChatHistory();
			
			(new HistoryRetriever()).execute();
			
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		} else {
			history = savedInstanceState.getParcelable("history");
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putParcelable("history", history);
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		history = savedInstanceState.getParcelable("history");
		
		LinearLayout layout_msg = (LinearLayout) findViewById(R.id.layout_msg);
		if (layout_msg.getChildCount() == 0) {
			updateChatView();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
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

}
