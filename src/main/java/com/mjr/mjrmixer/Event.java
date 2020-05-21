package com.mjr.mjrmixer;

public class Event {

	public enum EventType {
		MESSAGE("Message"), JOIN("Join"), PART("Part"), CONNECT("Connect"), DISCONNECT("Disconnect"), RECONNECT("Reconnect"), ERRORMSG("ErrorMessage"), INFOMSG(
				"InfoMessage"), USERUPDATE("UserUpdate"), FAILEDAUTH("FailedAuth"), FAILEDRECONNECT("FailedReconnect");

		public final String name;

		EventType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public EventType type;

	public Event(EventType type) {
		super();
		this.type = type;
	}
}
