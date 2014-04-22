package com.tan_ce.tecingamechat;

import java.util.ArrayList;

import android.R.color;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Build;

public class ChatActivity extends Activity {
	protected ChatHistory history;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		if (savedInstanceState == null) {
			history = new ChatHistory();
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
			for (int i = 0; i < history.size(); i++) {
				ChatMessage cm = history.get(i);
				addMsg(cm.getUser(), cm.getMessage());
			}
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
	
	@SuppressWarnings("deprecation")
	protected void addMsg(CharSequence user, CharSequence msg) {
		// Username
		TextView tv_user = new TextView(this);
		tv_user.setText(user + ":");
		tv_user.setTextColor(getResources().getColor(color.holo_blue_dark));
		tv_user.setTextSize(10);
		
		// Message
		TextView tv_msg = new TextView(this);
		tv_msg.setText(msg);
		tv_msg.setTextSize(15);
		
		// The parent layout
		LinearLayout msg_layout = new LinearLayout(this);
		msg_layout.setOrientation(LinearLayout.VERTICAL);
		msg_layout.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corner));
		msg_layout.addView(tv_user);
		msg_layout.addView(tv_msg);
		
		// Set a bottom margin
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,		// Width 
				LinearLayout.LayoutParams.WRAP_CONTENT);	// Height
		lp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, 
				getResources().getDisplayMetrics());
		msg_layout.setLayoutParams(lp);
		
		// Add 'em in
		((LinearLayout) findViewById(R.id.layout_msg)).addView(msg_layout);
	}
	
	public void sendMsg(View view) {
		EditText edit_msg = (EditText) findViewById(R.id.edit_msg);
		CharSequence msg = edit_msg.getText().toString();
		
		addMsg("some_user", msg);
		history.add(new ChatMessage("some_user", msg));
		
		edit_msg.setText("");
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
