package com.mjr.mjrmixer;

import java.util.List;

import com.mixer.api.resource.MixerUser.Role;
import com.mjr.mjrmixer.Event.EventType;
import com.mjr.mjrmixer.events.DisconnectEvent;
import com.mjr.mjrmixer.events.DisconnectEvent.DisconnectType;
import com.mjr.mjrmixer.events.ErrorEvent;
import com.mjr.mjrmixer.events.InfoEvent;
import com.mjr.mjrmixer.events.JoinEvent;
import com.mjr.mjrmixer.events.MessageEvent;
import com.mjr.mjrmixer.events.PartEvent;

public class MixerEventHooks {
	public static void triggerOnMessageEvent(final String message, final String channel, final int channelID, final String sender, final int senderID, final List<Role> senderRoles) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.MESSAGE.getName().equalsIgnoreCase(event.type.getName()))
				((MessageEvent) event).onEvent(new MessageEvent(sender, channel, channelID, message, senderID, senderRoles));
		}
	}

	public static void triggerOnJoinEvent(final String channel, final int channelID, final String sender, final int senderID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.JOIN.getName().equalsIgnoreCase(event.type.getName()))
				((JoinEvent) event).onEvent(new JoinEvent(channel, channelID, sender, senderID));
		}
	}

	public static void triggerOnPartEvent(final String channel, final int channelID, final String sender, final int senderID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.PART.getName().equalsIgnoreCase(event.type.getName()))
				((PartEvent) event).onEvent(new PartEvent(channel, channelID, sender, senderID));
		}
	}

	public static void triggerOnDisconnectEvent(final DisconnectType type, final String channel, final int channelID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.DISCONNECT.getName().equalsIgnoreCase(event.type.getName()))
				((DisconnectEvent) event).onEvent(new DisconnectEvent(type, channel, channelID));
		}
	}

	public static void triggerOnInfoEvent(String message) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.INFOMSG.getName().equalsIgnoreCase(event.type.getName()))
				((InfoEvent) event).onEvent(new InfoEvent(message));
		}
	}

	public static void triggerOnErrorEvent(String errorMessage, Throwable error) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.ERRORMSG.getName().equalsIgnoreCase(event.type.getName()))
				((ErrorEvent) event).onEvent(new ErrorEvent(errorMessage, error));
		}
	}

}
