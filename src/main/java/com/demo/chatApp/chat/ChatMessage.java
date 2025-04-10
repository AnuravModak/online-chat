package com.demo.chatApp.chat;

import jakarta.persistence.*;

import java.util.Date;
import java.util.UUID;

@Entity
//@Table(name = "messages")
public class ChatMessage {

    @Id
    private String id;

    private String chatId;

    @Column(length = 2048)
    private String senderId;
    @Column(length = 2048)
    private String recipientId;
    @Column(length = 2048)
    private String content;
    private Date timestamp;

    private long keyId;

    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
    }

    public ChatMessage( String chatId, String senderId, String recipientId, String content, Date timestamp, long keyId) {
        this.id = UUID.randomUUID().toString();
        this.chatId = chatId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.timestamp = timestamp;
        this.keyId=keyId;
    }

    public ChatMessage(String id, String chatId, String senderId, String recipientId, String content, Date timestamp, long keyId) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.timestamp = timestamp;
        this.keyId=keyId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public long getKeyId() {
        return keyId;
    }

    public void setKeyId(long keyId) {
        this.keyId = keyId;
    }
}