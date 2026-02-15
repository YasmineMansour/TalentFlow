package Entites;

import java.sql.Timestamp;

public class Message {
    private int id;
    private int senderId;
    private int receiverId;
    private String content;
    private Timestamp sentAt;

    public Message() {}

    public Message(int id, int senderId, int receiverId, String content, Timestamp sentAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.sentAt = sentAt;
    }

    // Getters and Setters...
    public int getId() { return id; }
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public void setId(int id) { this.id = id; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }
    public Timestamp getSentAt() { return sentAt; }
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }
}