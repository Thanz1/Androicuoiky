package com.example.androicuoiky;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.androicuoiky.models.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {

    private ShapeableImageView ivProfileAvatar;
    private ImageButton btnChangeAvatar;
    private TextInputEditText etFullName, etClassId, etPhone, etEmail;
    private Button btnSaveProfile;
    private ProgressBar progressBarProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri newAvatarUri = null;
    private String currentAvatarUrl = "";

    private final ActivityResultLauncher<String> avatarPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    newAvatarUri = uri;
                    ivProfileAvatar.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        etFullName = findViewById(R.id.etFullName);
        etClassId = findViewById(R.id.etClassId);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBarProfile = findViewById(R.id.progressBarProfile);

        btnChangeAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        loadCurrentUserData();
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        etEmail.setText(currentUser.getEmail());

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            etFullName.setText(user.getFullName());
                            etClassId.setText(user.getClassId());
                            etPhone.setText(user.getPhoneNumber());
                            currentAvatarUrl = user.getAvatarUrl();
                            if (!TextUtils.isEmpty(currentAvatarUrl)) {
                                Glide.with(ProfileActivity.this).load(currentAvatarUrl).into(ivProfileAvatar);
                            }
                        }
                    }
                });
    }

    private void saveProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String name = etFullName.getText().toString().trim();
        String classId = etClassId.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Vui lòng điền họ tên");
            return;
        }

        setLoading(true);

        // Nếu người dùng chọn ảnh avatar mới -> Đẩy lên Storage trước
        if (newAvatarUri != null) {
            String avatarPath = "avatars/" + currentUser.getUid() + ".jpg";
            StorageReference ref = storage.getReference().child(avatarPath);
            ref.putFile(newAvatarUri)
                    .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateFirestore(currentUser.getUid(), name, classId, phone, uri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            updateFirestore(currentUser.getUid(), name, classId, phone, currentAvatarUrl);
        }
    }

    private void updateFirestore(String uid, String name, String classId, String phone, String avatarUrl) {
        db.collection("users").document(uid)
                .update("fullName", name,
                        "classId", classId,
                        "phoneNumber", phone,
                        "avatarUrl", avatarUrl)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBarProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!isLoading);
        btnChangeAvatar.setEnabled(!isLoading);
    }
}