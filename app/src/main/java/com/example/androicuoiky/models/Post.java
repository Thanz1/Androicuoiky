package com.example.androicuoiky.models;

public class Post {
    public String postId;
    public String userId;
    public String authorName;
    public String title;
    public String location;
    public String authorPhone;
    public String description;
    public String postType;
    public long createdAt;

    public Post() {
    }

    public Post(String postId, String userId, String authorName, String title, String location, String authorPhone, String description, String postType, long createdAt) {
        this.postId = postId;
        this.userId = userId;
        this.authorName = authorName;
        this.title = title;
        this.location = location;
        this.authorPhone = authorPhone;
        this.description = description;
        this.postType = postType;
        this.createdAt = createdAt;
    }
}