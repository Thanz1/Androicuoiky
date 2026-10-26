package com.example.androicuoiky.adapters;

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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<Comment> commentList;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public CommentAdapter(Context context, List<Comment> commentList) {
        this.context = context;
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

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

        if (comment.getCreatedAt() != null) {
            holder.tvCommentDate.setText(sdf.format(comment.getCreatedAt().toDate()));
        }

        if (!TextUtils.isEmpty(comment.getImageUrl())) {
            holder.ivCommentImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(comment.getImageUrl()).into(holder.ivCommentImage);
        } else {
            holder.ivCommentImage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommentAuthor, tvCommentContent, tvCommentDate;
        ImageView ivCommentImage;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            tvCommentDate = itemView.findViewById(R.id.tvCommentDate);
            ivCommentImage = itemView.findViewById(R.id.ivCommentImage);
        }
    }
}