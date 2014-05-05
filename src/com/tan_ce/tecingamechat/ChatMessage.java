package com.tan_ce.tecingamechat;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

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

	/**
	 * Parses a Chat Message from a JSON response
	 * 
	 * @param jcm
	 * @throws JSONException
	 */
	ChatMessage(JSONObject jcm) throws JSONException {
		this.idx = jcm.getInt("id");
		this.user = jcm.getString("user");
		this.msg = jcm.getString("msg");
		// Java uses milliseconds since the epoch:
		this.unixTime = jcm.getLong("ts") * 1000;
	}

	/**
	 * Parses a Chat Message from an Intent's extras bundle
	 * 
	 * @param extras
	 * @throws Exception
	 */
	ChatMessage(Bundle extras) throws Exception {
		String idx_str = extras.getString("idx");

		if (idx_str != null) {
			try {
				idx = Integer.parseInt(idx_str);
			} catch (NumberFormatException e) {
				throw new Exception("ChatMessage(): Invalid format");
			}
		}

		user = extras.getString("user");
		msg = extras.getString("msg");
		String ts_str = extras.getString("ts");
		if (user == null || msg == null || ts_str == null) {
			throw new Exception("ChatMessage(): Invalid format");
		}

		try {
			// Java uses milliseconds since epoch
			unixTime = Long.parseLong(ts_str) * 1000;
		} catch (NumberFormatException e) {
			throw new Exception("ChatMessage(): Invalid format");
		}
	}

	int getIdx() { return idx; }
	CharSequence getUser() { return user; }
	CharSequence getMessage() { return msg; }
	long getTimestamp() {return unixTime; }
}