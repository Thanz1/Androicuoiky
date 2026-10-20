package com.example.androicuoiky.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.androicuoiky.R;
import com.example.androicuoiky.models.Post;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<Post> postList;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.tvTitle.setText(post.title);
        holder.tvLocation.setText("Địa điểm: " + post.location);
        holder.tvDescription.setText(post.description);
        holder.tvPhone.setText("Liên hệ: " + post.authorPhone);
        holder.tvAuthorName.setText("Người đăng: " + (post.authorName != null && !post.authorName.isEmpty() ? post.authorName : "Ẩn danh"));

        if ("LOST".equals(post.postType)) {
            holder.tvPostType.setText("MẤT ĐỒ");
            holder.tvPostType.setTextColor(Color.parseColor("#D32F2F"));
            holder.tvPostType.setBackgroundColor(Color.parseColor("#FFEBEE"));
        } else {
            holder.tvPostType.setText("NHẶT ĐƯỢC");
            holder.tvPostType.setTextColor(Color.parseColor("#2E7D32"));
            holder.tvPostType.setBackgroundColor(Color.parseColor("#E8F5E9"));
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvPostType, tvTitle, tvLocation, tvDescription, tvPhone, tvAuthorName;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPostType = itemView.findViewById(R.id.tvPostType);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
        }
    }
}