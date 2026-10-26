package com.example.androicuoiky.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androicuoiky.R;
import com.example.androicuoiky.models.Comment;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnAvatarClickListener {
        void onAvatarClick(String userId);
    }

    private final Context context;
    private final List<Comment> commentList;
    private final OnAvatarClickListener avatarClickListener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CommentAdapter(Context context, List<Comment> commentList, OnAvatarClickListener avatarClickListener) {
        this.context = context;
        this.commentList = commentList;
        this.avatarClickListener = avatarClickListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.tvCommentAuthor.setText(comment.getAuthorName());

        if (TextUtils.isEmpty(comment.getContent())) {
            holder.tvCommentContent.setVisibility(View.GONE);
        } else {
            holder.tvCommentContent.setVisibility(View.VISIBLE);
            holder.tvCommentContent.setText(comment.getContent());
        }

        // Dòng mới an toàn:
        Date date = comment.getCreatedAtDate();
        if (date != null) {
            holder.tvCommentDate.setText(sdf.format(date));
        } else {
            holder.tvCommentDate.setText("");
        }

        // Tải ảnh bình luận nếu có
        if (!TextUtils.isEmpty(comment.getImageUrl())) {
            holder.ivCommentImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(comment.getImageUrl()).into(holder.ivCommentImage);
        } else {
            holder.ivCommentImage.setVisibility(View.GONE);
        }

        // Tải Avatar của người bình luận từ collection "users"
        if (!TextUtils.isEmpty(comment.getUserId())) {
            db.collection("users").document(comment.getUserId()).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String avatarUrl = userDoc.getString("avatarUrl");
                            if (!TextUtils.isEmpty(avatarUrl)) {
                                Glide.with(context).load(avatarUrl).into(holder.ivCommentAvatar);
                            } else {
                                holder.ivCommentAvatar.setImageResource(R.drawable.ic_avatar_default);
                            }
                        }
                    });

            // Sự kiện bấm vào Avatar để mở Mini Profile Dialog
            holder.ivCommentAvatar.setOnClickListener(v -> {
                if (avatarClickListener != null) {
                    avatarClickListener.onAvatarClick(comment.getUserId());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivCommentAvatar;
        TextView tvCommentAuthor, tvCommentContent, tvCommentDate;
        ImageView ivCommentImage;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCommentAvatar = itemView.findViewById(R.id.ivCommentAvatar);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            tvCommentDate = itemView.findViewById(R.id.tvCommentDate);
            ivCommentImage = itemView.findViewById(R.id.ivCommentImage);
        }
    }
}