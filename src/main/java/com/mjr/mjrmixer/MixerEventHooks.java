package com.mjr.mjrmixer;

import java.util.List;

import com.mixer.api.resource.MixerUser.Role;
import com.mjr.mjrmixer.Event.EventType;
import com.mjr.mjrmixer.events.DisconnectEvent;
import com.mjr.mjrmixer.events.DisconnectEvent.DisconnectType;
import com.mjr.mjrmixer.events.ErrorEvent;
import com.mjr.mjrmixer.events.JoinEvent;
import com.mjr.mjrmixer.events.MessageEvent;
import com.mjr.mjrmixer.events.PartEvent;

public class MixerEventHooks {
	public static void triggerOnMessageEvent(final String message, final String channel, final int channelID, final String sender, final int senderID, final List<Role> senderRoles) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.MESSAGE.getName().equalsIgnoreCase(event.typeIRC.getName()))
				((MessageEvent) event).onEvent(new MessageEvent(sender, channel, channelID, message, senderID, senderRoles));
		}
	}

	public static void triggerOnJoinEvent(final String channel, final int channelID, final String sender, final int senderID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.JOIN.getName().equalsIgnoreCase(event.typeIRC.getName()))
				((JoinEvent) event).onEvent(new JoinEvent(channel, channelID, sender, senderID));
		}
	}

	public static void triggerOnPartEvent(final String channel, final int channelID, final String sender, final int senderID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.PART.getName().equalsIgnoreCase(event.typeIRC.getName()))
				((PartEvent) event).onEvent(new PartEvent(channel, channelID, sender, senderID));
		}
	}

	public static void triggerOnDisconnectEvent(final DisconnectType type, final String channel, final int channelID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.DISCONNECT.getName().equalsIgnoreCase(event.typeIRC.getName()))
				((DisconnectEvent) event).onEvent(new DisconnectEvent(type, channel, channelID));
		}
	}

	public static void triggerOnErrorEvent(String errorMessage, Throwable error) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.ERRORMSG.getName().equalsIgnoreCase(event.typeIRC.getName()))
				((ErrorEvent) event).onEvent(new ErrorEvent(errorMessage, error));
		}
	}

}
