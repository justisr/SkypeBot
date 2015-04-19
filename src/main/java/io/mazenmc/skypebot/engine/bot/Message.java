package io.mazenmc.skypebot.engine.bot;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class Message {
    private final String sender;
    private final String contents;

    public Message(String sender, String contents) {
        this.sender = sender;
        this.contents = contents;
    }

    public String sender() {
        return sender;
    }

    public String contents() {
        return contents;
    }
}
