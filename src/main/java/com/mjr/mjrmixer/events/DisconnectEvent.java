package com.mjr.mjrmixer.events;

import com.mjr.mjrmixer.Event;

public class DisconnectEvent extends Event {

	public enum DisconnectType {
		CHAT, CONSTELLATION;
	}

	public final DisconnectType type;
	public final String channel;
	public final int channelID;

	public DisconnectEvent(DisconnectType type, String channel, int channelID) {
		super(EventType.DISCONNECT);
		this.type = type;
		this.channel = channel;
		this.channelID = channelID;
	}
	
	public DisconnectEvent() {
		super(EventType.DISCONNECT);
		this.type = null;
		this.channel = null;
		this.channelID = -1;
	}

	public void onEvent(DisconnectEvent event) {

	}

}