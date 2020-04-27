package com.mjr.mjrmixer.events;

import com.mixer.api.resource.chat.events.data.ChatDisconnectData;
import com.mixer.api.resource.constellation.events.data.ConstellationDisconnectData;
import com.mjr.mjrmixer.Event;

/**
 * Reconnecting of Chat/Constellation Connections is done automatically by the Framework, this event is designed to be used for notification of disconnect
 */
public class DisconnectEvent extends Event {

	public enum DisconnectType {
		CHAT, CONSTELLATION;
	}

	public final DisconnectType type;
	public final String channelName;
	public final int channelID;
	public ConstellationDisconnectData constellationData;
	public ChatDisconnectData chatData;

	public DisconnectEvent(DisconnectType type, String channelName, int channelID, ConstellationDisconnectData constellationData) {
		super(EventType.DISCONNECT);
		this.type = type;
		this.channelName = channelName;
		this.channelID = channelID;
		this.constellationData = constellationData;
	}

	public DisconnectEvent(DisconnectType type, String channelName, int channelID, ChatDisconnectData chatData) {
		super(EventType.DISCONNECT);
		this.type = type;
		this.channelName = channelName;
		this.channelID = channelID;
		this.chatData = chatData;
	}

	public DisconnectEvent() {
		super(EventType.DISCONNECT);
		this.type = null;
		this.channelName = null;
		this.channelID = -1;
		this.constellationData = null;
		this.chatData = null;
	}

	public void onEvent(DisconnectEvent event) {

	}

}