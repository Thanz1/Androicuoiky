package com.example.androicuoiky.models;

import com.google.firebase.Timestamp;

public class Comment {
    private String commentId;
    private String userId;
    private String authorName;
    private String content;
    private String imageUrl; // Hỗ trợ gửi ảnh kèm bình luận
    private Timestamp createdAt;

    public Comment() {
    }

    public Comment(String commentId, String userId, String authorName, String content, String imageUrl, Timestamp createdAt) {
        this.commentId = commentId;
        this.userId = userId;
        this.authorName = authorName;
        this.content = content;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}