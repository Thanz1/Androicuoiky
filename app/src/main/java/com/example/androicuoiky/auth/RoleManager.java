package com.example.androicuoiky.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RoleManager {

    public static final String STUDENT = "student";
    public static final String STAFF = "staff";
    public static final String ADMIN = "admin";

    public interface RoleCallback {
        void onSuccess(String role);
        void onError(String message);
    }

    public static void checkRole(RoleCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance()
                .getCurrentUser();

        if (user == null) {
            callback.onError("Bạn chưa đăng nhập");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        callback.onError(
                                "Không tìm thấy hồ sơ tài khoản");
                        return;
                    }

                    String uid = doc.getString("uid");
                    if (uid != null && !uid.equals(user.getUid())) {
                        callback.onError("Dữ liệu tài khoản không hợp lệ");
                        return;
                    }

                    String status =
                            doc.getString("accountStatus");

                    // Tài khoản cũ cần được chuyển đổi sang active.
                    // Khi chưa có trường, tạm cho hoạt động.
                    if (status == null) {
                        status = "active";
                    }

                    if (!"active".equals(status)) {
                        callback.onError("Tài khoản đã bị khóa");
                        return;
                    }

                    String role = doc.getString("role");

                    if (STUDENT.equals(role)
                            || STAFF.equals(role)
                            || ADMIN.equals(role)) {

                        callback.onSuccess(role);

                    } else {
                        callback.onError(
                                "Vai trò tài khoản không hợp lệ");
                    }
                })
                .addOnFailureListener(e ->
                        callback.onError(
                                "Không kiểm tra được quyền: "
                                        + e.getMessage())
                );
    }
}
