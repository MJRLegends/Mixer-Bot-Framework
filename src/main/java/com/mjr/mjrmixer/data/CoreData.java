package com.mjr.mjrmixer.data;

import java.util.ArrayList;
import java.util.List;

import com.mixer.api.resource.chat.events.IncomingMessageEvent;

public class CoreData {
	private List<String> moderators;
	private List<String> viewers;
	private List<IncomingMessageEvent> messageIDCaches;
	private List<String> liveEvents;

	private String botName;

	private int channelID;
	private int userID;
	private String channelName;

	public CoreData(String botName) {
		this.botName = botName;
		moderators = new ArrayList<String>();
		viewers = new ArrayList<String>();
		messageIDCaches = new ArrayList<IncomingMessageEvent>();
	}

	public List<String> getModerators() {
		return moderators;
	}

	public void setModerators(List<String> moderators) {
		this.moderators = moderators;
	}

	public void addModerator(String moderator) {
		this.moderators.add(moderator);
	}

	public void removeModerator(String moderator) {
		this.moderators.remove(moderator);
	}

	public List<String> getViewers() {
		return viewers;
	}

	public void setViewers(List<String> viewers) {
		this.viewers = viewers;
	}

	public void addViewer(String viewer) {
		this.viewers.add(viewer);
	}

	public void removeViewer(String viewer) {
		this.viewers.remove(viewer);
	}

	public List<IncomingMessageEvent> getMessageIDCaches() {
		return messageIDCaches;
	}

	public void setMessageIDCaches(List<IncomingMessageEvent> messageIDCache) {
		this.messageIDCaches = messageIDCache;
	}

	public void addMessageIDCaches(IncomingMessageEvent messageIDCache) {
		this.messageIDCaches.add(messageIDCache);
	}

	public List<String> getLiveEvents() {
		return liveEvents;
	}

	public void setLiveEvents(List<String> liveEvents) {
		this.liveEvents = liveEvents;
	}

	public String getBotName() {
		return botName;
	}

	public void setBotName(String botName) {
		this.botName = botName;
	}

	public int getChannelID() {
		return channelID;
	}

	public void setChannelID(int channelID) {
		this.channelID = channelID;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
}
