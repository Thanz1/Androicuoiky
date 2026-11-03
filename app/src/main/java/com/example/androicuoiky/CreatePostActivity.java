package com.example.androicuoiky;

import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import com.example.androicuoiky.models.PostMedia;
import com.example.androicuoiky.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etLocation, etDescription;
    private RadioGroup rgCategory;
    private RadioButton rbLost;
    private ImageView ivPreview;
    private Button btnSelectImage, btnSubmitPost;
    private CheckBox cbAnonymous;
    private ProgressBar progressBar;

    private final List<Uri> selectedMediaUris = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private final ActivityResultLauncher<Intent> mediaPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK
                                || result.getData() == null) {
                            return;
                        }

                        Intent data = result.getData();
                        ClipData clipData = data.getClipData();

                        if (clipData != null) {
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                addSelectedMedia(
                                        clipData.getItemAt(i).getUri()
                                );
                            }
                        } else if (data.getData() != null) {
                            addSelectedMedia(data.getData());
                        }

                        updateMediaPreview();
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

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

        btnSelectImage.setOnClickListener(v -> openMediaPicker());
        btnSubmitPost.setOnClickListener(v -> validateAndSubmit());
    }

    private void openMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        mediaPickerLauncher.launch(intent);
    }

    private void addSelectedMedia(Uri uri) {
        if (uri == null || selectedMediaUris.contains(uri)) return;

        String mimeType = getContentResolver().getType(uri);

        if (mimeType == null ||
                !(mimeType.startsWith("image/")
                        || mimeType.startsWith("video/"))) {
            return;
        }

        selectedMediaUris.add(uri);
    }

    private void updateMediaPreview() {
        if (selectedMediaUris.isEmpty()) {
            ivPreview.setVisibility(View.GONE);
            return;
        }

        Uri first = selectedMediaUris.get(0);
        String mime = getContentResolver().getType(first);

        ivPreview.setVisibility(View.VISIBLE);

        if (mime != null && mime.startsWith("image/")) {
            ivPreview.setImageURI(first);
        } else {
            ivPreview.setImageResource(
                    android.R.drawable.ic_media_play
            );
        }

        Toast.makeText(
                this,
                "Đã chọn " + selectedMediaUris.size() + " file",
                Toast.LENGTH_SHORT
        ).show();
    }

    private long getFileSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{OpenableColumns.SIZE},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()
                    && !cursor.isNull(0)) {
                return cursor.getLong(0);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private void uploadMediaSequentially(
            int index,
            String postId,
            List<PostMedia> uploaded,
            Post newPost
    ) {
        if (index >= selectedMediaUris.size()) {

            newPost.setMedia(uploaded);

            for (PostMedia item : uploaded) {
                if ("IMAGE".equals(item.getType())) {
                    newPost.setImageUrl(item.getUrl());
                    break;
                }
            }

            db.collection("posts")
                    .document(postId)
                    .set(newPost)
                    .addOnSuccessListener(unused -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Đăng bài thành công!",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> showUploadError(e));

            return;
        }

        Uri uri = selectedMediaUris.get(index);
        String mime = getContentResolver().getType(uri);

        if (mime == null || !(mime.startsWith("image/")
                || mime.startsWith("video/"))) {
            showUploadError(new IllegalArgumentException(
                    "Định dạng file không hợp lệ"));
            return;
        }

        long size = getFileSize(uri);
        long maxSize = mime.startsWith("video/")
                ? 100L * 1024 * 1024
                : 15L * 1024 * 1024;

        if (size < 0 || size > maxSize) {
            showUploadError(new IllegalArgumentException(
                    "File không đọc được dung lượng hoặc quá lớn"));
            return;
        }

        String type = mime.startsWith("video/")
                ? "VIDEO" : "IMAGE";

        String path = "posts/" + postId + "/"
                + UUID.randomUUID();

        StorageReference ref =
                storage.getReference().child(path);

        StorageMetadata metadata =
                new StorageMetadata.Builder()
                        .setContentType(mime)
                        .build();

        ref.putFile(uri, metadata)
                .addOnSuccessListener(task ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {

                                    uploaded.add(new PostMedia(
                                            downloadUri.toString(),
                                            type,
                                            path
                                    ));

                                    uploadMediaSequentially(
                                            index + 1,
                                            postId,
                                            uploaded,
                                            newPost
                                    );
                                })
                                .addOnFailureListener(this::showUploadError)
                )
                .addOnFailureListener(this::showUploadError);
    }

    private void showUploadError(Exception e) {
        setLoading(false);

        Toast.makeText(
                this,
                "Lỗi đăng bài: " + e.getMessage(),
                Toast.LENGTH_LONG
        ).show();
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

                    String postId = db.collection("posts").document().getId();

                    // Sinh mã xác nhận tự động nếu là đồ nhặt được (FOUND)
                    String verificationCode = "";
                    if ("FOUND".equals(category)) {
                        verificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    }

                    Post newPost = new Post(
                            postId,
                            currentUser.getUid(),
                            authorName,
                            classId,
                            title,
                            description,
                            location,
                            category,
                            "",
                            "PENDING",
                            isAnonymous,
                            false,
                            Timestamp.now()
                    );

                    newPost.setVerificationCode(verificationCode);
                    newPost.setClaimStatus("PENDING");

                    uploadMediaSequentially(
                            0,
                            postId,
                            new ArrayList<>(),
                            newPost
                    );
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(CreatePostActivity.this, "Lỗi lấy thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSubmitPost.setEnabled(!isLoading);
        btnSelectImage.setEnabled(!isLoading);
    }
}