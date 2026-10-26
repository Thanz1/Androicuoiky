package com.example.androicuoiky.adapters;

import android.content.Context;
import android.graphics.Color;
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
import com.example.androicuoiky.models.Post;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    private final Context context;
    private final List<Post> postList;
    private final OnPostClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public PostAdapter(Context context, List<Post> postList, OnPostClickListener listener) {
        this.context = context;
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvTitle.setText(post.getTitle());
        holder.tvLocation.setText("Vị trí: " + post.getLocation());

        // Phân loại LOST (Mất đồ) hay FOUND (Nhặt được)
        if ("LOST".equalsIgnoreCase(post.getCategory())) {
            holder.tvCategoryBadge.setText("CẦN TÌM ĐỒ");
            holder.tvCategoryBadge.setBackgroundColor(Color.parseColor("#D32F2F")); // Màu đỏ
        } else {
            holder.tvCategoryBadge.setText("NHẶT ĐƯỢC");
            holder.tvCategoryBadge.setBackgroundColor(Color.parseColor("#388E3C")); // Màu xanh lá
        }

        // Trạng thái đồ vật
        if ("RESOLVED".equalsIgnoreCase(post.getStatus())) {
            holder.tvStatus.setText("ĐÃ TRAO TRẢ");
            holder.tvStatus.setTextColor(Color.parseColor("#388E3C"));
        } else {
            holder.tvStatus.setText("ĐANG CHỜ TÌM");
            holder.tvStatus.setTextColor(Color.parseColor("#F57C00"));
        }

        // Xử lý logic Ẩn danh
        if (post.isAnonymous()) {
            holder.tvAuthor.setText("Đăng bởi: Người dùng ẩn danh");
        } else {
            String authorText = post.getAuthorName() != null ? post.getAuthorName() : "Học sinh";
            if (!TextUtils.isEmpty(post.getClassId())) {
                authorText += " (" + post.getClassId() + ")";
            }
            holder.tvAuthor.setText("Đăng bởi: " + authorText);
        }

        // Ngày đăng
        if (post.getCreatedAt() != null) {
            holder.tvDate.setText(dateFormat.format(post.getCreatedAtDate()));
        }

        // Load ảnh bằng Glide
        if (!TextUtils.isEmpty(post.getImageUrl())) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(post.getImageUrl())
                    .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryBadge, tvStatus, tvTitle, tvLocation, tvAuthor, tvDate;
        ImageView ivPostImage;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
        }
    }
}