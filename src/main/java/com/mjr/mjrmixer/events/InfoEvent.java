package com.mjr.mjrmixer.events;

import com.mjr.mjrmixer.Event;

public class InfoEvent extends Event {
	public final String message;
	public final int channelID;
	public final String channelName;

	public InfoEvent(String message, int channelID, String channelName) {
		super(EventType.INFOMSG);
		this.message = message;
		this.channelID = channelID;
		this.channelName = channelName;
	}

	public InfoEvent() {
		super(EventType.INFOMSG);
		this.message = null;
		this.channelID = -1;
		this.channelName = null;
	}

	public void onEvent(InfoEvent event) {

	}
}
