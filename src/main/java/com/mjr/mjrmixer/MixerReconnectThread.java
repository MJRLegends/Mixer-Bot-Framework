package com.mjr.mjrmixer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MixerReconnectThread extends Thread {
	private List<MixerBotBase> mixerBots = new ArrayList<MixerBotBase>();

	private int mixerBotSleepTime;

	public MixerReconnectThread(int mixerBotSleepTime) {
		super("Mixer Framework Reconnect Thread");
		this.mixerBotSleepTime = mixerBotSleepTime;
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (mixerBots.size() != 0) {

					Iterator<MixerBotBase> iterator = mixerBots.iterator();
					while (iterator.hasNext()) {
						MixerBotBase bot = iterator.next();
						boolean done = false;
						if (bot.isChatConnectionClosed()) {
							bot.reconnectChat();
							done = true;
						}
						if (bot.isConstellationConnectionClosed()) {
							bot.reconnectChat();
							done = true;
						}
						if (done)
							iterator.remove();
						if (mixerBots.size() != 0)
							Thread.sleep(mixerBotSleepTime * 1000);
					}
				}

				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {
					MixerEventHooks.triggerOnErrorEvent("Mixer Framework: ReconnectThread Errored", e);

				}
			} catch (Exception e) {
				MixerEventHooks.triggerOnErrorEvent("Mixer Framework: ReconnectThread Errored", e);
			}
		}

	}

	public List<MixerBotBase> getMixerBotBases() {
		return mixerBots;
	}

	public void addMixerBotBase(MixerBotBase bot) {
		this.mixerBots.add(bot);
	}

	public int getMixerBotBaseSleepTime() {
		return mixerBotSleepTime;
	}
}
