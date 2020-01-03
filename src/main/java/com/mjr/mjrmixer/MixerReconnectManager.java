package com.mjr.mjrmixer;

public class MixerReconnectManager {
	private static MixerReconnectThread mixerReconnectThread;

	public static MixerReconnectThread getMixerReconnectThread() {
		return mixerReconnectThread;
	}

	public static void initMixerReconnectThreadIfDoesntExist() {
		if (mixerReconnectThread == null) {
			mixerReconnectThread = new MixerReconnectThread(5);
			mixerReconnectThread.start();
		}
	}
}
