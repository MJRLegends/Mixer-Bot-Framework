package com.mjr.mjrmixer.events;

import com.mjr.mjrmixer.Event;

/**
 * Reconnecting of Chat/Constellation Connections is done automatically by the Framework, this event is designed to be used for notification of reconnecting
 */
public class ReconnectEvent extends Event {

	public enum ReconnectType {
		CHAT, CONSTELLATION;
	}

	public final ReconnectType type;
	public final String channel;
	public final int channelID;

	public ReconnectEvent(ReconnectType type, String channel, int channelID) {
		super(EventType.RECONNECT);
		this.type = type;
		this.channel = channel;
		this.channelID = channelID;
	}

	public ReconnectEvent() {
		super(EventType.RECONNECT);
		this.type = null;
		this.channel = null;
		this.channelID = -1;
	}

	public void onEvent(ReconnectEvent event) {

	}

}