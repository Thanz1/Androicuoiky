package com.example.androicuoiky;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androicuoiky.adapters.ChatAdapter;
import com.example.androicuoiky.models.ChatMessage;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ImageButton btnBackChat, btnSendMessage;
    private ShapeableImageView ivReceiverAvatar;
    private TextView tvReceiverName, tvReceiverStatus;
    private EditText etMessageInput;
    private RecyclerView rvChatMessages;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String currentUserId = "";
    private String receiverId = "";
    private String receiverName = "";
    private String chatRoomId = "";

    private ListenerRegistration receiverListener;
    private ListenerRegistration messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        receiverId = getIntent().getStringExtra("RECEIVER_ID");
        receiverName = getIntent().getStringExtra("RECEIVER_NAME");

        if (TextUtils.isEmpty(receiverId)) {
            Toast.makeText(this, "Không tìm thấy người nhận!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Tạo chatRoomId chung cho 2 người theo thứ tự alphabet
        if (currentUserId.compareTo(receiverId) < 0) {
            chatRoomId = currentUserId + "_" + receiverId;
        } else {
            chatRoomId = receiverId + "_" + currentUserId;
        }

        initViews();
        setupRecyclerView();
        loadReceiverInfoRealtime();
        listenMessages();

        btnBackChat.setOnClickListener(v -> finish());
        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void initViews() {
        btnBackChat = findViewById(R.id.btnBackChat);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        ivReceiverAvatar = findViewById(R.id.ivReceiverAvatar);
        tvReceiverName = findViewById(R.id.tvReceiverName);
        tvReceiverStatus = findViewById(R.id.tvReceiverStatus);
        etMessageInput = findViewById(R.id.etMessageInput);
        rvChatMessages = findViewById(R.id.rvChatMessages);

        tvReceiverName.setText(!TextUtils.isEmpty(receiverName) ? receiverName : "Học sinh");
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(this, messageList, currentUserId);
        rvChatMessages.setAdapter(chatAdapter);
    }

    // Lắng nghe realtime tên, avatar và trạng thái isOnline của đối phương
    private void loadReceiverInfoRealtime() {
        receiverListener = db.collection("users").document(receiverId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullName");
                        String avatarUrl = documentSnapshot.getString("avatarUrl");
                        Boolean isOnline = documentSnapshot.getBoolean("isOnline");

                        if (!TextUtils.isEmpty(name)) {
                            tvReceiverName.setText(name);
                        }
                        if (!TextUtils.isEmpty(avatarUrl)) {
                            Glide.with(ChatActivity.this).load(avatarUrl).into(ivReceiverAvatar);
                        }

                        // Cập nhật trạng thái hiển thị chuẩn xác
                        if (Boolean.TRUE.equals(isOnline)) {
                            tvReceiverStatus.setText("Đang hoạt động");
                            tvReceiverStatus.setTextColor(Color.parseColor("#4CAF50")); // Màu xanh lá
                        } else {
                            tvReceiverStatus.setText("Không hoạt động");
                            tvReceiverStatus.setTextColor(Color.parseColor("#9E9E9E")); // Màu xám
                        }
                    }
                });
    }

    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        etMessageInput.setText("");

        String messageId = db.collection("chats").document(chatRoomId)
                .collection("messages").document().getId();

        ChatMessage chatMessage = new ChatMessage(
                messageId,
                currentUserId,
                receiverId,
                messageText,
                Timestamp.now()
        );

        db.collection("chats").document(chatRoomId)
                .collection("messages").document(messageId)
                .set(chatMessage)
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi gửi tin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void listenMessages() {
        messagesListener = db.collection("chats").document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            messageList.add(message);
                        }
                        updateChatUI();
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateChatUI() {
        chatAdapter.notifyDataSetChanged();
        if (!messageList.isEmpty()) {
            rvChatMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserStatusManager.setStatus(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        UserStatusManager.setStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiverListener != null) receiverListener.remove();
        if (messagesListener != null) messagesListener.remove();
    }
}