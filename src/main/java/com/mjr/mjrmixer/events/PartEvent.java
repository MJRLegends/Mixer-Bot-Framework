package com.mjr.mjrmixer.events;

import com.mjr.mjrmixer.Event;

public class PartEvent extends Event {

	public final String channelName;
	public final int channelID;
	public final String sender;
	public final int senderID;

	public PartEvent(String channelName, int channelID, String sender, int senderID) {
		super(EventType.PART);
		this.channelName = channelName;
		this.channelID = channelID;
		this.sender = sender;
		this.senderID = senderID;
	}

	public PartEvent() {
		super(EventType.PART);
		this.channelName = null;
		this.channelID = -1;
		this.sender = null;
		this.senderID = -1;
	}

	public void onEvent(PartEvent event) {

	}

}