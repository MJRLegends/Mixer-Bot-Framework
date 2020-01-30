package com.mjr.mjrmixer.events;

import java.util.List;

import com.mixer.api.resource.MixerUser.Role;
import com.mjr.mjrmixer.Event;

public class MessageEvent extends Event{

	public final String message;
	public final List<String> emotes;
	public final List<String> links;
	public final String channelName;
	public final int channelID;
	public final String sender;
	public final int senderID;
	public final List<Role> senderRoles;
	
	public MessageEvent(String message, List<String> emotes, List<String> links, String channelName, int channelID, String sender, int senderID, List<Role> senderRoles) {
		super(EventType.MESSAGE);
		this.message = message;
		this.emotes = emotes;
		this.links = links;
		this.channelName = channelName;
		this.channelID = channelID;
		this.sender = sender;
		this.senderID = senderID;
		this.senderRoles = senderRoles;
	}
	
	public MessageEvent() {
		super(EventType.MESSAGE);
		this.message = null;
		this.emotes = null;
		this.links = null;
		this.channelName = null;
		this.channelID = -1;
		this.sender = null;
		this.senderID = -1;
		this.senderRoles = null;
	}
	
	public void onEvent(MessageEvent event) {
		
	}

}