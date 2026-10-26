package com.example.androicuoiky;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androicuoiky.adapters.FriendAdapter;
import com.example.androicuoiky.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {

    private RecyclerView rvFriends;
    private TextView tvEmptyFriends;
    private FriendAdapter friendAdapter;
    private final List<User> friendList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvFriends = findViewById(R.id.rvFriends);
        tvEmptyFriends = findViewById(R.id.tvEmptyFriends);

        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        friendAdapter = new FriendAdapter(this, friendList, this::removeFriend);
        rvFriends.setAdapter(friendAdapter);

        loadFriends();
    }

    private void loadFriends() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Lấy danh sách bạn bè từ subcollection "friends" của user hiện tại
        db.collection("users").document(currentUser.getUid()).collection("friends")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        friendList.clear();
                        if (value.isEmpty()) {
                            tvEmptyFriends.setVisibility(View.VISIBLE);
                            friendAdapter.notifyDataSetChanged();
                            return;
                        }

                        tvEmptyFriends.setVisibility(View.GONE);
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            // Lấy thông tin chi tiết của người bạn từ collection "users"
                            String friendUid = doc.getId();
                            db.collection("users").document(friendUid).get()
                                    .addOnSuccessListener(friendDoc -> {
                                        if (friendDoc.exists()) {
                                            User friend = friendDoc.toObject(User.class);
                                            if (friend != null) {
                                                friendList.add(friend);
                                                friendAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void removeFriend(User friend) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Xóa liên kết 2 chiều
        db.collection("users").document(currentUser.getUid()).collection("friends").document(friend.getUid()).delete();
        db.collection("users").document(friend.getUid()).collection("friends").document(currentUser.getUid()).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã hủy kết bạn với " + friend.getFullName(), Toast.LENGTH_SHORT).show());
    }
}