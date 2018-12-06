package com.mjr.mjrmixer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.mixer.api.MixerAPI;
import com.mixer.api.resource.MixerUser;
import com.mixer.api.resource.chat.MixerChat;
import com.mixer.api.resource.chat.events.EventHandler;
import com.mixer.api.resource.chat.events.IncomingMessageEvent;
import com.mixer.api.resource.chat.events.UserJoinEvent;
import com.mixer.api.resource.chat.events.UserLeaveEvent;
import com.mixer.api.resource.chat.events.data.MessageComponent.MessageTextComponent;
import com.mixer.api.resource.chat.events.data.MessageComponent.MessageTextComponent.Type;
import com.mixer.api.resource.chat.methods.AuthenticateMessage;
import com.mixer.api.resource.chat.methods.ChatSendMethod;
import com.mixer.api.resource.chat.replies.AuthenticationReply;
import com.mixer.api.resource.chat.replies.ReplyHandler;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;
import com.mixer.api.resource.constellation.MixerConstellation;
import com.mixer.api.resource.constellation.events.LiveEvent;
import com.mixer.api.resource.constellation.methods.LiveSubscribeMethod;
import com.mixer.api.resource.constellation.methods.data.LiveRequestData;
import com.mixer.api.resource.constellation.ws.MixerConstellationConnectable;
import com.mixer.api.response.users.UserSearchResponse;
import com.mixer.api.services.impl.ChatService;
import com.mixer.api.services.impl.UsersService;
import com.mjr.mjrmixer.chatMethods.ChatDeleteMethod;

public abstract class MJR_MixerBot {
	private MixerUser user;
	private MixerUser connectedChannel;
	private MixerChat chat;
	private MixerConstellation constellation;
	private MixerChatConnectable connectable;
	private MixerConstellationConnectable constellationConnectable;
	private MixerAPI mixer;

	private List<String> moderators;
	private List<String> viewers;
	private List<IncomingMessageEvent> messageIDCache;

	private String name;

	private boolean connected = false;
	private boolean authenticated = false;
	private boolean debugMessages = false;

	private List<String> outputMessages = new ArrayList<String>();

	public MJR_MixerBot(String clientId, String authcode, String name) {
		this.name = name;
		mixer = new MixerAPI(clientId, authcode);
		moderators = new ArrayList<String>();
		viewers = new ArrayList<String>();
		messageIDCache = new ArrayList<IncomingMessageEvent>();
	}

	/**
	 * Used to connect the bot to Mixer & join a channel
	 *
	 * @param channel
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	protected synchronized void joinMixerChannel(String channel) throws InterruptedException, ExecutionException, IOException {
		joinMixerChannel(channel, new ArrayList<String>());
	}

	/**
	 * Used to connect the bot to Mixer & join a channel
	 *
	 * @param channel
	 * @param liveEvents
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	protected synchronized void joinMixerChannel(String channel, ArrayList<String> liveEvents) throws InterruptedException, ExecutionException, IOException {
		if (debugMessages)
			addOutputMessage("Connecting to channel: " + channel);

		user = mixer.use(UsersService.class).getCurrent().get();

		UserSearchResponse search = mixer.use(UsersService.class).search(channel.toLowerCase()).get();
		if (search.size() > 0) {
			connectedChannel = mixer.use(UsersService.class).findOne(search.get(0).id).get();
		} else {
			if (debugMessages)
				addOutputMessage("No channel found!");
			return;
		}

		chat = mixer.use(ChatService.class).findOne(connectedChannel.channel.id).get();
		connectable = chat.connectable(mixer);
		connected = connectable.connect();
		if (!liveEvents.isEmpty()) {
			constellation = new MixerConstellation();
			constellationConnectable = constellation.connectable(mixer);
			constellationConnectable.connect();
			LiveRequestData test = new LiveRequestData();
			test.events = liveEvents;
			LiveSubscribeMethod live = new LiveSubscribeMethod();
			live.params = test;
			constellationConnectable.send(live);
		}
		if (connected) {
			if (debugMessages) {
				addOutputMessage("The channel id for the channel you're joining is " + connectedChannel.channel.id);
				addOutputMessage("Trying to authenticate to Mixer");
			}
			connectable.send(AuthenticateMessage.from(connectedChannel.channel, user, chat.authkey), new ReplyHandler<AuthenticationReply>() {
				@Override
				public void onSuccess(AuthenticationReply reply) {
					authenticated = true;
					if (debugMessages) {
						addOutputMessage("Authenticated to Mixer");
					}

				}

				@Override
				public void onFailure(Throwable err) {
					if (debugMessages)
						addOutputMessage(err.getMessage());
				}
			});
		}

		if (debugMessages) {
			addOutputMessage("Setting up IncomingMessageEvent");
		}
		connectable.on(IncomingMessageEvent.class, new EventHandler<IncomingMessageEvent>() {
			@Override
			public void onEvent(IncomingMessageEvent event) {
				if (messageIDCache.size() >= 100) {
					messageIDCache.remove(0);
					if (debugMessages)
						addOutputMessage("Removed oldest message from the message cache due to limit of 100 messages in the cache has been reached");
				}
				messageIDCache.add(event);
				String msg = "";
				for (MessageTextComponent msgp : event.data.message.message) {
					if (msgp.equals(Type.LINK)) {
						return;
					}
					msg += msgp.data;
				}
				onMessage(event.data.userName, msg);
			}
		});
		if (debugMessages) {
			addOutputMessage("Setting up UserJoinEvent");
		}
		connectable.on(UserJoinEvent.class, new EventHandler<UserJoinEvent>() {
			@Override
			public void onEvent(UserJoinEvent event) {
				if (!viewers.contains(event.data.username.toLowerCase()))
					viewers.add(event.data.username.toLowerCase());
				onJoin(event.data.username);
			}
		});
		if (debugMessages) {
			addOutputMessage("Setting up UserLeaveEvent");
		}
		connectable.on(UserLeaveEvent.class, new EventHandler<UserLeaveEvent>() {
			@Override
			public void onEvent(UserLeaveEvent event) {
				if (viewers.contains(event.data.username.toLowerCase()))
					viewers.remove(event.data.username.toLowerCase());
				onPart(event.data.username);
			}
		});
		if (debugMessages) {
			addOutputMessage("Loading Moderators & Viewers");
		}
		if (!liveEvents.isEmpty()) {
			constellationConnectable.on(LiveEvent.class, new com.mixer.api.resource.constellation.events.EventHandler<LiveEvent>() {
				@Override
				public void onEvent(LiveEvent event) {
					onLiveEvent(event);
				}
			});
			if (debugMessages) {
				addOutputMessage("Setting up ConstellationEvent");
			}
		}
		try {
			this.loadModerators();
			this.loadViewers();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (debugMessages) {
			if (connected && authenticated)
				addOutputMessage("Connected & Authenticated to Mixer");
			else if (connected && !authenticated)
				addOutputMessage("Connected to Mixer but not Authenticated");
			else if (authenticated && !connected)
				addOutputMessage("Authenticated to Mixer but not connected");
		}
	}

	/**
	 * Used to disconnect the bot from a channel
	 */
	public final synchronized void disconnect() {
		connectable.disconnect();
		viewers.clear();
		moderators.clear();
		messageIDCache.clear();
		if (debugMessages)
			addOutputMessage("Disconnected from Mixer!");
		this.connected = false;
		this.authenticated = false;
	}

	/**
	 * Send a message to the connected channels chat
	 *
	 * @param msg
	 */
	public void sendMessage(String msg) {
		connectable.send(ChatSendMethod.of(msg));
	}

	/**
	 * Deletes all messages in connected channels chat for a user
	 *
	 * @param user
	 * @return
	 */
	public String deleteUserMessages(String user) {
		int messagesDeleted = 0;
		for (IncomingMessageEvent message : messageIDCache) {
			if (user.equalsIgnoreCase(message.data.userName)) {
				connectable.send(ChatDeleteMethod.of(message.data));
				messagesDeleted++;
			}
		}
		if (messagesDeleted > 0) {
			return "Deleted all of " + user + " messages!";
		}
		return "";
	}

	/**
	 * Deletes the last message in connected channels chat for a user
	 *
	 * @param user
	 * @return
	 */
	public String deleteLastMessageForUser(String user) {
		int lastMessage = 0;
		for (int i = 0; i < messageIDCache.size(); i++) {
			if (user.equalsIgnoreCase(messageIDCache.get(i).data.userName)) {
				lastMessage = i;
			}
		}
		connectable.send(ChatDeleteMethod.of(messageIDCache.get(lastMessage).data));
		return "Deleted last message for " + user + "!";
	}

	/**
	 * Deletes the last message in connected channels chat for any user
	 *
	 * @return
	 */
	public String deleteLastMessage() {
		connectable.send(ChatDeleteMethod.of(messageIDCache.get(messageIDCache.size() - 1).data));
		return "Deleted last message!";
	}

	/**
	 * Used to ban a user from the connected channels chat
	 *
	 * @param user
	 */
	public void ban(String user) {
		deleteUserMessages(user);
		String path = mixer.basePath.resolve("channels/" + connectedChannel.channel.id + "/users/" + user.toLowerCase()).toString();
		HashMap<String, Object> map = new HashMap<>();
		map.put("add", new String[] { "Banned" });
		try {
			Object result = mixer.http.patch(path, Object.class, map).get(4, TimeUnit.SECONDS);
			if ((result != null) && result.toString().contains("username")) {
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			addOutputMessage(e.getMessage());
			return;
		}
		if (debugMessages)
			addOutputMessage("Banned " + user);
	}

	/**
	 * Used to unban a user from the connected channels chat
	 *
	 * @param user
	 */
	public void unban(String user) {
		String path = mixer.basePath.resolve("channels/" + connectedChannel.channel.id + "/users/" + user.toLowerCase()).toString();
		HashMap<String, Object> map = new HashMap<>();
		map.put("add", new String[] { "" });
		try {
			Object result = mixer.http.patch(path, Object.class, map).get(4, TimeUnit.SECONDS);
			if ((result != null) && result.toString().contains("username")) {
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			addOutputMessage(e.getMessage());
		}
		if (debugMessages)
			addOutputMessage("unban " + user);
	}

	private void loadModerators() throws IOException {
		String result = "";
		URL url = new URL("https://mixer.com/api/v1/channels/" + connectedChannel.channel.id + "/users");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line = "";
		while ((line = reader.readLine()) != null) {
			result += line;
		}
		reader.close();
		boolean done = false;
		String username = "";
		String permission = "";
		while (done == false) {
			if (result.contains("username")) {
				username = result.substring(result.indexOf("username") + 11);
				permission = username.substring(username.indexOf("name") + 7);
				permission = permission.substring(0, permission.indexOf("}") - 1);
				username = username.substring(0, username.indexOf(",") - 1);
				result = result.substring(result.indexOf("username") + 8);
				if (permission.contains("Mod")) {
					if (!moderators.contains(username.toLowerCase()))
						moderators.add(username.toLowerCase());
				}
			} else
				done = true;
		}
		if (debugMessages) {
			addOutputMessage("Found " + moderators.size() + " moderators!");
		}
	}

	private void loadViewers() throws IOException {
		String result = "";
		URL url = new URL("https://mixer.com/api/v1/chats/" + connectedChannel.channel.id + "/users");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line = "";
		while ((line = reader.readLine()) != null) {
			result += line;
		}
		reader.close();
		boolean done = false;
		String username = "";
		while (done == false) {
			if (result.contains("userName")) {
				username = result.substring(result.indexOf("userName") + 11);
				username = username.substring(0, username.indexOf("\""));
				result = result.substring(result.indexOf(username));
				if (!viewers.contains(username.toLowerCase()))
					viewers.add(username.toLowerCase());
			} else
				done = true;
		}
		if (debugMessages) {
			addOutputMessage("Found " + viewers.size() + " viewers!");
		}
	}

	public boolean isUserMod(String user) {
		if (connectedChannel == null) {
			return false;
		}
		return (moderators != null) && moderators.contains(user.toLowerCase());
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public String getBotName() {
		return this.name;
	}

	public List<String> getModerators() {
		return moderators;
	}

	public List<String> getViewers() {
		return viewers;
	}

	protected void setdebug(boolean value) {
		debugMessages = value;
	}

	public List<String> getOutputMessages() {
		return outputMessages;
	}

	public void setOutputMessages(List<String> outputMessages) {
		this.outputMessages = outputMessages;
		this.onDebugMessage();
	}

	public void addOutputMessage(String outputMessage) {
		this.outputMessages.add(outputMessage);
		this.onDebugMessage();
	}

	public void removeOutputMessage(String outputMessage) {
		this.outputMessages.remove(outputMessage);
	}

	public void clearOutputMessages() {
		this.outputMessages.clear();
	}

	protected void addViewer(String viewer) {
		if (!viewers.contains(viewer.toLowerCase()) && !viewer.equals(""))
			this.viewers.add(viewer.toLowerCase());
	}

	protected void removeViewer(String viewer) {
		if (viewers.contains(viewer.toLowerCase()))
			viewers.remove(viewer.toLowerCase());
	}

	protected void addModerator(String moderator) {
		if (!moderators.contains(moderator.toLowerCase()) && !moderator.equals(""))
			this.moderators.add(moderator.toLowerCase());
	}

	protected void removeModerator(String moderator) {
		if (moderators.contains(moderator.toLowerCase()))
			moderators.remove(moderator.toLowerCase());
	}

	public int getNumOfFollowers() {
		updateConnectedChannel();
		return connectedChannel.channel.numFollowers;
	}

	public String getAudience() {
		updateConnectedChannel();
		return connectedChannel.channel.audience.toString();
	}

	public int getNumOfTotalViewers() {
		updateConnectedChannel();
		return connectedChannel.channel.viewersTotal;
	}

	public boolean isStreaming() {
		updateConnectedChannel();
		return connectedChannel.channel.online;
	}

	public boolean isPartnered() {
		updateConnectedChannel();
		return connectedChannel.channel.partnered;
	}

	public Date getUpdatedAt() {
		updateConnectedChannel();
		return connectedChannel.channel.updatedAt;
	}

	public void updateConnectedChannel() {
		try {
			connectedChannel = mixer.use(UsersService.class).findOne(connectedChannel.id).get(); // Used to update API info
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	protected abstract void onMessage(String sender, String message);

	protected abstract void onJoin(String sender);

	protected abstract void onPart(String sender);

	protected abstract void onDebugMessage();

	protected abstract void onLiveEvent(LiveEvent event);
}
