package com.example.androicuoiky.models;

public class User {
    private String uid;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String classId; // Lớp (ví dụ: D23CNTT, 12A1) để tính điểm thi đua
    private String role;    // "student" hoặc "admin"
    private String avatarUrl;

    // Bắt buộc phải có constructor rỗng cho Firestore
    public User() {
    }

    public User(String uid, String email, String fullName, String phoneNumber, String classId, String role, String avatarUrl) {
        this.uid = uid;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.classId = classId;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }

    // Getters và Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}