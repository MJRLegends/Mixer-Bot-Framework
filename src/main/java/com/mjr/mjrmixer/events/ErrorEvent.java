package com.mjr.mjrmixer.events;

import com.mjr.mjrmixer.Event;

public class ErrorEvent extends Event {

	public final String errorMessage;
	public final Throwable error;

	public ErrorEvent(String errorMessage, Throwable error) {
		super(EventType.ERRORMSG);
		this.errorMessage = errorMessage;
		this.error = error;
	}

	public ErrorEvent() {
		super(EventType.ERRORMSG);
		this.errorMessage = null;
		this.error = null;
	}

	public void onEvent(ErrorEvent event) {

	}

}