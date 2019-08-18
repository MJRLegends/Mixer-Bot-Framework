package com.mjr.mjrmixer;

public class Event {

	public enum EventType {
		MESSAGE("Message"), JOIN("Join"), PART("Part"), CONNECT("Connect"), DISCONNECT("Disconnect"), ERRORMSG("ErrorMessage"), INFOMSG(
				"InfoMessage");

		public final String name;

		EventType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public EventType typeIRC;

	public Event(EventType type) {
		super();
		this.typeIRC = type;
	}
}
