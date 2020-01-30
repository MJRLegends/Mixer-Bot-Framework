package com.mjr.mjrmixer.events;

import java.util.List;

import com.mixer.api.resource.MixerUser.Role;
import com.mjr.mjrmixer.Event;

public class UserUpdateEvent extends Event{

	public final String channelName;
	public final int channelID;
	public final int userID;
	public final List<Role> userRoles;	

	
	public UserUpdateEvent(String channelName, int channelID, int userID, List<Role> userRoles) {
		super(EventType.USERUPDATE);
		this.channelName = channelName;
		this.channelID = channelID;
		this.userID = userID;
		this.userRoles = userRoles;
	}
	
	public UserUpdateEvent() {
		super(EventType.USERUPDATE);
		this.channelName = null;
		this.channelID = -1;
		this.userID = -1;
		this.userRoles = null;
	}
	
	public void onEvent(UserUpdateEvent event) {
		
	}

}