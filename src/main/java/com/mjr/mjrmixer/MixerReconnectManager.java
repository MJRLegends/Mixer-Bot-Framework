package com.mjr.mjrmixer;

public class MixerReconnectManager {
	private static MixerReconnectThread mixerReconnectThread;

	public static MixerReconnectThread getMixerReconnectThread() {
		return mixerReconnectThread;
	}

	public static void initMixerReconnectThreadIfDoesntExist() {
		if (mixerReconnectThread == null || mixerReconnectThread.getState() == Thread.State.TERMINATED) {
			mixerReconnectThread = new MixerReconnectThread(10);
			mixerReconnectThread.start();
		}
	}
}
