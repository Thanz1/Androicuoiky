package com.example.androicuoiky.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androicuoiky.R;
import com.example.androicuoiky.models.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public interface OnFriendActionListener {
        void onRemoveFriend(User friend);
    }

    private final Context context;
    private final List<User> friendList;
    private final OnFriendActionListener listener;

    public FriendAdapter(Context context, List<User> friendList, OnFriendActionListener listener) {
        this.context = context;
        this.friendList = friendList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend = friendList.get(position);
        holder.tvFriendName.setText(friend.getFullName());
        holder.tvFriendClass.setText("Lớp: " + (!TextUtils.isEmpty(friend.getClassId()) ? friend.getClassId() : "Chưa rõ"));

        if (!TextUtils.isEmpty(friend.getAvatarUrl())) {
            Glide.with(context).load(friend.getAvatarUrl()).into(holder.ivFriendAvatar);
        } else {
            holder.ivFriendAvatar.setImageResource(R.drawable/ic_avatar_default);
        }

        holder.btnRemoveFriend.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveFriend(friend);
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivFriendAvatar;
        TextView tvFriendName, tvFriendClass;
        Button btnRemoveFriend;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFriendAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
            tvFriendClass = itemView.findViewById(R.id.tvFriendClass);
            btnRemoveFriend = itemView.findViewById(R.id.btnRemoveFriend);
        }
    }
}