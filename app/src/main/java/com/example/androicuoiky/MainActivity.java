package com.example.androicuoiky;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androicuoiky.adapters.PostAdapter;
import com.example.androicuoiky.models.Post;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private PostAdapter adapter;
    private List<Post> postList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvPosts = findViewById(R.id.rvPosts);
        FloatingActionButton fabAddPost = findViewById(R.id.fabAddPost);

        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        adapter = new PostAdapter(postList);
        rvPosts.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Nút mở màn hình đăng bài
        fabAddPost.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreatePostActivity.class));
        });

        // Lấy dữ liệu bài đăng theo thời gian thực (mới nhất lên đầu)
        loadPostsFromFirestore();
    }

    private void loadPostsFromFirestore() {
        db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                postList.add(dc.getDocument().toObject(Post.class));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}