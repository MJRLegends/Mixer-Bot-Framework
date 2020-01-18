package com.mjr.mjrmixer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MixerReconnectThread extends Thread {
	private List<MixerBotBase> mixerBotsChat = new ArrayList<MixerBotBase>();
	private List<MixerBotBase> mixerBotsConstell = new ArrayList<MixerBotBase>();

	private int mixerBotSleepTime;

	public MixerReconnectThread(int mixerBotSleepTime) {
		super("Mixer Framework Reconnect Thread");
		this.mixerBotSleepTime = mixerBotSleepTime;
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (mixerBotsChat.size() != 0) {
					Iterator<MixerBotBase> iterator = mixerBotsChat.iterator();
					while (iterator.hasNext()) {
						MixerBotBase bot = iterator.next();
						boolean done = false;
						if (bot.isChatConnectionClosed()) {
							bot.reconnectChat();
							done = true;
						}
						if (done)
							iterator.remove();
						if (mixerBotsChat.size() != 0)
							Thread.sleep(mixerBotSleepTime * 1000);
					}
				}
				if (mixerBotsConstell.size() != 0) {
					Iterator<MixerBotBase> iterator = mixerBotsConstell.iterator();
					while (iterator.hasNext()) {
						MixerBotBase bot = iterator.next();
						boolean done = false;
						if (bot.isConstellationConnectionClosed()) {
							bot.reconnectConstellation();
							done = true;
						}
						if (done)
							iterator.remove();
						if (mixerBotsConstell.size() != 0)
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

	public List<MixerBotBase> getMixerBotChatBases() {
		return mixerBotsChat;
	}

	public List<MixerBotBase> getMixerBotConstellationBases() {
		return mixerBotsConstell;
	}

	public void addMixerBotChatBase(MixerBotBase bot) {
		if (!mixerBotsChat.contains(bot))
			this.mixerBotsChat.add(bot);
	}

	public void addMixerBotChatConstellation(MixerBotBase bot) {
		if (!mixerBotsConstell.contains(bot))
			this.mixerBotsConstell.add(bot);
	}

	public int getMixerBotBaseSleepTime() {
		return mixerBotSleepTime;
	}
}
