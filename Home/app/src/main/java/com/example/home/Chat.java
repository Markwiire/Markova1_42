package com.example.home;

public class Chat {
    private String id;
    private String petId;
    private String petName;
    private String clientId;
    private String clientName;
    private String managerId;
    private String managerName;
    private String lastMessage;
    private String lastMessageTime;

    public Chat(String id, String petId, String petName, String clientId,
                String clientName, String managerId, String managerName,
                String lastMessage, String lastMessageTime) {
        this.id = id;
        this.petId = petId;
        this.petName = petName;
        this.clientId = clientId;
        this.clientName = clientName;
        this.managerId = managerId;
        this.managerName = managerName;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    // Геттеры
    public String getId() { return id; }
    public String getPetId() { return petId; }
    public String getPetName() { return petName; }
    public String getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getManagerId() { return managerId; }
    public String getManagerName() { return managerName; }
    public String getLastMessage() { return lastMessage; }
    public String getLastMessageTime() { return lastMessageTime; }
}