package com.example.androicuoiky.models;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Post {
    private String postId;
    private String userId;        // ID của người đăng thực tế (Admin luôn tra cứu được)
    private String authorName;    // Tên thật của người đăng
    private String classId;       // Lớp của người đăng để cộng điểm khi trả đồ
    private String title;
    private String description;
    private String location;      // Vị trí (VD: Thư viện, Khu A, Sân bóng)
    private String category;      // "LOST" (mất đồ) hoặc "FOUND" (nhặt được đồ)
    private String imageUrl;
    private String status;        // "PENDING" (đang tìm/chờ trả), "RESOLVED" (đã trả/đã nhận lại)
    private boolean isAnonymous;  // True: Ẩn danh với người dùng thường (chỉ Admin thấy UID/Tên thật)
    private boolean isScoreApproved; // True: Đã được Admin duyệt cộng điểm thi đua cho lớp
    private Object createdAt;

    // Bắt buộc constructor rỗng cho Firestore
    public Post() {
    }

    public Post(String postId, String userId, String authorName, String classId, String title,
                String description, String location, String category, String imageUrl,
                String status, boolean isAnonymous, boolean isScoreApproved, Object createdAt) {
        this.postId = postId;
        this.userId = userId;
        this.authorName = authorName;
        this.classId = classId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.category = category;
        this.imageUrl = imageUrl;
        this.status = status;
        this.isAnonymous = isAnonymous;
        this.isScoreApproved = isScoreApproved;
        this.createdAt = createdAt;
    }

    // Getters và Setters
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public boolean isScoreApproved() {
        return isScoreApproved;
    }

    public void setScoreApproved(boolean scoreApproved) {
        isScoreApproved = scoreApproved;
    }

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCreatedAtDate() {
        if (createdAt instanceof Timestamp) {
            return ((Timestamp) createdAt).toDate();
        } else if (createdAt instanceof Long) {
            return new Date((Long) createdAt);
        } else if (createdAt instanceof Date) {
            return (Date) createdAt;
        }
        return new Date();
    }
    // Thêm trường này vào trong class Post:
    private Map<String, String> reactions = new HashMap<>();

    // Thêm Getter và Setter:
    public Map<String, String> getReactions() {
        if (reactions == null) reactions = new HashMap<>();
        return reactions;
    }

    public void setReactions(Map<String, String> reactions) {
        this.reactions = reactions;
    }
}