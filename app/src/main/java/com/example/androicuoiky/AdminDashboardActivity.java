package com.example.androicuoiky;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private EditText etSearchCode;
    private TextView tvResultInfo;
    private Button btnVerifyClaim;
    private LinearLayout layoutDetails;
    private String currentFoundPostId = null;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goLogin();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!"admin".equals(doc.getString("role"))
                            || "disabled".equals(doc.getString("accountStatus"))) {
                        goLogin();
                        return;
                    }
                    initAdminUI();
                })
                .addOnFailureListener(e -> goLogin());
    }

    private void initAdminUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 48, 32, 32);

        TextView title = new TextView(this);
        title.setText("SchoolFind - Tra cứu & Xác nhận đồ");
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Nhập mã xác nhận của đồ nhặt được để kiểm tra trạng thái trao trả.");
        desc.setPadding(0, 8, 0, 24);
        layout.addView(desc);

        etSearchCode = new EditText(this);
        etSearchCode.setHint("Nhập mã xác nhận (VD: A3F8K2)");
        layout.addView(etSearchCode);

        Button btnSearch = new Button(this);
        btnSearch.setText("Tra cứu mã");
        btnSearch.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
        btnSearch.setTextColor(getResources().getColor(android.R.color.white));
        layout.addView(btnSearch);

        layoutDetails = new LinearLayout(this);
        layoutDetails.setOrientation(LinearLayout.VERTICAL);
        layoutDetails.setPadding(0, 24, 0, 0);
        layoutDetails.setVisibility(View.GONE);

        tvResultInfo = new TextView(this);
        tvResultInfo.setTextSize(16);
        layoutDetails.addView(tvResultInfo);

        btnVerifyClaim = new Button(this);
        btnVerifyClaim.setText("Xác nhận đã trao món đồ này");
        btnVerifyClaim.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        btnVerifyClaim.setTextColor(getResources().getColor(android.R.color.white));
        layoutDetails.addView(btnVerifyClaim);

        layout.addView(layoutDetails);

        Button logout = new Button(this);
        logout.setText("Đăng xuất");
        logout.setOnClickListener(v -> goLogin());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 64, 0, 0);
        logout.setLayoutParams(params);
        layout.addView(logout);

        scrollView.addView(layout);
        setContentView(scrollView);

        btnSearch.setOnClickListener(v -> searchByVerificationCode());
        btnVerifyClaim.setOnClickListener(v -> confirmItemClaimed());
    }

    private void searchByVerificationCode() {
        String code = etSearchCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(code)) {
            etSearchCode.setError("Vui lòng nhập mã xác nhận");
            return;
        }

        FirebaseFirestore.getInstance().collection("posts")
                .whereEqualTo("verificationCode", code)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        layoutDetails.setVisibility(View.GONE);
                        Toast.makeText(this, "Không tìm thấy món đồ nào với mã này!", Toast.LENGTH_SHORT).show();
                    } else {
                        com.google.firebase.firestore.DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        currentFoundPostId = document.getId();

                        String postTitle = document.getString("title");
                        String author = document.getString("authorName");
                        String location = document.getString("location");
                        String claimStatus = document.getString("claimStatus");
                        String statusText = "PENDING".equals(claimStatus) ? "⏳ Chờ trao trả" : "✅ Đã trao thành công";

                        String info = "📦 Tên đồ vật: " + postTitle + "\n" +
                                "📍 Vị trí nhặt: " + location + "\n" +
                                "👤 Người nhặt: " + author + "\n" +
                                "📌 Trạng thái: " + statusText;

                        tvResultInfo.setText(info);
                        layoutDetails.setVisibility(View.VISIBLE);

                        if ("COMPLETED".equals(claimStatus)) {
                            btnVerifyClaim.setVisibility(View.GONE);
                        } else {
                            btnVerifyClaim.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tra cứu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmItemClaimed() {
        if (currentFoundPostId == null) return;

        FirebaseFirestore.getInstance().collection("posts")
                .document(currentFoundPostId)
                .update("claimStatus", "COMPLETED", "status", "RESOLVED")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show();
                    btnVerifyClaim.setVisibility(View.GONE);
                    searchByVerificationCode();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void goLogin() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}