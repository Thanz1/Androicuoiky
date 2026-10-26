package com.example.androicuoiky.models;

import com.google.firebase.Timestamp;

import java.util.Date;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    private Object timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String messageId, String senderId, String receiverId, String message, Object timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getTimestamp() { return timestamp; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    public Date getTimestampDate() {
        if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toDate();
        } else if (timestamp instanceof Long) {
            return new Date((Long) timestamp);
        } else if (timestamp instanceof Date) {
            return (Date) timestamp;
        }
        return new Date();
    }
}