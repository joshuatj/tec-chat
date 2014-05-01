package com.tan_ce.tecingamechat;

import java.util.concurrent.atomic.AtomicInteger;

public class ViewID {
	protected static ViewID INSTANCE = new ViewID();
	protected AtomicInteger seq;

	ViewID() {
		seq = new AtomicInteger(Integer.MAX_VALUE);
	}

	public static int get() {
		return INSTANCE.seq.decrementAndGet();
	}
}
