package com.example.androicuoiky;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FriendManager {

    public interface FriendStatusCallback {
        // Trạng thái: "NONE" (Chưa là gì), "SENT" (Đã gửi chờ duyệt), "RECEIVED" (Người ta gửi cho mình), "FRIEND" (Đã là bạn)
        void onStatusResult(String status, String requestId);
    }

    public static void checkStatus(String targetUserId, FriendStatusCallback callback) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null || targetUserId == null || currentUid.equals(targetUserId)) {
            callback.onStatusResult("SELF", null);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Kiểm tra xem đã là bạn bè chính thức chưa
        db.collection("users").document(currentUid).collection("friends").document(targetUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onStatusResult("FRIEND", null);
                    } else {
                        // 2. Kiểm tra xem mình có đang gửi request không
                        String sentDocId = currentUid + "_" + targetUserId;
                        db.collection("friend_requests").document(sentDocId).get()
                                .addOnSuccessListener(sentDoc -> {
                                    if (sentDoc.exists() && "PENDING".equals(sentDoc.getString("status"))) {
                                        callback.onStatusResult("SENT", sentDocId);
                                    } else {
                                        // 3. Kiểm tra xem đối phương có gửi request cho mình không
                                        String recDocId = targetUserId + "_" + currentUid;
                                        db.collection("friend_requests").document(recDocId).get()
                                                .addOnSuccessListener(recDoc -> {
                                                    if (recDoc.exists() && "PENDING".equals(recDoc.getString("status"))) {
                                                        callback.onStatusResult("RECEIVED", recDocId);
                                                    } else {
                                                        callback.onStatusResult("NONE", null);
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    // Gửi lời mời kết bạn (Chuyển sang trạng thái chờ)
    public static void sendRequest(String targetUserId, Runnable onSuccess) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        String docId = currentUid + "_" + targetUserId;
        Map<String, Object> req = new HashMap<>();
        req.put("senderId", currentUid);
        req.put("receiverId", targetUserId);
        req.put("status", "PENDING");
        req.put("timestamp", Timestamp.now());

        FirebaseFirestore.getInstance().collection("friend_requests").document(docId)
                .set(req)
                .addOnSuccessListener(v -> onSuccess.run());
    }

    // Chấp nhận lời mời kết bạn
    public static void acceptRequest(String senderId, Runnable onSuccess) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", Timestamp.now());

        // Ghi vào subcollection friends của cả 2 người
        db.collection("users").document(currentUid).collection("friends").document(senderId).set(data);
        db.collection("users").document(senderId).collection("friends").document(currentUid).set(data);

        // Xóa request
        db.collection("friend_requests").document(senderId + "_" + currentUid).delete()
                .addOnSuccessListener(v -> onSuccess.run());
    }

    // Hủy kết bạn hoặc Hủy lời mời
    public static void cancelOrRemove(String targetUserId, Runnable onSuccess) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(currentUid).collection("friends").document(targetUserId).delete();
        db.collection("users").document(targetUserId).collection("friends").document(currentUid).delete();
        db.collection("friend_requests").document(currentUid + "_" + targetUserId).delete();
        db.collection("friend_requests").document(targetUserId + "_" + currentUid).delete()
                .addOnSuccessListener(v -> onSuccess.run());
    }
}