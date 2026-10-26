package com.example.androicuoiky.models;

public class ClassScore {
    private String classId;   // Mã lớp (làm Document ID trên Firestore)
    private String className; // Tên hiển thị của lớp
    private int totalPoints;  // Tổng điểm thi đua tích lũy từ các vụ trao trả đồ
    private int returnedItemCount; // Số lần học sinh lớp này nhặt và trả đồ thành công

    public ClassScore() {
    }

    public ClassScore(String classId, String className, int totalPoints, int returnedItemCount) {
        this.classId = classId;
        this.className = className;
        this.totalPoints = totalPoints;
        this.returnedItemCount = returnedItemCount;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getReturnedItemCount() {
        return returnedItemCount;
    }

    public void setReturnedItemCount(int returnedItemCount) {
        this.returnedItemCount = returnedItemCount;
    }
}