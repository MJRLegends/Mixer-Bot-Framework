package com.mjr.mjrmixer.events;

import com.mjr.mjrmixer.Event;

public class InfoEvent extends Event {
	public final String message;

	public InfoEvent(String message) {
		super(EventType.INFOMSG);
		this.message = message;
	}

	public InfoEvent() {
		super(EventType.INFOMSG);
		this.message = null;
	}

	public void onEvent(InfoEvent event) {

	}
}
