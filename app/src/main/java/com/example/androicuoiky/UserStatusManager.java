package com.example.androicuoiky;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserStatusManager {

    public static void setStatus(boolean isOnline) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("isOnline", isOnline);
        statusMap.put("lastSeen", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.getUid())
                .update(statusMap);
    }
}