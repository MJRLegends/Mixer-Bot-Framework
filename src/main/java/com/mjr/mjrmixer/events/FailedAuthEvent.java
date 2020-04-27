package com.mjr.mjrmixer.events;

import com.mjr.mjrmixer.Event;

public class FailedAuthEvent extends Event {

	public final String errorMessage;
	public final int numberOfFailedAuths;
	public final String channelName;
	public final int channelID;

	public FailedAuthEvent(String errorMessage, int numberOfFailedAuths, String channelName, int channelID) {
		super(EventType.FAILEDAUTH);
		this.errorMessage = errorMessage;
		this.numberOfFailedAuths = numberOfFailedAuths;
		this.channelName = channelName;
		this.channelID = channelID;
	}

	public FailedAuthEvent() {
		super(EventType.FAILEDAUTH);
		this.errorMessage = null;
		this.numberOfFailedAuths = -1;
		this.channelName = null;
		this.channelID = -1;
	}

	public void onEvent(FailedAuthEvent event) {

	}

}