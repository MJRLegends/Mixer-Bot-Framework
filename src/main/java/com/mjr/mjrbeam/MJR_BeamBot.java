package com.mjr.mjrbeam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pro.beam.api.BeamAPI;
import pro.beam.api.resource.BeamUser;
import pro.beam.api.resource.chat.BeamChat;
import pro.beam.api.resource.chat.BeamChatConnectable;
import pro.beam.api.resource.chat.events.EventHandler;
import pro.beam.api.resource.chat.events.IncomingMessageEvent;
import pro.beam.api.resource.chat.events.UserJoinEvent;
import pro.beam.api.resource.chat.events.UserLeaveEvent;
import pro.beam.api.resource.chat.events.data.IncomingMessageData.MessagePart;
import pro.beam.api.resource.chat.events.data.IncomingMessageData.MessagePart.Type;
import pro.beam.api.resource.chat.methods.AuthenticateMessage;
import pro.beam.api.resource.chat.methods.ChatSendMethod;
import pro.beam.api.resource.chat.replies.AuthenticationReply;
import pro.beam.api.resource.chat.replies.ReplyHandler;
import pro.beam.api.response.users.UserSearchResponse;
import pro.beam.api.services.impl.ChatService;
import pro.beam.api.services.impl.UsersService;

public class MJR_BeamBot {
	private BeamUser user;
	private BeamUser connectedChannel;
	private BeamChat chat;
	private BeamChatConnectable connectable;
	private BeamAPI beam = new BeamAPI();

	private String username = "";
	private String password = "";

	private List<String> moderators = new ArrayList<String>();
	private List<String> viewers = new ArrayList<String>();
	private List<IncomingMessageEvent> messageIDCache = new ArrayList<IncomingMessageEvent>();

	private boolean connected = false;
	private boolean authenticated = false;
	private boolean debugMessages = false;

	protected final synchronized void connect(String channel, String username,
			String password) throws InterruptedException, ExecutionException {
		this.username = username;
		this.password = password;
		try {
			if (debugMessages)
				System.out.println("Connecting to Beam! Using Username: "
						+ username);
			user = beam.use(UsersService.class).login(username, password).get();
		} catch (ExecutionException e) {
			if (debugMessages)
				System.out
						.println("Failed To login to beam! check your login credentials!");
			return;
		}
		if (debugMessages)
			System.out.println("Connecting to channel: " + channel);
		UserSearchResponse search = beam.use(UsersService.class)
				.search(channel.toLowerCase()).get();
		if (search.size() > 0) {
			connectedChannel = beam.use(UsersService.class)
					.findOne(search.get(0).id).get();
		} else {
			if (debugMessages)
				System.out.println("No channel found!");
			return;
		}
		chat = beam.use(ChatService.class).findOne(connectedChannel.channel.id)
				.get();
		connectable = chat.makeConnectable(beam);
		connected = connectable.connectBlocking();

		if (connected) {
			connectable.send(AuthenticateMessage.from(connectedChannel.channel,
					user, chat.authkey),
					new ReplyHandler<AuthenticationReply>() {
						@Override
						public void onSuccess(AuthenticationReply reply) {
							authenticated = true;
						}
					});
		}

		connectable.on(IncomingMessageEvent.class,
				new EventHandler<IncomingMessageEvent>() {
					@Override
					public void onEvent(IncomingMessageEvent event) {
						messageIDCache.add(event);
						String msg = "";
						for (MessagePart msgp : event.data.message) {
							if (msgp.type.equals(Type.LINK)) {
								return;
							}
							msg += msgp.data;
						}
						onMessage(event.data.user_name, msg);
					}
				});
		connectable.on(UserJoinEvent.class, new EventHandler<UserJoinEvent>() {
			@Override
			public void onEvent(UserJoinEvent event) {
				if (!viewers.contains(event.data.username.toLowerCase()))
					viewers.add(event.data.username.toLowerCase());
				onJoin(event.data.username);
			}
		});
		connectable.on(UserLeaveEvent.class,
				new EventHandler<UserLeaveEvent>() {
					@Override
					public void onEvent(UserLeaveEvent event) {
						if (viewers.contains(event.data.username.toLowerCase()))
							viewers.remove(event.data.username.toLowerCase());
						onPart(event.data.username);
					}
				});
		try {
			this.loadModerators();
			this.loadViewers();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (debugMessages) {
			if (connected && authenticated)
				System.out.println("Connected & Authenticated to Beam");
			else if (connected && !authenticated)
				System.out.println("Connected to Beam");
		}
	}

	protected final synchronized void disconnect() {
		connectable.close();
		viewers.clear();
		moderators.clear();
		messageIDCache.clear();
		if (debugMessages)
			System.out.println("Disconnected from Beam!");
	}

	public void sendMessage(String msg) {
		connectable.send(ChatSendMethod.of(msg));
	}

	protected String deleteUserMessages(String user) {
		int messagesDeleted = 0;
		for (IncomingMessageEvent message : messageIDCache) {
			if (user.equalsIgnoreCase(message.data.user_name)) {
				connectable.delete(message.data);
				messagesDeleted++;
			}
		}
		if (messagesDeleted > 0) {
			return "Deleted all of " + user + " messages!";
		}
		return "";
	}

	protected String deleteLastMessageForUser(String user) {
		int lastMessage = 0;
		for (int i = 0; i < messageIDCache.size(); i++) {
			if (user.equalsIgnoreCase(messageIDCache.get(i).data.user_name)) {
				lastMessage = i;
			}
		}
		connectable.delete(messageIDCache.get(lastMessage).data);
		return "Deleted last message for " + user + "!";
	}

	protected String deleteLastMessage() {
		connectable.delete(messageIDCache.get(messageIDCache.size() - 1).data);
		return "Deleted last message!";
	}

	protected void ban(String user) {
		deleteUserMessages(user);
		String path = BeamAPI.BASE_PATH.resolve(
				"channels/" + connectedChannel.channel.id + "/users/"
						+ user.toLowerCase()).toString();
		HashMap<String, Object> map = new HashMap<>();
		map.put("add", new String[] { "Banned" });
		try {
			Object result = beam.http.patch(path, Object.class, map).get(4,
					TimeUnit.SECONDS);
			if ((result != null) && result.toString().contains("username")) {
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			System.out.println(e.getMessage());
			return;
		}
		if (debugMessages)
			System.out.println("Banned " + user);
	}

	protected void unban(String user) {
		String path = BeamAPI.BASE_PATH.resolve(
				"channels/" + connectedChannel.channel.id + "/users/"
						+ user.toLowerCase()).toString();
		HashMap<String, Object> map = new HashMap<>();
		map.put("add", new String[] { "" });
		try {
			Object result = beam.http.patch(path, Object.class, map).get(4,
					TimeUnit.SECONDS);
			if ((result != null) && result.toString().contains("username")) {
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			System.out.println(e.getMessage());
		}
		if (debugMessages)
			System.out.println("unban " + user);
	}

	private void loadModerators() throws IOException {
		String result = "";
		URL url = new URL("https://beam.pro/api/v1/channels/"
				+ connectedChannel.channel.id + "/users");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
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
				permission = permission.substring(0,
						permission.indexOf("}") - 1);
				username = username.substring(0, username.indexOf(",") - 1);
				result = result.substring(result.indexOf("username") + 15);
				if (permission.contains("Mod")) {
					if (!moderators.contains(username.toLowerCase()))
						moderators.add(username.toLowerCase());
				}
			} else
				done = true;
		}
		if (debugMessages) {
			System.out.println("Found " + moderators.size() + " moderators!");
		}
	}

	private void loadViewers() throws IOException {
		String result = "";
		URL url = new URL("https://beam.pro/api/v1/chats/"
				+ connectedChannel.channel.id + "/users");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
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
				username = username.substring(0, username.indexOf(",") - 1);
				result = result.substring(result.indexOf("userName") + 15);
				if (!viewers.contains(username.toLowerCase()))
					viewers.add(username.toLowerCase());
			} else
				done = true;
		}
		if (debugMessages) {
			System.out.println("Found " + viewers.size() + " viewers!");
		}
	}

	public boolean isUserMod(String user) {
		if (connectedChannel == null) {
			return false;
		}
		return (moderators != null) && moderators.contains(user.toLowerCase());
	}

	protected boolean isConnected() {
		return connected;
	}

	protected boolean isAuthenticated() {
		return authenticated;
	}

	public String getBotName() {
		return username;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public List<String> getModerators() {
		return moderators;
	}

	public List<String> getViewers() {
		return viewers;
	}

	public void setdebug(boolean value) {
		debugMessages = value;
	}

	//,List<Role> user_roles
	protected void onMessage(String sender, String message) {
	}

	protected void onJoin(String sender) {

	}

	protected void onPart(String sender) {

	}
}