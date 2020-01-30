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
import com.mixer.api.resource.MixerUser.Role;
import com.mixer.api.resource.chat.MixerChat;
import com.mixer.api.resource.chat.events.ChatDisconnectEvent;
import com.mixer.api.resource.chat.events.EventHandler;
import com.mixer.api.resource.chat.events.IncomingMessageEvent;
import com.mixer.api.resource.chat.events.UserJoinEvent;
import com.mixer.api.resource.chat.events.UserLeaveEvent;
import com.mixer.api.resource.chat.events.WelcomeEvent;
import com.mixer.api.resource.chat.events.data.MessageComponent.MessageTextComponent;
import com.mixer.api.resource.chat.methods.AuthenticateMessage;
import com.mixer.api.resource.chat.methods.ChatSendMethod;
import com.mixer.api.resource.chat.replies.AuthenticationReply;
import com.mixer.api.resource.chat.replies.ReplyHandler;
import com.mixer.api.resource.chat.ws.MixerChatConnectable;
import com.mixer.api.resource.constellation.MixerConstellation;
import com.mixer.api.resource.constellation.events.ConstellationDisconnectEvent;
import com.mixer.api.resource.constellation.events.HelloEvent;
import com.mixer.api.resource.constellation.events.LiveEvent;
import com.mixer.api.resource.constellation.events.data.ConstellationDisconnectData;
import com.mixer.api.resource.constellation.methods.LiveSubscribeMethod;
import com.mixer.api.resource.constellation.methods.data.LiveRequestData;
import com.mixer.api.resource.constellation.ws.MixerConstellationConnectable;
import com.mixer.api.services.impl.ChatService;
import com.mixer.api.services.impl.UsersService;
import com.mjr.mjrmixer.chatMethods.ChatDeleteMethod;
import com.mjr.mjrmixer.events.DisconnectEvent.DisconnectType;
import com.mjr.mjrmixer.events.ReconnectEvent.ReconnectType;

public abstract class MixerBotBase {
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
	private List<String> liveEvents;

	private String botName;

	private boolean connected = false;
	private boolean authenticated = false;

	private int channelID;
	private int userID;
	private String channelName;

	public MixerBotBase(String clientId, String authcode, String botName) {
		this.botName = botName;
		mixer = new MixerAPI(clientId, authcode);
		moderators = new ArrayList<String>();
		viewers = new ArrayList<String>();
		messageIDCache = new ArrayList<IncomingMessageEvent>();
	}

	private void authWithMixer() {
		connectable.send(AuthenticateMessage.from(connectedChannel.channel, user, chat.authkey), new ReplyHandler<AuthenticationReply>() {
			@Override
			public void onSuccess(AuthenticationReply reply) {
				authenticated = true;
				MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Authenticated to Mixer");
			}

			@Override
			public void onFailure(Throwable err) {
				MixerEventHooks.triggerOnErrorEvent("Failed to Authenticate with Mixer", err);
			}
		});
	}

	private void requestEventsConstellation() {
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "ConstellationEvent Requesting events");
		LiveRequestData test = new LiveRequestData();
		test.events = (ArrayList<String>) this.liveEvents;
		LiveSubscribeMethod live = new LiveSubscribeMethod();
		live.params = test;
		constellationConnectable.send(live);
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "ConstellationEvent Requested events");
	}

	/**
	 * Used to connect the bot to Mixer & join a channel
	 * 
	 * @param events
	 * @param channelID
	 *
	 * @param userID (DONT USE channelID)
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 * @return channelName
	 */
	protected void joinMixerChannel(int userID) throws InterruptedException, ExecutionException, IOException {
		joinMixerChannel(userID, new ArrayList<String>());
	}

	/**
	 * Used to connect the bot to Mixer & join a channel
	 *
	 * @param userID (DONT USE channelID)
	 * @param liveEvents
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 * @return channelName
	 */
	protected void joinMixerChannel(int userID, final ArrayList<String> eventsInput) throws InterruptedException, ExecutionException, IOException {
		this.liveEvents = eventsInput;
		do {
		} while (mixer == null);
		MixerReconnectManager.initMixerReconnectThreadIfDoesntExist();

		user = mixer.use(UsersService.class).getCurrent().get();
		if (userID != -1) {
			connectedChannel = mixer.use(UsersService.class).findOne(userID).get();
		} else {
			MixerEventHooks.triggerOnErrorEvent("No channel for the provided channel id", null);
			return;
		}
		this.setUserID(userID);
		this.setChannelID(connectedChannel.channel.id);
		this.setChannelName(connectedChannel.username);
		chat = mixer.use(ChatService.class).findOne(connectedChannel.channel.id).get();
		connectable = chat.connectable(this.mixer);
		connected = connectable.connect();
		connectable.on(WelcomeEvent.class, new EventHandler<WelcomeEvent>() {
			@Override
			public void onEvent(WelcomeEvent event) {
				MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Connected Server " + event.data.server);
				MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Trying to authenticate to Mixer for channel " + getChannelID());
				authWithMixer();
			}
		});
		if (!liveEvents.isEmpty()) {
			MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Starting Setting up of Constellation Events: HelloEvent, LiveEvent, ConstellationDisconnectEvent");
			constellation = new MixerConstellation();
			constellationConnectable = constellation.connectable(mixer);
			constellationConnectable.connect();
			constellationConnectable.on(HelloEvent.class, new com.mixer.api.resource.constellation.events.EventHandler<HelloEvent>() {
				@Override
				public void onEvent(HelloEvent event) {
					requestEventsConstellation();
				}
			});
			constellationConnectable.on(LiveEvent.class, new com.mixer.api.resource.constellation.events.EventHandler<LiveEvent>() {
				@Override
				public void onEvent(LiveEvent event) {
					onLiveEvent(event);
				}
			});
			constellationConnectable.on(ConstellationDisconnectEvent.class, new com.mixer.api.resource.constellation.events.EventHandler<ConstellationDisconnectEvent>() {
				@Override
				public void onEvent(ConstellationDisconnectEvent event) {
					MixerEventHooks.triggerOnDisconnectEvent(DisconnectType.CONSTELLATION, getChannelName(), getChannelID(), event.data);
				}
			});
			MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Finished Setting up of Constellation Events: HelloEvent, LiveEvent, ConstellationDisconnectEvent");
		}
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "The channel id for the channel you're joining is " + connectedChannel.channel.id);
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Starting Setting up of Chat Events: IncomingMessageEvent, UserJoinEvent, UserLeaveEvent, ChatDisconnectEvent");
		connectable.on(IncomingMessageEvent.class, new EventHandler<IncomingMessageEvent>() {
			@Override
			public void onEvent(IncomingMessageEvent event) {
				if (messageIDCache.size() >= 100) {
					messageIDCache.remove(0);
					MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Removed oldest message from the message cache due to limit of 100 messages in the cache has been reached");
				}
				messageIDCache.add(event);
				String msg = "";
				List<String> emotes = new ArrayList<String>();
				List<String> links = new ArrayList<String>();
				for (MessageTextComponent msgp : event.data.message.message) {
					if (msgp.type != null && msgp.type.equals(MessageTextComponent.Type.EMOTICON))
						emotes.add(msgp.text);
					else if (msgp.type != null && msgp.type.equals(MessageTextComponent.Type.LINK))
						links.add(msgp.text);
					msg += msgp.text;
				}
				MixerEventHooks.triggerOnMessageEvent(msg, emotes, links, connectedChannel.username, event.data.channel, event.data.userName, event.data.userId, event.data.userRoles);
			}
		});
		connectable.on(UserJoinEvent.class, new EventHandler<UserJoinEvent>() {
			@Override
			public void onEvent(UserJoinEvent event) {
				if (!viewers.contains(event.data.username.toLowerCase()))
					viewers.add(event.data.username.toLowerCase());
				MixerEventHooks.triggerOnJoinEvent(connectedChannel.username, connectedChannel.channel.id, event.data.username, Integer.parseInt(event.data.id));
			}
		});
		connectable.on(UserLeaveEvent.class, new EventHandler<UserLeaveEvent>() {
			@Override
			public void onEvent(UserLeaveEvent event) {
				if (viewers.contains(event.data.username.toLowerCase()))
					viewers.remove(event.data.username.toLowerCase());
				MixerEventHooks.triggerOnPartEvent(connectedChannel.username, connectedChannel.channel.id, event.data.username, Integer.parseInt(event.data.id));
			}
		});
		connectable.on(ChatDisconnectEvent.class, new EventHandler<ChatDisconnectEvent>() {
			@Override
			public void onEvent(ChatDisconnectEvent event) {
				MixerEventHooks.triggerOnDisconnectEvent(DisconnectType.CHAT, getChannelName(), getChannelID(), event.data);
			}
		});
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Finished Setting up of Chat Events: IncomingMessageEvent, UserJoinEvent, UserLeaveEvent, ChatDisconnectEvent");

		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Loading Moderators & Viewers");
		try {
			this.loadModerators();
			this.loadViewers();
		} catch (IOException e) {
			MixerEventHooks.triggerOnErrorEvent("Error happened when trying to load moderators/viewers", e);
		}
		if (connected && authenticated)
			MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Connected & Authenticated to Mixer");
		else if (connected && !authenticated)
			MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Connected to Mixer but not Authenticated");
		else if (authenticated && !connected)
			MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Authenticated to Mixer but not connected");
	}

	/**
	 * Used to disconnect the bot from a channel's chat & constellation
	 */
	public final void disconnectAll() {
		this.disconnectChat();
		this.disconnectConstellation();
	}

	/**
	 * Used to disconnect the bot from a channel's chat
	 */
	public final void disconnectChat() {
		MixerEventHooks.triggerOnDisconnectEvent(DisconnectType.CHAT, this.channelName, this.channelID, new ConstellationDisconnectData(1000, "Requested Disconnect By Bot", false));
		connectable.disconnect();
		viewers.clear();
		moderators.clear();
		messageIDCache.clear();
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Disconnected from Mixer Chat!");
		this.connected = false;
		this.authenticated = false;
	}

	/**
	 * Used to reconnect the bot from a channel's chat
	 */
	public final void reconnectChat() {
		MixerEventHooks.triggerOnReconnectEvent(ReconnectType.CHAT, this.channelName, this.channelID);
		if (this.connectable.connect())
			MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Reconnected to Mixer Chat!");
		else
			MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Failed to be reconnected to Mixer Chat!");
	}

	/**
	 * Used to disconnect the bot from a channel's constellation
	 */
	public final void disconnectConstellation() {
		MixerEventHooks.triggerOnDisconnectEvent(DisconnectType.CONSTELLATION, this.channelName, this.channelID, new ConstellationDisconnectData(1000, "Requested Disconnect By Bot", false));
		this.constellationConnectable.disconnect();
	}

	/**
	 * Used to reconnect the bot from a channel's constellation
	 */
	public final void reconnectConstellation() {
		MixerEventHooks.triggerOnReconnectEvent(ReconnectType.CONSTELLATION, this.channelName, this.channelID);
		if (this.constellationConnectable.connect())
			MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Reconnected to Mixer Constellation!");
		else
			MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Failed to be reconnected to Mixer Constellation!");
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
			MixerEventHooks.triggerOnErrorEvent("Error happened when trying to ban User: " + user, e);
			return;
		}
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Banned " + user);
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
			MixerEventHooks.triggerOnErrorEvent("Error happened when trying to unban User: " + user, e);
		}
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "unban " + user);
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
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Found " + moderators.size() + " moderators!");
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
		MixerEventHooks.triggerOnInfoEvent(getChannelName(), getChannelID(), "Found " + viewers.size() + " viewers!");
	}

	public boolean isUserMod(String user) {
		if (connectedChannel == null) {
			return false;
		}
		return (moderators != null) && moderators.contains(user.toLowerCase());
	}

	public boolean isChatConnectionClosed() {
		return this.connectable.isClosed();
	}

	public boolean isConstellationConnectionClosed() {
		return this.constellationConnectable.isClosed();
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public String getBotName() {
		return this.botName;
	}

	public List<String> getModerators() {
		return moderators;
	}

	public List<String> getViewers() {
		return viewers;
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
			MixerEventHooks.triggerOnErrorEvent("Error happened when trying to update ConnectedChannel", e);
		}
	}

	public int getChannelID() {
		return this.channelID;
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
		return this.channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public abstract void onMessage(String sender, int senderID, List<Role> userRoles, String message, List<String> emotes, List<String> links);

	public abstract void onJoin(String sender, int senderID);

	public abstract void onPart(String sender, int senderID);

	public abstract void onLiveEvent(LiveEvent event);
}
