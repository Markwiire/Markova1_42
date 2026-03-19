package com.example.home;

public class Message {
    private String id;
    private String chatId;
    private String senderId;
    private String senderName;
    private String message;
    private String createdAt;
    private boolean isMyMessage;

    public Message(String id, String chatId, String senderId,
                   String senderName, String message, String createdAt,
                   boolean isMyMessage) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.createdAt = createdAt;
        this.isMyMessage = isMyMessage;
    }


    public String getId() { return id; }
    public String getChatId() { return chatId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
    public boolean isMyMessage() { return isMyMessage; }
}