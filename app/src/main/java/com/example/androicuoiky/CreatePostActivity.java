package com.example.androicuoiky;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.androicuoiky.models.Post;
import com.example.androicuoiky.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etLocation, etDescription;
    private RadioGroup rgCategory;
    private RadioButton rbLost;
    private ImageView ivPreview;
    private Button btnSelectImage, btnSubmitPost;
    private CheckBox cbAnonymous;
    private ProgressBar progressBar;

    private Uri selectedImageUri = null;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Bộ nhận kết quả chọn ảnh từ thư viện
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivPreview.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.firestore();
        storage = FirebaseStorage.getInstance();

        // Ánh xạ View
        etTitle = findViewById(R.id.etTitle);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        rgCategory = findViewById(R.id.rgCategory);
        rbLost = findViewById(R.id.rbLost);
        ivPreview = findViewById(R.id.ivPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSubmitPost = findViewById(R.id.btnSubmitPost);
        cbAnonymous = findViewById(R.id.cbAnonymous);
        progressBar = findViewById(R.id.progressBar);

        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSubmitPost.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {
        String title = etTitle.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = rbLost.isChecked() ? "LOST" : "FOUND";
        boolean isAnonymous = cbAnonymous.isChecked();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Vui lòng nhập tên đồ vật");
            return;
        }
        if (TextUtils.isEmpty(location)) {
            etLocation.setError("Vui lòng nhập vị trí");
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước khi đăng bài!", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Lấy thông tin lớp và tên người dùng hiện tại từ Firestore
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String authorName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Học sinh";
                    String classId = "Chưa rõ";

                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            if (!TextUtils.isEmpty(user.getFullName())) authorName = user.getFullName();
                            if (!TextUtils.isEmpty(user.getClassId())) classId = user.getClassId();
                        }
                    }

                    // Nếu có chọn ảnh -> Upload ảnh lên Firebase Storage trước
                    if (selectedImageUri != null) {
                        uploadImageAndCreatePost(currentUser.getUid(), authorName, classId, title, description, location, category, isAnonymous);
                    } else {
                        // Không có ảnh -> Lưu bài đăng trực tiếp với ảnh mặc định rỗng
                        savePostToFirestore(currentUser.getUid(), authorName, classId, title, description, location, category, "", isAnonymous);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(CreatePostActivity.this, "Lỗi lấy thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImageAndCreatePost(String userId, String authorName, String classId,
                                          String title, String description, String location,
                                          String category, boolean isAnonymous) {
        String imageFileName = "posts/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(imageFileName);

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    savePostToFirestore(userId, authorName, classId, title, description, location, category, imageUrl, isAnonymous);
                }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(CreatePostActivity.this, "Lỗi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void savePostToFirestore(String userId, String authorName, String classId,
                                     String title, String description, String location,
                                     String category, String imageUrl, boolean isAnonymous) {
        String postId = db.collection("posts").document().getId();

        Post newPost = new Post(
                postId,
                userId,
                authorName,
                classId,
                title,
                description,
                location,
                category,
                imageUrl,
                "PENDING",          // Trạng thái mặc định: Đang tìm/chờ nhận
                isAnonymous,
                false,              // Chưa duyệt điểm thi đua
                Timestamp.now()
        );

        db.collection("posts").document(postId).set(newPost)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(CreatePostActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Đóng màn hình sau khi đăng xong
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(CreatePostActivity.this, "Lỗi lưu bài: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSubmitPost.setEnabled(!isLoading);
        btnSelectImage.setEnabled(!isLoading);
    }
}