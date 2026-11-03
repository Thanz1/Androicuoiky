package com.example.androicuoiky;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.androicuoiky.models.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileActivity extends AppCompatActivity {

    private ShapeableImageView ivProfileAvatar;

    private TextInputEditText etFullName;
    private TextInputEditText etStudentId;
    private TextInputEditText etClassId;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;

    private TextView tvProfileName;
    private TextView tvProfileRole;

    private Button btnChangeAvatar;
    private Button btnSaveProfile;
    private Button btnViewFriends;
    private Button btnResetPassword;
    private Button btnLogoutProfile;
    private ImageButton btnProfileBack;

    private ProgressBar progressBarProfile;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri newAvatarUri;
    private String currentAvatarUrl = "";

    private boolean loading = false;
    private boolean hasLoadedProfile = false;

    private final ActivityResultLauncher<String> avatarPicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null && !loading) {
                            newAvatarUri = uri;
                            ivProfileAvatar.setImageURI(uri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        bindViews();
        setupEvents();
        loadCurrentUserData();
    }

    private void bindViews() {
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);

        etFullName = findViewById(R.id.etFullName);
        etStudentId = findViewById(R.id.etStudentId);
        etClassId = findViewById(R.id.etClassId);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);

        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnViewFriends = findViewById(R.id.btnViewFriends);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile);
        btnProfileBack = findViewById(R.id.btnProfileBack);

        progressBarProfile = findViewById(R.id.progressBarProfile);
    }

    private void setupEvents() {
        btnProfileBack.setOnClickListener(v -> finish());

        btnChangeAvatar.setOnClickListener(v ->
                avatarPicker.launch("image/*"));

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Sự kiện mở danh sách bạn bè từ hồ sơ cá nhân
        btnViewFriends.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FriendsActivity.class);
            startActivity(intent);
        });

        btnResetPassword.setOnClickListener(v ->
                confirmResetPassword());

        btnLogoutProfile.setOnClickListener(v -> confirmLogout());
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        setLoading(true);
        etEmail.setText(currentUser.getEmail());

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    setLoading(false);

                    if (isFinishing() || isDestroyed()) return;

                    if (!document.exists()) {
                        showMessage("Không tìm thấy hồ sơ");
                        return;
                    }

                    User user = document.toObject(User.class);
                    if (user == null) {
                        showMessage("Không đọc được thông tin");
                        return;
                    }

                    if ("disabled".equals(
                            document.getString("accountStatus"))) {
                        showMessage("Tài khoản đã bị khóa");
                        goToLogin();
                        return;
                    }

                    hasLoadedProfile = true;

                    String name = safe(user.getFullName());
                    String classId = safe(user.getClassId());
                    String studentId = safe(user.getStudentId());

                    etFullName.setText(name);
                    etPhone.setText(safe(user.getPhoneNumber()));
                    etClassId.setText(classId);
                    etStudentId.setText(studentId);

                    tvProfileName.setText(
                            name.isEmpty() ? "Học sinh" : name);

                    String role = safe(user.getRole());
                    String roleLabel;

                    switch (role) {
                        case "staff":
                            roleLabel = "Cán bộ phòng ban";
                            break;
                        case "admin":
                            roleLabel = "Quản trị viên";
                            break;
                        default:
                            roleLabel = "Học sinh";
                            break;
                    }

                    if (!classId.isEmpty()
                            && "student".equals(role)) {
                        roleLabel += " · Lớp " + classId;
                    }

                    tvProfileRole.setText(roleLabel);

                    currentAvatarUrl = safe(user.getAvatarUrl());

                    if (!currentAvatarUrl.isEmpty()) {
                        Glide.with(this)
                                .load(currentAvatarUrl)
                                .circleCrop()
                                .into(ivProfileAvatar);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showMessage(
                            "Không tải được hồ sơ: " + e.getMessage());
                });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void saveProfile() {
        if (loading || !hasLoadedProfile) return;

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        String fullName = etFullName.getText() == null
                ? "" : etFullName.getText().toString().trim();

        String phone = etPhone.getText() == null
                ? "" : etPhone.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ tên");
            etFullName.requestFocus();
            return;
        }

        if (fullName.length() > 100) {
            etFullName.setError("Họ tên quá dài");
            return;
        }

        if (!phone.isEmpty()
                && !phone.matches("\\+?[0-9]{9,15}")) {
            etPhone.setError(
                    "Số điện thoại không hợp lệ");
            return;
        }

        setLoading(true);

        if (newAvatarUri == null) {
            updateFirestore(
                    currentUser.getUid(),
                    fullName,
                    phone,
                    currentAvatarUrl
            );
            return;
        }

        uploadAvatar(
                currentUser.getUid(),
                fullName,
                phone
        );
    }

    private void uploadAvatar(
            String uid,
            String fullName,
            String phone
    ) {
        Uri imageUri = newAvatarUri;
        if (imageUri == null) {
            setLoading(false);
            return;
        }

        String mime = getContentResolver().getType(imageUri);

        if (mime == null || !mime.startsWith("image/")) {
            setLoading(false);
            showMessage("Chỉ được chọn file ảnh");
            return;
        }

        String path = "avatars/" + uid + "/"
                + UUID.randomUUID();

        StorageReference ref = storage
                .getReference()
                .child(path);

        StorageMetadata metadata =
                new StorageMetadata.Builder()
                        .setContentType(mime)
                        .build();

        ref.putFile(imageUri, metadata)
                .addOnSuccessListener(task ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        updateFirestore(
                                                uid,
                                                fullName,
                                                phone,
                                                uri.toString()
                                        )
                                )
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    showMessage(
                                            "Không lấy được URL ảnh: "
                                                    + e.getMessage());
                                })
                )
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showMessage("Lỗi tải ảnh: " + e.getMessage());
                });
    }

    private void updateFirestore(
            String uid,
            String fullName,
            String phone,
            String avatarUrl
    ) {
        Map<String, Object> changes = new HashMap<>();

        changes.put("fullName", fullName);
        changes.put("phoneNumber", phone);
        changes.put("avatarUrl", avatarUrl);

        db.collection("users")
                .document(uid)
                .update(changes)
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    currentAvatarUrl = avatarUrl;
                    newAvatarUri = null;

                    tvProfileName.setText(fullName);

                    showMessage("Đã cập nhật hồ sơ thành công!");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showMessage(
                            "Lưu hồ sơ thất bại: "
                                    + e.getMessage());
                });
    }

    private void confirmResetPassword() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            showMessage("Không tìm thấy email tài khoản");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Đặt lại mật khẩu")
                .setMessage(
                        "Gửi email đặt lại mật khẩu đến "
                                + user.getEmail() + "?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi email", (dialog, which) ->
                        auth.sendPasswordResetEmail(user.getEmail())
                                .addOnSuccessListener(unused ->
                                        showMessage(
                                                "Đã gửi yêu cầu. "
                                                        + "Hãy kiểm tra email."))
                                .addOnFailureListener(e ->
                                        showMessage(
                                                "Không gửi được: "
                                                        + e.getMessage()))
                )
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất tài khoản")
                .setMessage("Bạn có chắc muốn đăng xuất khỏi SchoolFind?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            try {
                UserStatusManager.setStatus(false);
            } catch (Exception ignored) {}
        }

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(
                ProfileActivity.this,
                AuthActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        performLogout();
    }

    private void setLoading(boolean isLoading) {
        loading = isLoading;

        progressBarProfile.setVisibility(
                isLoading ? View.VISIBLE : View.GONE);

        btnSaveProfile.setEnabled(!isLoading);
        btnChangeAvatar.setEnabled(!isLoading);
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}