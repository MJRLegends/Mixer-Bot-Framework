package com.mjr.mjrmixer.chatMethods;

import com.mixer.api.resource.chat.AbstractChatMethod;
import com.mixer.api.resource.chat.events.data.IncomingMessageData;

import java.util.Arrays;
import java.util.List;

public class ChatDeleteMethod extends AbstractChatMethod {
    public static ChatDeleteMethod of(IncomingMessageData data) {
        return of(data.id);
    }
    
    public static ChatDeleteMethod of(String messageID) {
        ChatDeleteMethod scm = new ChatDeleteMethod();
        scm.arguments = Arrays.asList(messageID);

        return scm;
    }

    public ChatDeleteMethod() {
        super("deleteMessage");
    }

    public List<String> arguments;
}
