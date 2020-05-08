# Mixer-Bot-Framework
A Mixer Bot Framework made in Java using the Mixer API. Made for making the creation of Bot's for Mixer quicker

#### Current Version: 1.1.8
### With Maven
In your `pom.xml` add:
```xml
<repositories>
  <repository>
    <id>maven.mjrlegends.com</id>
    <url>https://maven.mjrlegends.com/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.mjr.mjrmixer</groupId>
    <artifactId>Mixer-Bot-Framework</artifactId>
    <version>@VERSION@</version>
  </dependency>
</dependencies>
```
### With Gradle
In your `build.gradle` add: 
```groovy
repositories {
  	maven {
	    name 'MJRLegends'
	    url = "https://maven.mjrlegends.com/"
    }
}

dependencies {
  compile "com.mjr.mjrmixer:Mixer-Bot-Framework:@VERSION@"
}
```

<a rel="license" href="http://creativecommons.org/licenses/by-nc-nd/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc-nd/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-nc-nd/4.0/">Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International License</a>. **For more information on the license see** https://tldrlegal.com/license/creative-commons-attribution-noncommercial-noderivs-(cc-nc-nd)#summary

### How to use/Example of usage
In your main class
```java
	Example example;
	public static void main(final String[] args) {
		example = new Example("CLIENT_ID", "AUTH_CODE", "BOTNAME");
		example.joinChannel(CHANNELID, USERID);
	}

```
Bot Base class
```java
public class Example extends MixerBotBase{
	public Example(String clientId, String authcode, String botName) {
		super(clientId, authcode, botName);
	}	

	public void joinChannel(int channelID, int userID) {
		/*
		 * Example 1 using Constellation events, events can be access in onLiveEvent methods
		 */
		try {
			ArrayList<String> events = new ArrayList<String>();
			events.add("channel:" + channelID + ":followed");
			this.joinMixerChannel(userID, events);
		} catch (InterruptedException | ExecutionException | IOException e) {
			e.printStackTrace();
		}
		
		/*
		 * Example 2 No Constellation events
		 */
		try {
			this.joinMixerChannel(userID);
		} catch (InterruptedException | ExecutionException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(String sender, int senderID, List<Role> userRoles, String message) {
	
	}
	
	@Override
	protected void onJoin(String sender) {
		
	}

	@Override
	protected void onPart(String sender) {
		
	}

	@Override
	protected void onLiveEvent(LiveEvent event) {
		
	}
}
```
Event Handler Class
```java
	public static void initEvents() {
		MixerManager.registerEventHandler(new MessageEvent() {
			@Override
			public void onEvent(MessageEvent event) {
				try {
					MainClass.example.onMessage(event.sender, event.senderID, event.senderRoles, event.message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		MixerManager.registerEventHandler(new JoinEvent() {
			@Override
			public void onEvent(JoinEvent event) {
				try {
					MainClass.example.onJoin(event.sender, event.senderID);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		MixerManager.registerEventHandler(new PartEvent() {
			@Override
			public void onEvent(PartEvent event) {
				try {
					MainClass.example.onPart(event.sender, event.senderID);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		MixerManager.registerEventHandler(new DisconnectEvent() {
			@Override
			public void onEvent(DisconnectEvent event) {
				try {
					System.out.println("[Mixer] An client connection has triggered a onDisconnect event. Channel: " + event.channel + " Channel ID: " + event.channelID + " Type: " + event.type.name() 
					+ (event.chatData != null ? (" Disconnect Code: " + event.chatData.code + " Disconnect Reason: " + event.chatData.reason) : " Disconnect Code: " + event.constellationData.code + " Disconnect Reason: " + event.constellationData.reason));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		MixerManager.registerEventHandler(new ReconnectEvent() {
			@Override
			public void onEvent(ReconnectEvent event) {
				try {
					System.out.println("[Mixer] An client connection has triggered a onReconnect event. Trying to reconnect! Channel: " + event.channel + " Channel ID: " + event.channelID);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		MixerManager.registerEventHandler(new InfoEvent() {
			@Override
			public void onEvent(InfoEvent event) {
				System.out.println(event.message);
			}
		});
		MixerManager.registerEventHandler(new ErrorEvent() {
			@Override
			public void onEvent(ErrorEvent event) {
				if (event.error != null && event.errorMessage != null)
					logErrorMessage(event.errorMessage, event.error);
				else if (event.errorMessage != null)
					logErrorMessage(event.errorMessage);
				else if (event.error != null)
					logErrorMessage(event.error);
			}
		});
	}
	
	public static void logErrorMessage(String error, final Throwable throwable) {
		String stackTrace = MJRBotUtilities.getStackTraceString(throwable);
		logErrorMessage(error + " - " + stackTrace);
	}

	public static void logErrorMessage(final Throwable throwable) {
		String stackTrace = MJRBotUtilities.getStackTraceString(throwable);
		logErrorMessage(stackTrace);
	}

	public static void logErrorMessage(String stackTrace) {
		System.out.println(stackTrace);
	}
```
