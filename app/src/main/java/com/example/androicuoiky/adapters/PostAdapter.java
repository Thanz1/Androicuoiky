package com.example.androicuoiky.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.androicuoiky.R;
import com.example.androicuoiky.models.Post;
import com.example.androicuoiky.models.PostMedia;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostAdapter
        extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    private final Context context;
    private final List<Post> postList;
    private final OnPostClickListener listener;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
            );

    public PostAdapter(
            Context context,
            List<Post> postList,
            OnPostClickListener listener
    ) {
        this.context = context;
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_post, parent, false);

        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PostViewHolder holder,
            int position
    ) {
        Post post = postList.get(position);

        // 1. Tiêu đề
        holder.tvTitle.setText(
                TextUtils.isEmpty(post.getTitle())
                        ? "Chưa có tiêu đề"
                        : post.getTitle()
        );

        // 2. Mô tả
        if (!TextUtils.isEmpty(post.getDescription())) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(post.getDescription());
        } else {
            holder.tvDescription.setVisibility(View.GONE);
            holder.tvDescription.setText("");
        }

        // 3. Địa điểm
        String location = TextUtils.isEmpty(post.getLocation())
                ? "Chưa xác định"
                : post.getLocation();

        holder.tvLocation.setText("📍 " + location);

        // 4. Nhãn loại đồ vật
        if ("LOST".equalsIgnoreCase(post.getCategory())) {
            holder.tvCategoryBadge.setText("MẤT ĐỒ");
            setBadgeStyle(
                    holder.tvCategoryBadge,
                    "#FEE2E2", "#B91C1C"
            );
        } else if ("FOUND".equalsIgnoreCase(post.getCategory())) {
            holder.tvCategoryBadge.setText("NHẶT ĐƯỢC");
            setBadgeStyle(
                    holder.tvCategoryBadge,
                    "#D1FAE5", "#047857"
            );
        } else {
            holder.tvCategoryBadge.setText("KHÁC");
            setBadgeStyle(
                    holder.tvCategoryBadge,
                    "#E2E8F0", "#475569"
            );
        }

        // 5. Trạng thái bài đăng
        if ("RESOLVED".equalsIgnoreCase(post.getStatus())) {
            holder.tvStatus.setText("ĐÃ TRAO TRẢ");
            setBadgeStyle(
                    holder.tvStatus,
                    "#D1FAE5", "#047857"
            );
        } else {
            holder.tvStatus.setText("ĐANG TÌM KIẾM");
            setBadgeStyle(
                    holder.tvStatus,
                    "#FEF3C7", "#92400E"
            );
        }

        // 6. Hiển thị tên và lớp
        if (post.isAnonymous()) {
            holder.tvAuthor.setText("Người dùng ẩn danh");
        } else {
            String author = TextUtils.isEmpty(post.getAuthorName())
                    ? "Học sinh"
                    : post.getAuthorName();

            if (!TextUtils.isEmpty(post.getClassId())) {
                author += " · Lớp " + post.getClassId();
            }

            holder.tvAuthor.setText(author);
        }

        // 7. Thời gian đăng
        if (post.getCreatedAt() != null) {
            holder.tvDate.setText(
                    dateFormat.format(post.getCreatedAtDate())
            );
        } else {
            holder.tvDate.setText("Chưa có thời gian");
        }

        // 8. Media bài đăng (Ảnh/Video grid)
        List<PostMedia> media = new ArrayList<>(post.getMedia());

        // Bài đăng cũ chỉ có imageUrl
        if (media.isEmpty() && !TextUtils.isEmpty(post.getImageUrl())) {
            media.add(new PostMedia(post.getImageUrl(), "IMAGE", ""));
        }

        Glide.with(holder.itemView).clear(holder.ivPostImage);
        holder.ivPostImage.setVisibility(View.GONE);
        holder.layoutPostMedia.removeAllViews();

        if (media.isEmpty()) {
            holder.layoutPostMedia.setVisibility(View.GONE);
        } else {
            holder.layoutPostMedia.setVisibility(View.VISIBLE);
            showMediaGrid(holder.layoutPostMedia, media, post);
        }

        // 9. Mở chi tiết bài đăng
        View.OnClickListener openDetail = v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        };

        holder.itemView.setOnClickListener(openDetail);
        holder.tvViewDetail.setOnClickListener(openDetail);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    private void showMediaGrid(
            LinearLayout container,
            List<PostMedia> media,
            Post post
    ) {
        container.removeAllViews();

        int visibleCount = Math.min(media.size(), 4);
        int index = 0;

        while (index < visibleCount) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);

            container.addView(row,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));

            int columns = Math.min(2, visibleCount - index);

            if (visibleCount == 1) columns = 1;

            for (int col = 0; col < columns; col++) {
                PostMedia item = media.get(index);

                FrameLayout cell = new FrameLayout(context);

                LinearLayout.LayoutParams cellParams =
                        new LinearLayout.LayoutParams(
                                0,
                                dp(visibleCount == 1 ? 270 : 160),
                                1f
                        );

                cellParams.setMargins(dp(1), dp(1), dp(1), dp(1));
                row.addView(cell, cellParams);

                if ("IMAGE".equals(item.getType())) {
                    ImageView image = new ImageView(context);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    cell.addView(image,
                            new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            ));

                    Glide.with(image)
                            .load(item.getUrl())
                            .centerCrop()
                            .into(image);

                } else if ("VIDEO".equals(item.getType())) {
                    ImageView videoIcon = new ImageView(context);

                    videoIcon.setImageResource(
                            android.R.drawable.ic_media_play
                    );
                    videoIcon.setPadding(
                            dp(45), dp(45), dp(45), dp(45)
                    );
                    videoIcon.setBackgroundColor(
                            Color.parseColor("#172033")
                    );

                    cell.addView(videoIcon,
                            new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            ));
                }

                // Ảnh cuối hiển thị số media còn lại
                if (index == 3 && media.size() > 4) {
                    TextView more = new TextView(context);
                    more.setText("+" + (media.size() - 4));
                    more.setTextSize(24);
                    more.setTextColor(Color.WHITE);
                    more.setGravity(android.view.Gravity.CENTER);
                    more.setBackgroundColor(0x99000000);

                    cell.addView(more,
                            new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            ));
                }

                cell.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPostClick(post);
                    }
                });

                index++;
            }
        }
    }

    // Hàm tạo badge bo góc
    private void setBadgeStyle(
            TextView textView,
            String backgroundColor,
            String textColor
    ) {
        GradientDrawable background = new GradientDrawable();

        background.setColor(
                Color.parseColor(backgroundColor)
        );

        float density = context.getResources()
                .getDisplayMetrics().density;

        background.setCornerRadius(100 * density);

        textView.setBackground(background);
        textView.setTextColor(
                Color.parseColor(textColor)
        );
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvCategoryBadge;
        TextView tvStatus;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvLocation;
        TextView tvAuthor;
        TextView tvDate;
        TextView tvViewDetail;

        ImageView ivPostImage;
        LinearLayout layoutPostMedia;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCategoryBadge =
                    itemView.findViewById(R.id.tvCategoryBadge);

            tvStatus =
                    itemView.findViewById(R.id.tvStatus);

            tvTitle =
                    itemView.findViewById(R.id.tvTitle);

            tvDescription =
                    itemView.findViewById(R.id.tvDescription);

            tvLocation =
                    itemView.findViewById(R.id.tvLocation);

            tvAuthor =
                    itemView.findViewById(R.id.tvAuthor);

            tvDate =
                    itemView.findViewById(R.id.tvDate);

            tvViewDetail =
                    itemView.findViewById(R.id.tvViewDetail);

            ivPostImage =
                    itemView.findViewById(R.id.ivPostImage);
            layoutPostMedia =
                    itemView.findViewById(R.id.layoutPostMedia);
        }
    }
}
