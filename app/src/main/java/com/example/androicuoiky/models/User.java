package com.example.androicuoiky.models;

public class User {
    public String userId;
    public String fullName;
    public String email;
    public String studentId;

    public User() {
    }

    public User(String userId, String fullName, String email, String studentId) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.studentId = studentId;
    }
}