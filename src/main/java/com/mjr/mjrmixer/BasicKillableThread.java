package com.mjr.mjrmixer;

public class BasicKillableThread extends Thread{

	private boolean killThread;

	public BasicKillableThread(String name) {
		super(name);
	}

	public boolean isKillThread() {
		return killThread;
	}

	public void killThread(){
		this.killThread = true;
	}
}
