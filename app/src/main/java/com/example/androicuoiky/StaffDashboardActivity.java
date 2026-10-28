package com.example.androicuoiky;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class StaffDashboardActivity extends AppCompatActivity {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db =
            FirebaseFirestore.getInstance();

    private boolean authorized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Chưa hiện chức năng cho đến khi xác minh vai trò
        checkStaffAccess();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authorized) checkStaffAccess();
    }

    private void checkStaffAccess() {
        if (auth.getCurrentUser() == null) {
            returnToLogin();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) return;

                    String role = doc.getString("role");
                    String status = doc.getString("accountStatus");

                    if (status == null) status = "active";

                    if (!"active".equals(status)
                            || (!"staff".equals(role)
                            && !"admin".equals(role))) {
                        authorized = false;
                        returnToLogin();
                        return;
                    }

                    authorized = true;
                    setContentView(R.layout.activity_staff_dashboard);

                    String name = doc.getString("fullName");
                    android.widget.TextView tvName =
                            findViewById(R.id.tvStaffName);

                    tvName.setText("Xin chào, "
                            + (name != null ? name : "cán bộ phòng ban"));

                    setupButtons();
                })
                .addOnFailureListener(e -> returnToLogin());
    }

    private void setupButtons() {
        findViewById(R.id.btnStaffReceive).setOnClickListener(v ->
                showComingSoon("Tiếp nhận đồ vật"));

        findViewById(R.id.btnStaffVerify).setOnClickListener(v ->
                showComingSoon("Xác minh chủ sở hữu"));

        findViewById(R.id.btnStaffHandover).setOnClickListener(v ->
                showComingSoon("Bàn giao đồ vật"));

        findViewById(R.id.btnStaffScores).setOnClickListener(v ->
                showComingSoon("Hồ sơ điểm thưởng"));

        findViewById(R.id.btnStaffFeed).setOnClickListener(v ->
                startActivity(new Intent(
                        this, MainActivity.class)));

        findViewById(R.id.btnStaffLogout).setOnClickListener(v ->
                returnToLogin());
    }

    private void showComingSoon(String feature) {
        if (!authorized) return;
        Toast.makeText(this,
                feature + " – chưa triển khai nghiệp vụ",
                Toast.LENGTH_SHORT).show();
    }

    private void returnToLogin() {
        authorized = false;
        auth.signOut();

        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
