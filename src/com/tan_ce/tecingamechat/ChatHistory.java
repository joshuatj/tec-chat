package com.tan_ce.tecingamechat;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatHistory implements Parcelable {
	protected ArrayList<ChatMessage> history;

	public ChatMessage get(int idx) { return history.get(idx); }
	public int size() { return history.size(); }
	public void add(ChatMessage msg) { history.add(msg); }
	
	public ChatHistory() {
		history = new ArrayList<ChatMessage>();
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// Write the number of items
		dest.writeInt(history.size());
		
		// Save each chat message
		for (ChatMessage cm : history) {
			dest.writeString(cm.getUser().toString());
			dest.writeString(cm.getMessage().toString());
		}
	}
	
	public ChatHistory(Parcel in) {
		// Read the number of items to be restored
		int size = in.readInt();
		// Reserve extra space
		history = new ArrayList<ChatMessage>(size + (size / 2));
		
		// Read all of the chat messages
		for (int i = 0; i < size; i++) {
			String user = in.readString();
			String msg = in.readString();
			history.add(new ChatMessage(user, msg));
		}
	}
	
	public static final Parcelable.Creator<ChatHistory> CREATOR
			= new Parcelable.Creator<ChatHistory>() {
		public ChatHistory createFromParcel(Parcel in) {
			return new ChatHistory(in);
		}

		public ChatHistory[] newArray(int size) {
			return new ChatHistory[size];
		}
	};
}