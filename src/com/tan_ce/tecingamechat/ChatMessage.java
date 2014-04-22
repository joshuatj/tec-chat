package com.tan_ce.tecingamechat;

public class ChatMessage {
	protected CharSequence user;
	protected CharSequence msg;
	
	ChatMessage(CharSequence user, CharSequence message) {
		this.user = user;
		this.msg = message;
	}
	
	CharSequence getUser() { return user; }
	CharSequence getMessage() { return msg; }
}