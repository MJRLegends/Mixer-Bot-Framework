package com.mjr.mjrmixer;

import java.util.List;

import com.mixer.api.resource.MixerUser.Role;
import com.mixer.api.resource.chat.events.data.ChatDisconnectData;
import com.mixer.api.resource.constellation.events.data.ConstellationDisconnectData;
import com.mjr.mjrmixer.Event.EventType;
import com.mjr.mjrmixer.events.*;
import com.mjr.mjrmixer.events.DisconnectEvent.DisconnectType;
import com.mjr.mjrmixer.events.ReconnectEvent.ReconnectType;

public class MixerEventHooks {
	public static void triggerOnMessageEvent(final String message, List<String> emotes, List<String> links, final String channelName, final int channelID, final String sender, final int senderID, final List<Role> senderRoles) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.MESSAGE.getName().equalsIgnoreCase(event.type.getName()))
				((MessageEvent) event).onEvent(new MessageEvent(message, emotes, links, channelName, channelID, sender, senderID, senderRoles));
		}
	}

	public static void triggerOnJoinEvent(final String channelName, final int channelID, final String sender, final int senderID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.JOIN.getName().equalsIgnoreCase(event.type.getName()))
				((JoinEvent) event).onEvent(new JoinEvent(channelName, channelID, sender, senderID));
		}
	}

	public static void triggerOnPartEvent(final String channelName, final int channelID, final String sender, final int senderID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.PART.getName().equalsIgnoreCase(event.type.getName()))
				((PartEvent) event).onEvent(new PartEvent(channelName, channelID, sender, senderID));
		}
	}

	public static void triggerOnDisconnectEvent(final DisconnectType type, final String channelName, final int channelID, ChatDisconnectData data) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.DISCONNECT.getName().equalsIgnoreCase(event.type.getName()))
				((DisconnectEvent) event).onEvent(new DisconnectEvent(type, channelName, channelID, data));
		}
	}

	public static void triggerOnDisconnectEvent(final DisconnectType type, final String channelName, final int channelID, ConstellationDisconnectData data) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.DISCONNECT.getName().equalsIgnoreCase(event.type.getName()))
				((DisconnectEvent) event).onEvent(new DisconnectEvent(type, channelName, channelID, data));
		}
	}

	public static void triggerOnReconnectEvent(final ReconnectType type, final String channelName, final int channelID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.RECONNECT.getName().equalsIgnoreCase(event.type.getName()))
				((ReconnectEvent) event).onEvent(new ReconnectEvent(type, channelName, channelID));
		}
	}

	public static void triggerOnInfoEvent(final String channelName, final int channelID, String message) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.INFOMSG.getName().equalsIgnoreCase(event.type.getName()))
				((InfoEvent) event).onEvent(new InfoEvent(message, channelID, channelName));
		}
	}

	public static void triggerOnErrorEvent(String errorMessage, Throwable error) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.ERRORMSG.getName().equalsIgnoreCase(event.type.getName()))
				((ErrorEvent) event).onEvent(new ErrorEvent(errorMessage, error));
		}
	}

	public static void triggerOnFailedAuthEvent(MixerBotBase bot, String errorMessage, int numberOfFailedAuths, String channelName, int channelID) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.FAILEDAUTH.getName().equalsIgnoreCase(event.type.getName()))
				((FailedAuthEvent) event).onEvent(new FailedAuthEvent(errorMessage, numberOfFailedAuths, channelName, channelID));
		}
		if(bot != null && bot.getConnectedChannel() == null)
			bot.updateConnectedChannel();
	}

	public static void triggerOnUserUpdateEvent(final String channelName, final int channelID, int userID, List<Role> userRoles) {
		for (Event event : MixerManager.getEventListeners()) {
			if (EventType.USERUPDATE.getName().equalsIgnoreCase(event.type.getName()))
				((UserUpdateEvent) event).onEvent(new UserUpdateEvent(channelName, channelID, userID, userRoles));
		}
	}

}
