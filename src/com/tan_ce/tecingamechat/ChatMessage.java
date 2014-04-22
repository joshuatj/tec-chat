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
	
	ChatMessage(int idx, CharSequence user, CharSequence message) {
		this.idx = idx;
		this.user = user;
		this.msg = message;
	}
	
	int getIdx() { return idx; }
	CharSequence getUser() { return user; }
	CharSequence getMessage() { return msg; }
}