package com.mjr.mjrmixer;

import java.util.ArrayList;
import java.util.List;

public class MixerManager {

	private static List<Event> listeners = new ArrayList<Event>();

	public static void registerEventHandler(Event event) {
		MixerManager.listeners.add(event);
	}

	public static List<Event> getEventListeners() {
		return listeners;
	}
}
