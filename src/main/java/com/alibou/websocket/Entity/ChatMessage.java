package com.alibou.websocket.Entity;


import com.alibou.websocket.chat.MessageType;

public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;
    private String recipient;  // <-- Add recipient for private messages

    public ChatMessage() {
    }

    public ChatMessage(MessageType type, String content, String sender, String recipient) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.recipient = recipient;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}

