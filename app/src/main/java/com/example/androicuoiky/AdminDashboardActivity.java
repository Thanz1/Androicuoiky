package com.example.androicuoiky;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            goLogin();
            return;
        }

        String uid = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!"admin".equals(doc.getString("role"))
                            || "disabled".equals(
                                    doc.getString("accountStatus"))) {
                        goLogin();
                        return;
                    }

                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(32, 48, 32, 32);

                    TextView title = new TextView(this);
                    title.setText("SchoolFind Admin");
                    title.setTextSize(26);
                    layout.addView(title);

                    TextView desc = new TextView(this);
                    desc.setText("Quản lý học sinh, phòng ban, "
                            + "lớp và điểm thưởng.\n"
                            + "Các chức năng quản trị sẽ "
                            + "được triển khai tiếp.");
                    layout.addView(desc);

                    Button logout = new Button(this);
                    logout.setText("Đăng xuất");
                    logout.setOnClickListener(v -> goLogin());
                    layout.addView(logout);

                    setContentView(layout);
                })
                .addOnFailureListener(e -> goLogin());
    }

    private void goLogin() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
