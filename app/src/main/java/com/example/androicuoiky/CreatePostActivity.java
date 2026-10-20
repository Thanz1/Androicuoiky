package com.example.androicuoiky;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.androicuoiky.models.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreatePostActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        EditText etTitle = findViewById(R.id.etTitle);
        EditText etLocation = findViewById(R.id.etLocation);
        EditText etPhone = findViewById(R.id.etPhone);
        EditText etDescription = findViewById(R.id.etDescription);
        RadioButton rbLost = findViewById(R.id.rbLost);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String postType = rbLost.isChecked() ? "LOST" : "FOUND";

            if (title.isEmpty() || location.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lấy tên người dùng từ bảng users trước khi đăng
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String authorName = documentSnapshot.getString("fullName");
                        if (authorName == null || authorName.isEmpty()) {
                            authorName = "Sinh viên ẩn danh";
                        }

                        DocumentReference newDocRef = db.collection("posts").document();
                        Post newPost = new Post(
                                newDocRef.getId(),
                                currentUser.getUid(),
                                authorName,
                                title,
                                location,
                                phone,
                                desc,
                                postType,
                                System.currentTimeMillis()
                        );

                        newDocRef.set(newPost)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đăng tin thành công!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lấy thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}