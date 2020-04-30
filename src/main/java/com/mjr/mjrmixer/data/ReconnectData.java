package com.mjr.mjrmixer.data;

public class ReconnectData {
	private long lastDisconnectTimeChat;
	private long lastReconnectTimeChat;
	private int lastReconnectCodeChat;

	private long lastDisconnectTimeConstel;
	private long lastReconnectTimeConstel;
	private int lastReconnectCodeConstel;

	private int numberOfFailedAuths = 0;

	public long getLastDisconnectTimeChat() {
		return lastDisconnectTimeChat;
	}

	public void setLastDisconnectTimeChat(long lastDisconnectTimeChat) {
		this.lastDisconnectTimeChat = lastDisconnectTimeChat;
	}

	public long getLastReconnectTimeChat() {
		return lastReconnectTimeChat;
	}

	public void setLastReconnectTimeChat(long lastReconnectTimeChat) {
		this.lastReconnectTimeChat = lastReconnectTimeChat;
	}

	public int getLastReconnectCodeChat() {
		return lastReconnectCodeChat;
	}

	public void setLastReconnectCodeChat(int lastReconnectCodeChat) {
		this.lastReconnectCodeChat = lastReconnectCodeChat;
	}

	public long getLastDisconnectTimeConstel() {
		return lastDisconnectTimeConstel;
	}

	public void setLastDisconnectTimeConstel(long lastDisconnectTimeConstel) {
		this.lastDisconnectTimeConstel = lastDisconnectTimeConstel;
	}

	public long getLastReconnectTimeConstel() {
		return lastReconnectTimeConstel;
	}

	public void setLastReconnectTimeConstel(long lastReconnectTimeConstel) {
		this.lastReconnectTimeConstel = lastReconnectTimeConstel;
	}

	public int getLastReconnectCodeConstel() {
		return lastReconnectCodeConstel;
	}

	public void setLastReconnectCodeConstel(int lastReconnectCodeConstel) {
		this.lastReconnectCodeConstel = lastReconnectCodeConstel;
	}

	public int getNumberOfFailedAuths() {
		return numberOfFailedAuths;
	}

	public void setNumberOfFailedAuths(int numberOfFailedAuths) {
		this.numberOfFailedAuths = numberOfFailedAuths;
	}

	public int increaseNumberOfFailedAuths() {
		return this.numberOfFailedAuths = getNumberOfFailedAuths()+1;
	}
}
