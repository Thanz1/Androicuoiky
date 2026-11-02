package com.example.androicuoiky;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androicuoiky.adapters.CommentAdapter;
import com.example.androicuoiky.models.Comment;
import com.example.androicuoiky.models.Post;
import com.example.androicuoiky.models.PostMedia;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PostDetailActivity extends AppCompatActivity {

    private TextView tvDetailTitle, tvDetailCategory, tvDetailLocation, tvDetailAuthor, tvDetailDescription;
    private TextView tvDetailStatus, tvDetailDate;
    private ImageButton btnDetailBack;
    private ShapeableImageView ivDetailAvatar;
    private TextView tvReactionCount, tvCommentCount, tvReactionIcon, tvReactionText;
    private ImageView ivDetailImage, ivCommentPreview;
    private LinearLayout layoutDetailMedia;
    private VideoView activeVideo;
    private Button btnMarkResolved, btnDeletePost, btnAddFriend, btnOpenChat;
    private LinearLayout btnReaction, btnCommentAction;
    private RelativeLayout layoutCommentPreview;
    private EditText etComment;
    private ImageButton btnAttachImage, btnSendComment;
    private ImageView btnRemovePreview;
    private RecyclerView rvComments;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private String postId;
    private Post currentPost;

    private CommentAdapter commentAdapter;
    private final List<Comment> commentList = new ArrayList<>();
    private Uri commentImageUri = null;

    private final ActivityResultLauncher<String> commentImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    commentImageUri = uri;
                    ivCommentPreview.setImageURI(uri);
                    layoutCommentPreview.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        postId = getIntent().getStringExtra("POST_ID");

        initViews();
        setupRecyclerView();

        if (postId != null) {
            loadPostDetail();
            listenForComments();
        }

        setupEvents();
    }

    private void initViews() {
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailAuthor = findViewById(R.id.tvDetailAuthor);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailDate = findViewById(R.id.tvDetailDate);
        btnDetailBack = findViewById(R.id.btnDetailBack);
        ivDetailAvatar = findViewById(R.id.ivDetailAvatar);
        ivDetailImage = findViewById(R.id.ivDetailImage);
        layoutDetailMedia = findViewById(R.id.layoutDetailMedia);
        btnMarkResolved = findViewById(R.id.btnMarkResolved);
        btnDeletePost = findViewById(R.id.btnDeletePost);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        btnOpenChat = findViewById(R.id.btnOpenChat);

        tvReactionCount = findViewById(R.id.tvReactionCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        tvReactionIcon = findViewById(R.id.tvReactionIcon);
        tvReactionText = findViewById(R.id.tvReactionText);
        btnReaction = findViewById(R.id.btnReaction);
        btnCommentAction = findViewById(R.id.btnCommentAction);

        layoutCommentPreview = findViewById(R.id.layoutCommentPreview);
        ivCommentPreview = findViewById(R.id.ivCommentPreview);
        btnRemovePreview = findViewById(R.id.btnRemovePreview);
        etComment = findViewById(R.id.etComment);
        btnAttachImage = findViewById(R.id.btnAttachImage);
        btnSendComment = findViewById(R.id.btnSendComment);
        rvComments = findViewById(R.id.rvComments);
    }

    private void setupRecyclerView() {
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        // Truyền callback mở User Profile Dialog khi click vào avatar comment
        commentAdapter = new CommentAdapter(this, commentList, this::showUserProfileDialog);
        rvComments.setAdapter(commentAdapter);
    }

    private void setupEvents() {
        btnDetailBack.setOnClickListener(v -> finish());

        btnCommentAction.setOnClickListener(v -> {
            etComment.requestFocus();
            etComment.setFocusableInTouchMode(true);
        });

        btnReaction.setOnClickListener(v -> toggleReaction("LIKE"));

        btnReaction.setOnLongClickListener(v -> {
            showReactionPopup(v);
            return true;
        });

        btnAttachImage.setOnClickListener(v -> commentImageLauncher.launch("image/*"));

        btnRemovePreview.setOnClickListener(v -> {
            commentImageUri = null;
            layoutCommentPreview.setVisibility(View.GONE);
        });

        btnSendComment.setOnClickListener(v -> sendComment());
        btnMarkResolved.setOnClickListener(v -> markPostAsResolved());
        btnDeletePost.setOnClickListener(v -> deletePost());

        btnOpenChat.setOnClickListener(v -> {
            if (currentPost != null && !TextUtils.isEmpty(currentPost.getUserId())) {
                Intent intent = new Intent(PostDetailActivity.this, ChatActivity.class);
                intent.putExtra("RECEIVER_ID", currentPost.getUserId());
                intent.putExtra("RECEIVER_NAME", currentPost.getAuthorName());
                startActivity(intent);
            }
        });
    }

    private void loadPostDetail() {
        db.collection("posts").document(postId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        currentPost = documentSnapshot.toObject(Post.class);
                        if (currentPost != null) {
                            displayData(currentPost);
                        }
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void displayData(Post post) {
        tvDetailTitle.setText(post.getTitle());
        tvDetailLocation.setText("📍 " + (TextUtils.isEmpty(post.getLocation()) ? "Chưa xác định" : post.getLocation()));
        tvDetailDescription.setText(post.getDescription());

        // Ngày đăng
        if (post.getCreatedAt() != null) {
            SimpleDateFormat formatter = new SimpleDateFormat(
                    "dd/MM/yyyy · HH:mm",
                    Locale.getDefault()
            );
            tvDetailDate.setText(formatter.format(post.getCreatedAtDate()));
        } else {
            tvDetailDate.setText("");
        }

        // Nhãn loại bài đăng
        if ("LOST".equalsIgnoreCase(post.getCategory())) {
            tvDetailCategory.setText("MẤT ĐỒ");
            tvDetailCategory.setTextColor(getColor(R.color.sf_danger));
            tvDetailCategory.setBackgroundTintList(null);
            tvDetailCategory.setBackgroundColor(getColor(R.color.sf_danger_light));
        } else {
            tvDetailCategory.setText("NHẶT ĐƯỢC");
            tvDetailCategory.setTextColor(getColor(R.color.sf_success));
            tvDetailCategory.setBackgroundColor(getColor(R.color.sf_success_light));
        }

        // Nhãn trạng thái
        if ("RESOLVED".equalsIgnoreCase(post.getStatus())) {
            tvDetailStatus.setText("✓ Đã trao trả");
            tvDetailStatus.setTextColor(getColor(R.color.sf_success));
            tvDetailStatus.setBackgroundColor(getColor(R.color.sf_success_light));
        } else {
            tvDetailStatus.setText(
                    "LOST".equalsIgnoreCase(post.getCategory())
                            ? "Đang tìm kiếm"
                            : "Đang tìm chủ"
            );
            tvDetailStatus.setTextColor(getColor(R.color.sf_warning));
            tvDetailStatus.setBackgroundColor(getColor(R.color.sf_warning_light));
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";

        if (post.isAnonymous()) {
            tvDetailAuthor.setText("Người đăng: Ẩn danh");
            ivDetailAvatar.setImageResource(R.drawable.ic_avatar_default);
            btnAddFriend.setVisibility(View.GONE);
            btnOpenChat.setVisibility(View.GONE);
        } else {
            String authorInfo = TextUtils.isEmpty(post.getAuthorName()) ? "Học sinh" : post.getAuthorName();
            if (!TextUtils.isEmpty(post.getClassId())) {
                authorInfo += " · Lớp " + post.getClassId();
            }
            tvDetailAuthor.setText(authorInfo);

            ivDetailAvatar.setImageResource(R.drawable.ic_avatar_default);
            if (!TextUtils.isEmpty(post.getUserId())) {
                String authorId = post.getUserId();
                db.collection("users")
                        .document(authorId)
                        .get()
                        .addOnSuccessListener(document -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (document.exists()
                                    && currentPost != null
                                    && !currentPost.isAnonymous()
                                    && authorId.equals(currentPost.getUserId())) {
                                String avatar = document.getString("avatarUrl");
                                if (!TextUtils.isEmpty(avatar)) {
                                    Glide.with(this)
                                            .load(avatar)
                                            .circleCrop()
                                            .into(ivDetailAvatar);
                                }
                            }
                        });
            }

            if (!TextUtils.isEmpty(post.getUserId()) && !post.getUserId().equals(currentUid)) {
                btnOpenChat.setVisibility(View.VISIBLE);
                updateFriendButton(btnAddFriend, post.getUserId());
            } else {
                btnAddFriend.setVisibility(View.GONE);
                btnOpenChat.setVisibility(View.GONE);
            }
        }

        displayDetailMedia(post);

        if (currentUid.equals(post.getUserId())) {
            btnDeletePost.setVisibility(View.VISIBLE);
            if (!"RESOLVED".equalsIgnoreCase(post.getStatus())) {
                btnMarkResolved.setVisibility(View.VISIBLE);
            } else {
                btnMarkResolved.setVisibility(View.GONE);
            }
        } else {
            btnDeletePost.setVisibility(View.GONE);
            btnMarkResolved.setVisibility(View.GONE);
        }

        updateReactionUI();
    }

    private void displayDetailMedia(Post post) {
        if (activeVideo != null) {
            activeVideo.stopPlayback();
            activeVideo = null;
        }

        layoutDetailMedia.removeAllViews();

        List<PostMedia> media = new ArrayList<>(post.getMedia());

        // Hỗ trợ bài đăng cũ
        if (media.isEmpty()
                && !TextUtils.isEmpty(post.getImageUrl())) {
            media.add(new PostMedia(
                    post.getImageUrl(), "IMAGE", ""
            ));
        }

        ivDetailImage.setVisibility(View.GONE);

        if (media.isEmpty()) {
            layoutDetailMedia.setVisibility(View.GONE);
            return;
        }

        layoutDetailMedia.setVisibility(View.VISIBLE);

        float density = getResources()
                .getDisplayMetrics().density;

        int mediaHeight = (int) (260 * density);
        int marginBottom = (int) (12 * density);

        for (PostMedia item : media) {
            if (TextUtils.isEmpty(item.getUrl())) continue;

            if ("IMAGE".equals(item.getType())) {
                ImageView image = new ImageView(this);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                -1, mediaHeight
                        );
                params.bottomMargin = marginBottom;

                image.setLayoutParams(params);
                image.setScaleType(
                        ImageView.ScaleType.FIT_CENTER
                );

                image.setBackgroundColor(
                        getColor(R.color.sf_surface_variant)
                );

                Glide.with(this)
                        .load(item.getUrl())
                        .into(image);

                layoutDetailMedia.addView(image);

            } else if ("VIDEO".equals(item.getType())) {
                VideoView videoView = new VideoView(this);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                -1, mediaHeight
                        );
                params.bottomMargin = marginBottom;

                videoView.setLayoutParams(params);
                videoView.setVideoURI(
                        Uri.parse(item.getUrl())
                );

                MediaController controller =
                        new MediaController(this);

                controller.setAnchorView(videoView);
                videoView.setMediaController(controller);

                // Người dùng bấm để phát video
                videoView.setOnClickListener(v -> {
                    if (!videoView.isPlaying()) {
                        if (activeVideo != null &&
                                activeVideo != videoView) {
                            activeVideo.pause();
                        }
                        activeVideo = videoView;
                        videoView.start();
                    } else {
                        videoView.pause();
                    }
                });

                layoutDetailMedia.addView(videoView);
            }
        }
    }

    @Override
    protected void onStop() {
        if (activeVideo != null) {
            activeVideo.pause();
        }
        super.onStop();
    }

    // Hiển thị Mini Profile Dialog khi bấm vào Avatar người comment
    @SuppressLint("SetTextI18n")
    private void showUserProfileDialog(String targetUserId) {
        if (targetUserId == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_preview, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ShapeableImageView ivDialogAvatar = dialogView.findViewById(R.id.ivDialogAvatar);
        TextView tvDialogName = dialogView.findViewById(R.id.tvDialogName);
        TextView tvDialogClass = dialogView.findViewById(R.id.tvDialogClass);
        Button btnDialogChat = dialogView.findViewById(R.id.btnDialogChat);
        Button btnDialogFriend = dialogView.findViewById(R.id.btnDialogFriend);
        Button btnDialogViewProfile = dialogView.findViewById(R.id.btnDialogViewProfile);

        db.collection("users").document(targetUserId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            String name = doc.getString("fullName");
            String avatar = doc.getString("avatarUrl");
            String classId = doc.getString("classId");

            tvDialogName.setText(name != null ? name : "Học sinh");
            tvDialogClass.setText("Lớp: " + (classId != null ? classId : "Chưa rõ"));
            if (avatar != null && !avatar.isEmpty()) {
                Glide.with(this).load(avatar).into(ivDialogAvatar);
            }

            // Cập nhật trạng thái nút kết bạn có chờ duyệt
            updateFriendButton(btnDialogFriend, targetUserId);

            btnDialogChat.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("RECEIVER_ID", targetUserId);
                intent.putExtra("RECEIVER_NAME", name);
                startActivity(intent);
            });

            btnDialogViewProfile.setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(this, "Xem trang của " + name, Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }

    // Cập nhật trạng thái và hành vi của nút Kết bạn (3 trạng thái chuẩn)
    @SuppressLint("SetTextI18n")
    private void updateFriendButton(Button btn, String targetUserId) {
        btn.setVisibility(View.VISIBLE);
        FriendManager.checkStatus(targetUserId, (status, requestId) -> {
            switch (status) {
                case "SELF":
                    btn.setVisibility(View.GONE);
                    break;
                case "FRIEND":
                    btn.setText("Bạn bè ✓");
                    btn.setBackgroundColor(Color.parseColor("#388E3C"));
                    btn.setOnClickListener(v -> FriendManager.cancelOrRemove(targetUserId, () -> updateFriendButton(btn, targetUserId)));
                    break;
                case "SENT":
                    btn.setText("Đã gửi lời mời ✕");
                    btn.setBackgroundColor(Color.parseColor("#757575"));
                    btn.setOnClickListener(v -> FriendManager.cancelOrRemove(targetUserId, () -> updateFriendButton(btn, targetUserId)));
                    break;
                case "RECEIVED":
                    btn.setText("Chấp nhận ✓");
                    btn.setBackgroundColor(Color.parseColor("#1877F2"));
                    btn.setOnClickListener(v -> FriendManager.acceptRequest(targetUserId, () -> updateFriendButton(btn, targetUserId)));
                    break;
                default: // "NONE"
                    btn.setText("+ Thêm bạn bè");
                    btn.setBackgroundColor(Color.parseColor("#1877F2"));
                    btn.setOnClickListener(v -> FriendManager.sendRequest(targetUserId, () -> updateFriendButton(btn, targetUserId)));
                    break;
            }
        });
    }

    private void showReactionPopup(View anchorView) {
        LinearLayout popupLayout = new LinearLayout(this);
        popupLayout.setOrientation(LinearLayout.HORIZONTAL);
        popupLayout.setBackgroundColor(Color.WHITE);
        popupLayout.setPadding(24, 16, 24, 16);
        popupLayout.setElevation(16f);

        String[] icons = {"👍", "❤️", "😢", "😮"};
        String[] types = {"LIKE", "LOVE", "SAD", "WOW"};

        final PopupWindow popupWindow = new PopupWindow(popupLayout,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        for (int i = 0; i < icons.length; i++) {
            final String type = types[i];
            TextView tv = new TextView(this);
            tv.setText(icons[i]);
            tv.setTextSize(26);
            tv.setPadding(16, 4, 16, 4);
            tv.setOnClickListener(v -> {
                toggleReaction(type);
                popupWindow.dismiss();
            });
            popupLayout.addView(tv);
        }

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0], location[1] - 130);
    }

    private void toggleReaction(String reactionType) {
        if (mAuth.getCurrentUser() == null || currentPost == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        DocumentReference postRef = db.collection("posts").document(postId);

        Map<String, String> reactions = currentPost.getReactions();
        if (reactions.containsKey(uid) && reactionType.equals(reactions.get(uid))) {
            reactions.remove(uid);
        } else {
            reactions.put(uid, reactionType);
        }

        postRef.update("reactions", reactions).addOnSuccessListener(aVoid -> updateReactionUI());
    }

    @SuppressLint("SetTextI18n")
    private void updateReactionUI() {
        if (currentPost == null) return;
        Map<String, String> reactions = currentPost.getReactions();
        tvReactionCount.setText(reactions.size() + " lượt tương tác");

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            if (reactions.containsKey(uid)) {
                String type = reactions.get(uid);
                if (type == null) type = "LIKE";

                switch (type) {
                    case "LOVE":
                        tvReactionIcon.setText("❤️");
                        tvReactionText.setText("Yêu thích");
                        tvReactionText.setTextColor(Color.parseColor("#E91E63"));
                        break;
                    case "SAD":
                        tvReactionIcon.setText("😢");
                        tvReactionText.setText("Buồn");
                        tvReactionText.setTextColor(Color.parseColor("#FF9800"));
                        break;
                    case "WOW":
                        tvReactionIcon.setText("😮");
                        tvReactionText.setText("Wow");
                        tvReactionText.setTextColor(Color.parseColor("#FF9800"));
                        break;
                    default:
                        tvReactionIcon.setText("👍");
                        tvReactionText.setText("Thích");
                        tvReactionText.setTextColor(Color.parseColor("#1976D2"));
                        break;
                }
                return;
            }
        }
        tvReactionIcon.setText("👍");
        tvReactionText.setText("Thích");
        tvReactionText.setTextColor(Color.parseColor("#616161"));
    }

    private void sendComment() {
        String content = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(content) && commentImageUri == null) {
            Toast.makeText(this, "Vui lòng nhập nội dung hoặc chọn ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước khi bình luận!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendComment.setEnabled(false);
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String author = "Học sinh";
            if (userDoc.exists() && userDoc.getString("fullName") != null) {
                author = userDoc.getString("fullName");
            } else if (mAuth.getCurrentUser().getDisplayName() != null) {
                author = mAuth.getCurrentUser().getDisplayName();
            }

            final String finalAuthor = author;
            if (commentImageUri != null) {
                String fileName = "comments/" + UUID.randomUUID().toString() + ".jpg";
                StorageReference ref = storage.getReference().child(fileName);
                ref.putFile(commentImageUri)
                        .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri ->
                                saveCommentToFirestore(uid, finalAuthor, content, uri.toString())
                        ))
                        .addOnFailureListener(e -> {
                            btnSendComment.setEnabled(true);
                            Toast.makeText(this, "Lỗi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                saveCommentToFirestore(uid, finalAuthor, content, "");
            }
        });
    }

    private void saveCommentToFirestore(String uid, String author, String content, String imageUrl) {
        String commentId = db.collection("posts").document(postId).collection("comments").document().getId();
        Comment comment = new Comment(commentId, uid, author, content, imageUrl, Timestamp.now());

        db.collection("posts").document(postId).collection("comments").document(commentId)
                .set(comment)
                .addOnSuccessListener(aVoid -> {
                    btnSendComment.setEnabled(true);
                    etComment.setText("");
                    commentImageUri = null;
                    layoutCommentPreview.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    btnSendComment.setEnabled(true);
                    Toast.makeText(this, "Lỗi gửi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
    private void listenForComments() {
        db.collection("posts").document(postId).collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        commentList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            commentList.add(doc.toObject(Comment.class));
                        }
                        commentAdapter.notifyDataSetChanged();
                        tvCommentCount.setText(commentList.size() + " bình luận");
                        if (!commentList.isEmpty()) {
                            rvComments.smoothScrollToPosition(commentList.size() - 1);
                        }
                    }
                });
    }

    private void markPostAsResolved() {
        db.collection("posts").document(postId)
                .update("status", "RESOLVED")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã cập nhật trạng thái đã trao trả!", Toast.LENGTH_SHORT).show());
    }

    private void deletePost() {
        db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa bài đăng!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
