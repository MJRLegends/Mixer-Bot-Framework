package com.mjr.mjrmixer.events;

import com.mjr.mjrmixer.Event;

public class FailedReconnectEvent extends Event {

	public final int numberOfFailedAttempts;
	public final String channelName;
	public final int channelID;

	public FailedReconnectEvent(int numberOfFailedAttempts, String channelName, int channelID) {
		super(EventType.FAILEDRECONNECT);
		this.numberOfFailedAttempts = numberOfFailedAttempts;
		this.channelName = channelName;
		this.channelID = channelID;
	}

	public FailedReconnectEvent() {
		super(EventType.FAILEDRECONNECT);
		this.numberOfFailedAttempts = -1;
		this.channelName = null;
		this.channelID = -1;
	}

	public void onEvent(FailedReconnectEvent event) {

	}

}