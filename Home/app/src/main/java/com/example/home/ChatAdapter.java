package com.example.home;

import android.content.Intent;
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

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Chat> chatList;
    private ChatListActivity context;
    private String userRole;

    public ChatAdapter(List<Chat> chatList, ChatListActivity context, String userRole) {
        this.chatList = chatList;
        this.context = context;
        this.userRole = userRole;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);


        String formattedTime = formatTime(chat.getLastMessageTime());


        holder.textPetName.setText(chat.getPetName());

        if ("Manager".equals(userRole)) {
            holder.textUserName.setText("Клиент: " + chat.getClientName());
        } else {
            holder.textUserName.setText("Менеджер");
        }

        holder.textLastMessage.setText(chat.getLastMessage());
        holder.textTime.setText(formattedTime);


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("chat_id", chat.getId());
            intent.putExtra("pet_id", chat.getPetId());
            intent.putExtra("pet_name", chat.getPetName());

            if ("Manager".equals(userRole)) {
                intent.putExtra("client_id", chat.getClientId());
                intent.putExtra("client_name", chat.getClientName());
            } else {
                intent.putExtra("manager_id", chat.getManagerId());
            }

            context.startActivity(intent);
        });
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
            return timeString;
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textPetName, textUserName, textLastMessage, textTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textPetName = itemView.findViewById(R.id.textPetName);
            textUserName = itemView.findViewById(R.id.textUserName);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}