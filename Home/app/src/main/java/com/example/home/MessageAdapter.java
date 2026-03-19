package com.example.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int TYPE_MY_MESSAGE = 1;
    private static final int TYPE_OTHER_MESSAGE = 2;

    private List<Message> messageList;
    private String currentUserId;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_MY_MESSAGE) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_my, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_other, parent, false);
        }
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.textMessage.setText(message.getMessage());
        holder.textTime.setText(formatTime(message.getCreatedAt()));
    }

    private String formatTime(String timeString) {
        try {

            if (timeString.contains(".")) {
                timeString = timeString.substring(0, timeString.indexOf("."));
            }


            if (timeString.contains("+")) {
                timeString = timeString.substring(0, timeString.indexOf("+"));
            }


            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());


            Date date = inputFormat.parse(timeString);
            return outputFormat.format(date);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.isMyMessage() ? TYPE_MY_MESSAGE : TYPE_OTHER_MESSAGE;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;

        public MessageViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == TYPE_MY_MESSAGE) {
                textMessage = itemView.findViewById(R.id.textMyMessage);
                textTime = itemView.findViewById(R.id.textMyTime);
            } else {
                textMessage = itemView.findViewById(R.id.textOtherMessage);
                textTime = itemView.findViewById(R.id.textOtherTime);
            }
        }
    }
}