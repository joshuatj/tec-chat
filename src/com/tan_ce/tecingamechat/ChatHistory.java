package com.tan_ce.tecingamechat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents the chat history cache of the app. Is a parcelable in order
 * to survive instance saving.
 * 
 * @author tan-ce
 *
 */
public class ChatHistory implements Parcelable {
	protected int earliest_idx = Integer.MAX_VALUE;
	protected List<ChatMessage> history;

	public ChatMessage get(int idx) { return history.get(idx); }
	public Iterable<ChatMessage> getIterable() { return history; }
	public int size() { return history.size(); }
	public void add(ChatMessage msg) { history.add(msg); }
	
	public synchronized int getEarliestIdx() { return earliest_idx; }
	
	/**
	 * Merges two lists.
	 * Note: Involves sorting the entire history. Not recommended to run in the
	 * UI thread.
	 * 
	 * @param c
	 */
	public synchronized void mergeHistory(List<ChatMessage> c) {
		// Append
		c.addAll(history);
		
		// Sort
		Collections.sort(c, new ChatMessage.ChatMessageComparator());
		
		// Some processing on the whole history
		earliest_idx = Integer.MAX_VALUE;
		ChatMessage prev = null;
		ListIterator<ChatMessage> li = c.listIterator();
		while(li.hasNext()) {
			ChatMessage cur = li.next();
			
			// Find the smallest index
			if (cur.getIdx() < earliest_idx) earliest_idx = cur.getIdx();
			
			// Look for duplicates
			if (prev != null && cur.getIdx() == prev.getIdx()) {
				li.remove();
			}
			prev = cur;
		}
		
		history = c;
	}
	
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
			dest.writeInt(cm.getIdx());
			dest.writeString(cm.getUser().toString());
			dest.writeString(cm.getMessage().toString());
			dest.writeLong(cm.getTimestamp());
		}
	}
	
	public ChatHistory(Parcel in) {
		// Read the number of items to be restored
		int size = in.readInt();
		// Reserve extra space
		history = new ArrayList<ChatMessage>(size + (size / 2));
		
		// Read all of the chat messages
		for (int i = 0; i < size; i++) {
			int idx = in.readInt();
			String user = in.readString();
			String msg = in.readString();
			long date = in.readLong();
			history.add(new ChatMessage(idx, user, msg, date));
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