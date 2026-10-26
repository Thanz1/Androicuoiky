package com.example.androicuoiky.models;

import com.google.firebase.Timestamp;
import java.util.Date;

public class Comment {
    private String commentId;
    private String userId;
    private String authorName;
    private String content;
    private String imageUrl;
    private Object createdAt; // Đặt kiểu Object để nhận cả Timestamp lẫn Long

    public Comment() {
    }

    public Comment(String commentId, String userId, String authorName, String content, String imageUrl, Object createdAt) {
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

    public Object getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }

    // Hàm an toàn để chuyển Object về Date mà không bị lỗi compile hoặc runtime crash
    public Date getCreatedAtDate() {
        if (createdAt instanceof Timestamp) {
            return ((Timestamp) createdAt).toDate();
        } else if (createdAt instanceof Long) {
            return new Date((Long) createdAt);
        } else if (createdAt instanceof Date) {
            return (Date) createdAt;
        }
        return null;
    }
}