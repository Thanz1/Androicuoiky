package com.example.androicuoiky;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androicuoiky.adapters.PostAdapter;
import com.example.androicuoiky.models.Post;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MaterialCardView cardLost, cardFound;
    private RecyclerView rvPosts;
    private ProgressBar mainProgressBar;
    private Button btnFilterAll, btnFilterLost, btnFilterFound;
    private ExtendedFloatingActionButton fabAddPost;
    private ShapeableImageView ivUserAvatar;

    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();
    private final List<Post> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 1. Ánh xạ View
        rvPosts = findViewById(R.id.rvPosts);
        mainProgressBar = findViewById(R.id.mainProgressBar);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterLost = findViewById(R.id.btnFilterLost);
        btnFilterFound = findViewById(R.id.btnFilterFound);
        fabAddPost = findViewById(R.id.fabAddPost);
        ivUserAvatar = findViewById(R.id.ivUserAvatar);
        cardLost = findViewById(R.id.cardLost);
        cardFound = findViewById(R.id.cardFound);
        // 2. Cài đặt RecyclerView bài đăng
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(this, filteredList, post -> {
            Intent intent = new Intent(MainActivity.this, PostDetailActivity.class);
            intent.putExtra("POST_ID", post.getPostId());
            startActivity(intent);
        });
        rvPosts.setAdapter(postAdapter);

        // 3. Khởi tạo Avatar và Menu Dropdown
        setupAvatarDropdown();

        // 4. Sự kiện chuyển màn hình đăng bài
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });

        // 5. Sự kiện lọc bài
        btnFilterAll.setOnClickListener(v -> applyFilter("ALL"));
        btnFilterLost.setOnClickListener(v -> applyFilter("LOST"));
        btnFilterFound.setOnClickListener(v -> applyFilter("FOUND"));
// Nhấn thẻ tìm đồ thất lạc
        cardLost.setOnClickListener(v -> {
            applyFilter("LOST");
        });

// Nhấn thẻ đồ nhặt được
        cardFound.setOnClickListener(v -> {
            applyFilter("FOUND");
        });
        loadPostsFromFirestore();
    }

    private void setupAvatarDropdown() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Lắng nghe thay đổi avatar từ Firestore
            db.collection("users").document(currentUser.getUid())
                    .addSnapshotListener((doc, e) -> {
                        if (doc != null && doc.exists()) {
                            String avatarUrl = doc.getString("avatarUrl");
                            if (!TextUtils.isEmpty(avatarUrl)) {
                                Glide.with(MainActivity.this)
                                        .load(avatarUrl)
                                        .into(ivUserAvatar);
                            }
                        }
                    });
        }

        // Bấm vào avatar để hiển thị Popup Dropdown
        ivUserAvatar.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.menu_avatar_dropdown, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_profile) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    return true;
                } else if (id == R.id.menu_friends) {
                    startActivity(new Intent(MainActivity.this, FriendsActivity.class));
                    return true;
                } else if (id == R.id.menu_logout) {
                    mAuth.signOut();
                    Toast.makeText(MainActivity.this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
                    finish();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void loadPostsFromFirestore() {
        mainProgressBar.setVisibility(View.VISIBLE);
        db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    mainProgressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(MainActivity.this, "Lỗi đọc dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        postList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Post post = new Post();
                                post.setPostId(doc.getString("postId"));
                                post.setUserId(doc.getString("userId"));
                                post.setAuthorName(doc.getString("authorName"));
                                post.setClassId(doc.getString("classId"));
                                post.setTitle(doc.getString("title"));
                                post.setDescription(doc.getString("description"));
                                post.setLocation(doc.getString("location"));
                                post.setCategory(doc.getString("category"));
                                post.setImageUrl(doc.getString("imageUrl"));
                                post.setStatus(doc.getString("status"));
                                Boolean isAnonymous = doc.getBoolean("isAnonymous");
                                post.setAnonymous(isAnonymous != null ? isAnonymous : false);
                                Boolean isScoreApproved = doc.getBoolean("isScoreApproved");
                                post.setScoreApproved(isScoreApproved != null ? isScoreApproved : false);

                                Object createdAtObj = doc.get("createdAt");
                                if (createdAtObj instanceof Timestamp) {
                                    post.setCreatedAt((Timestamp) createdAtObj);
                                } else if (createdAtObj instanceof Long) {
                                    post.setCreatedAt(new Timestamp(new java.util.Date((Long) createdAtObj)));
                                } else {
                                    post.setCreatedAt(Timestamp.now());
                                }
                                postList.add(post);
                            } catch (Exception ignored) {
                            }
                        }
                        applyFilter(currentFilter);
                    }
                });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredList.clear();
        if ("ALL".equalsIgnoreCase(filter)) {
            filteredList.addAll(postList);
        } else {
            for (Post p : postList) {
                if (filter.equalsIgnoreCase(p.getCategory())) {
                    filteredList.add(p);
                }
            }
        }
        postAdapter.notifyDataSetChanged();
    }
}