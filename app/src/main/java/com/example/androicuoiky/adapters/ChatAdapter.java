package com.example.androicuoiky.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androicuoiky.R;
import com.example.androicuoiky.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final Context context;
    private final List<ChatMessage> messageList;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter(Context context, List<ChatMessage> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        String formattedTime = message.getTimestamp() != null ? timeFormat.format(message.getTimestampDate()) : "";

        if (holder instanceof SentViewHolder) {
            SentViewHolder sentHolder = (SentViewHolder) holder;
            sentHolder.tvSentMessage.setText(message.getMessage());
            sentHolder.tvSentTime.setText(formattedTime);
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;
            receivedHolder.tvReceivedMessage.setText(message.getMessage());
            receivedHolder.tvReceivedTime.setText(formattedTime);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvSentMessage, tvSentTime;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentTime = itemView.findViewById(R.id.tvSentTime);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvReceivedMessage, tvReceivedTime;
        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);
        }
    }
}