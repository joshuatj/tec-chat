package com.tan_ce.tecingamechat;

import java.util.Comparator;

/**
 * Represents a single chat message
 * 
 * @author tan-ce
 *
 */
public class ChatMessage {
	public static class ChatMessageComparator implements Comparator<ChatMessage> {
		@Override
		public int compare(ChatMessage lhs, ChatMessage rhs) {
			return lhs.idx - rhs.idx;
		}

	}

	protected int idx;
	protected CharSequence user;
	protected CharSequence msg;
	protected long unixTime;

	ChatMessage(int idx, CharSequence user, CharSequence message, long date) {
		this.idx = idx;
		this.user = user;
		this.msg = message;
		this.unixTime = date;
	}

	int getIdx() { return idx; }
	CharSequence getUser() { return user; }
	CharSequence getMessage() { return msg; }
	long getTimestamp() {return unixTime; }
}