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
import com.mixer.api.resource.chat.events.*;
import com.mixer.api.resource.chat.events.data.ChatDisconnectData;
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
import com.mjr.mjrmixer.data.CoreData;
import com.mjr.mjrmixer.data.ReconnectData;
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

	private boolean connected = false;
	private boolean authenticated = false;

	private ReconnectData reconnectData;
	private CoreData coreData;

	public MixerBotBase(String clientId, String authcode, String botName) {
		mixer = new MixerAPI(clientId, authcode);
		reconnectData = new ReconnectData();
		coreData = new CoreData(botName);
	}

	private void authWithMixer() {
		try {
			int amount = this.reconnectData.increaseNumberOfFailedAuths();
			if (connectedChannel == null)
				MixerEventHooks.triggerOnFailedAuthEvent(this, "ConnectedChannel was null when trying to Authenticate with Mixer, Mixer Framework will try to fix this!", amount, getCoreData().getChannelName(), getCoreData().getChannelID());
			if (user == null)
				MixerEventHooks.triggerOnFailedAuthEvent(this, "User was null when trying to Authenticate with Mixer", amount, getCoreData().getChannelName(), getCoreData().getChannelID());
			if (chat == null)
				MixerEventHooks.triggerOnFailedAuthEvent(this, "User was null when trying to Authenticate with Mixer", amount, getCoreData().getChannelName(), getCoreData().getChannelID());
			else if (chat.authkey == null)
				MixerEventHooks.triggerOnFailedAuthEvent(this, "User Auth Key was null when trying to Authenticate with Mixer", amount, getCoreData().getChannelName(), getCoreData().getChannelID());
			else {
				connectable.send(AuthenticateMessage.from(connectedChannel.channel, user, chat.authkey), new ReplyHandler<AuthenticationReply>() {
					@Override
					public void onSuccess(AuthenticationReply reply) {
						try {
							MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Error:" + reply.error + ", Authed: " + reply.authenticated + ", ID: " + reply.id + ", Type: " + reply.type);
							if (reply.error == null) {
								MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Authenticated with Mixer, ID: " + reply.id);
								authenticated = true;
								reconnectData.setNumberOfFailedAuths(0);
							} else {
								authenticated = false;
								MixerEventHooks.triggerOnFailedAuthEvent(null, "Failed to Authenticate with Mixer, due to Error: " + reply.error, amount, getCoreData().getChannelName(), getCoreData().getChannelID());
							}
						} catch (Exception e) {
							MixerEventHooks.triggerOnErrorEvent("Auth With Mixer Error", e);
						}
					}

					@Override
					public void onFailure(Throwable err) {
						try {
							authenticated = false;
							MixerEventHooks.triggerOnErrorEvent("", err);
							MixerEventHooks.triggerOnFailedAuthEvent(null, "Failed to Authenticate with Mixer, due to an exception", amount, getCoreData().getChannelName(), getCoreData().getChannelID());
						} catch (Exception e) {
							MixerEventHooks.triggerOnErrorEvent("Auth With Mixer Error", e);
						}
					}
				});
			}
		} catch (Exception e) {
			MixerEventHooks.triggerOnErrorEvent("Auth With Mixer Error", e);
		}
	}

	private void requestEventsConstellation() {
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "ConstellationEvent Requesting events");
		LiveRequestData test = new LiveRequestData();
		test.events = (ArrayList<String>) this.getCoreData().getLiveEvents();
		LiveSubscribeMethod live = new LiveSubscribeMethod();
		live.params = test;
		constellationConnectable.send(live);
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "ConstellationEvent Requested events");
	}

	/**
	 * Used to connect the bot to Mixer & join a channel
	 *
	 * @param userID (DONT USE channelID)
	 * @return channelName
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	protected void joinMixerChannel(int userID) throws InterruptedException, ExecutionException, IOException {
		joinMixerChannel(userID, new ArrayList<String>());
	}

	/**
	 * Used to connect the bot to Mixer & join a channel
	 *
	 * @param userID      (DONT USE channelID)
	 * @param eventsInput
	 * @return channelName
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	protected void joinMixerChannel(int userID, final ArrayList<String> eventsInput) throws InterruptedException, ExecutionException, IOException {
		getCoreData().setLiveEvents(eventsInput);
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
		getCoreData().setUserID(userID);
		getCoreData().setChannelID(connectedChannel.channel.id);
		getCoreData().setChannelName(connectedChannel.username);
		chat = mixer.use(ChatService.class).findOne(connectedChannel.channel.id).get();
		connectable = chat.connectable(this.mixer, false);
		connected = connectable.connect();
		connectable.on(WelcomeEvent.class, new EventHandler<WelcomeEvent>() {
			@Override
			public void onEvent(WelcomeEvent event) {
				MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Connected Server " + event.data.server);
				MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Trying to authenticate to Mixer for channel " + getCoreData().getChannelID());
				authWithMixer();
			}
		});
		if (!getCoreData().getLiveEvents().isEmpty()) {
			MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Starting Setting up of Constellation Events: HelloEvent, LiveEvent, ConstellationDisconnectEvent");
			constellation = new MixerConstellation();
			constellationConnectable = constellation.connectable(mixer, false);
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
					reconnectData.setLastDisconnectTimeConstel(System.currentTimeMillis());
					MixerEventHooks.triggerOnDisconnectEvent(DisconnectType.CONSTELLATION, getCoreData().getChannelName(), getCoreData().getChannelID(), event.data);
				}
			});
			MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Finished Setting up of Constellation Events: HelloEvent, LiveEvent, ConstellationDisconnectEvent");
		}
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "The channel id for the channel you're joining is " + connectedChannel.channel.id);
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Starting Setting up of Chat Events: IncomingMessageEvent, UserJoinEvent, UserLeaveEvent, ChatDisconnectEvent");
		connectable.on(IncomingMessageEvent.class, new EventHandler<IncomingMessageEvent>() {
			@Override
			public void onEvent(IncomingMessageEvent event) {
				if (getCoreData().getMessageIDCaches().size() >= 100) {
					getCoreData().getMessageIDCaches().remove(0);
					MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Removed oldest message from the message cache due to limit of 100 messages in the cache has been reached");
				}
				getCoreData().addMessageIDCaches(event);
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
				if (!getCoreData().getViewers().contains(event.data.username.toLowerCase()))
					getCoreData().addViewer(event.data.username.toLowerCase());
				MixerEventHooks.triggerOnJoinEvent(connectedChannel.username, connectedChannel.channel.id, event.data.username, Integer.parseInt(event.data.id));
			}
		});
		connectable.on(UserLeaveEvent.class, new EventHandler<UserLeaveEvent>() {
			@Override
			public void onEvent(UserLeaveEvent event) {
				if (getCoreData().getViewers().contains(event.data.username.toLowerCase()))
					getCoreData().removeViewer(event.data.username.toLowerCase());
				MixerEventHooks.triggerOnPartEvent(connectedChannel.username, connectedChannel.channel.id, event.data.username, Integer.parseInt(event.data.id));
			}
		});
		connectable.on(ChatDisconnectEvent.class, new EventHandler<ChatDisconnectEvent>() {
			@Override
			public void onEvent(ChatDisconnectEvent event) {
				reconnectData.setLastDisconnectTimeChat(System.currentTimeMillis());
				MixerEventHooks.triggerOnDisconnectEvent(DisconnectType.CHAT, getCoreData().getChannelName(), getCoreData().getChannelID(), event.data);
			}
		});
		connectable.on(UserUpdateEvent.class, new EventHandler<UserUpdateEvent>() {
			@Override
			public void onEvent(UserUpdateEvent event) {
				MixerEventHooks.triggerOnUserUpdateEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), event.data.user, event.data.roles);
			}
		});
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Finished Setting up of Chat Events: IncomingMessageEvent, UserJoinEvent, UserLeaveEvent, ChatDisconnectEvent");

		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Loading Moderators & Viewers");
		try {
			this.loadModerators();
			this.loadViewers();
		} catch (IOException e) {
			MixerEventHooks.triggerOnErrorEvent("Error happened when trying to load moderators/viewers", e);
		}
		if (connected && authenticated)
			MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Connected & Authenticated to Mixer");
		else if (connected && !authenticated)
			MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Connected to Mixer but not Authenticated");
		else if (authenticated && !connected)
			MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Authenticated to Mixer but not connected");
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
		MixerEventHooks.triggerOnDisconnectEvent(DisconnectType.CHAT, getCoreData().getChannelName(), getCoreData().getChannelID(), new ChatDisconnectData(1000, "Requested Disconnect By Bot", false));
		connectable.disconnect();
		getCoreData().getViewers().clear();
		getCoreData().getModerators().clear();
		getCoreData().getMessageIDCaches().clear();
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Disconnected from Mixer Chat!");
		this.connected = false;
		this.authenticated = false;
	}

	/**
	 * Used to add Chat Connection to Reconnect Thread
	 *
	 * @param code
	 */
	public final void addForReconnectChat(int code) {
		this.reconnectData.setLastReconnectCodeChat(code);
		MixerReconnectManager.getMixerReconnectThread().addMixerBotChatBase(this);
	}

	/**
	 * Used to reconnect the bot from a channel's chat
	 */
	public final void reconnectChat() {
		this.reconnectData.setLastReconnectTimeChat(System.currentTimeMillis());
		MixerEventHooks.triggerOnReconnectEvent(ReconnectType.CHAT, getCoreData().getChannelName(), getCoreData().getChannelID());
		try {
			if (this.connectable.reconnectBlocking())
				MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Reconnected to Mixer Chat!");
			else
				MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Failed to be reconnected to Mixer Chat!");
		} catch (InterruptedException e) {
			MixerEventHooks.triggerOnErrorEvent("InterruptedException when Reconnecting Websocket", e);
		}
	}

	/**
	 * Used to disconnect the bot from a channel's constellation
	 */
	public final void disconnectConstellation() {
		MixerEventHooks.triggerOnDisconnectEvent(DisconnectType.CONSTELLATION, getCoreData().getChannelName(), getCoreData().getChannelID(), new ConstellationDisconnectData(1000, "Requested Disconnect By Bot", false));
		this.constellationConnectable.disconnect();
	}

	/**
	 * Used to add Chat Constellation to Reconnect Thread
	 *
	 * @param code
	 */
	public final void addForReconnectConstellation(int code) {
		this.reconnectData.setLastReconnectCodeConstel(code);
		MixerReconnectManager.getMixerReconnectThread().addMixerBotChatConstellation(this);
	}

	/**
	 * Used to reconnect the bot from a channel's constellation
	 */
	public final void reconnectConstellation() {
		this.reconnectData.setLastReconnectTimeConstel(System.currentTimeMillis());
		MixerEventHooks.triggerOnReconnectEvent(ReconnectType.CONSTELLATION, getCoreData().getChannelName(), getCoreData().getChannelID());
		try {
			if (this.constellationConnectable.reconnectBlocking())
				MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Reconnected to Mixer Constellation!");
			else
				MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Failed to be reconnected to Mixer Constellation!");
		} catch (InterruptedException e) {
			MixerEventHooks.triggerOnErrorEvent("InterruptedException when Reconnecting Websocket", e);
		}
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
		for (IncomingMessageEvent message : getCoreData().getMessageIDCaches()) {
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
		for (int i = 0; i < getCoreData().getMessageIDCaches().size(); i++) {
			if (user.equalsIgnoreCase(getCoreData().getMessageIDCaches().get(i).data.userName)) {
				lastMessage = i;
			}
		}
		connectable.send(ChatDeleteMethod.of(getCoreData().getMessageIDCaches().get(lastMessage).data));
		return "Deleted last message for " + user + "!";
	}

	/**
	 * Deletes the last message in connected channels chat for any user
	 *
	 * @return
	 */
	public String deleteLastMessage() {
		connectable.send(ChatDeleteMethod.of(getCoreData().getMessageIDCaches().get(getCoreData().getMessageIDCaches().size() - 1).data));
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
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Banned " + user);
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
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "unban " + user);
	}

	/**
	 * Clean & Reload local storage of moderators
	 *
	 * @throws IOException
	 */
	public void reloadModerators() throws IOException {
		getCoreData().getModerators().clear();
		this.loadModerators();
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
					if (!getCoreData().getModerators().contains(username.toLowerCase()))
						getCoreData().addModerator(username.toLowerCase());
				}
			} else
				done = true;
		}
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Found " + getCoreData().getModerators().size() + " moderators!");
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
				if (!getCoreData().getViewers().contains(username.toLowerCase()))
					getCoreData().addViewer(username.toLowerCase());
			} else
				done = true;
		}
		MixerEventHooks.triggerOnInfoEvent(getCoreData().getChannelName(), getCoreData().getChannelID(), "Found " + getCoreData().getViewers().size() + " viewers!");
	}

	public boolean isUserMod(String user) {
		if (connectedChannel == null) {
			return false;
		}
		return (getCoreData().getModerators() != null) && getCoreData().getModerators().contains(user.toLowerCase());
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

	public MixerUser getConnectedChannel() {
		return connectedChannel;
	}

	public int getNumOfFollowers() {
		if (connected == false)
			return -1;
		updateConnectedChannel();
		return connectedChannel.channel.numFollowers;
	}

	public String getAudience() {
		if (connected == false)
			return null;
		updateConnectedChannel();
		return connectedChannel.channel.audience.toString();
	}

	public int getNumOfTotalViewers() {
		if (connected == false)
			return -1;
		updateConnectedChannel();
		return connectedChannel.channel.viewersTotal;
	}

	public boolean isStreaming() {
		if (connected == false)
			return false;
		updateConnectedChannel();
		return connectedChannel.channel.online;
	}

	public boolean isPartnered() {
		if (connected == false)
			return false;
		updateConnectedChannel();
		return connectedChannel.channel.partnered;
	}

	public Date getUpdatedAt() {
		if (connected == false)
			return null;
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

	public ReconnectData getReconnectData() {
		return reconnectData;
	}

	public CoreData getCoreData() {
		return coreData;
	}

	public abstract void onMessage(String sender, int senderID, List<Role> userRoles, String message, List<String> emotes, List<String> links);

	public abstract void onJoin(String sender, int senderID);

	public abstract void onPart(String sender, int senderID);

	public abstract void onLiveEvent(LiveEvent event);

	public abstract void onUserUpdate(int userID, List<Role> userRoles);
}
