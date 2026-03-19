package com.example.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private static final String SUPABASE_URL_MESSAGES = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_MESSAGES;
    private static final String SUPABASE_URL_CHATS = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_CHATS;
    private static final String SUPABASE_API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private TextView textChatTitle;

    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter adapter;
    private String chatId, petId, petName, currentUserId;
    private String clientId, clientName, managerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        Intent intent = getIntent();
        chatId = intent.getStringExtra("chat_id");
        petId = intent.getStringExtra("pet_id");
        petName = intent.getStringExtra("pet_name");
        clientId = intent.getStringExtra("client_id");
        clientName = intent.getStringExtra("client_name");
        managerId = intent.getStringExtra("manager_id");


        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");


        recyclerView = findViewById(R.id.recyclerViewMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        textChatTitle = findViewById(R.id.textChatTitle);


        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(messageList, currentUserId);
        recyclerView.setAdapter(adapter);


        textChatTitle.setText("Чат о " + petName);


        loadMessages();


        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        OkHttpClient client = new OkHttpClient();
        String url = SUPABASE_URL_MESSAGES + "?chat_id=eq." + chatId + "&order=created_at.asc";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
                    Log.e("CHAT", "Connection error", e);
                });
            }


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    try {
                        JSONArray messagesArray = new JSONArray(responseBody);
                        messageList.clear();

                        for (int i = 0; i < messagesArray.length(); i++) {
                            JSONObject messageJson = messagesArray.getJSONObject(i);

                            Message message = new Message(
                                    messageJson.getString("id"),
                                    messageJson.getString("chat_id"),
                                    messageJson.getString("sender_id"),
                                    "Отправитель", // Имя можно получить отдельным запросом
                                    messageJson.getString("message"),
                                    messageJson.getString("created_at"),
                                    messageJson.getString("sender_id").equals(currentUserId)
                            );
                            messageList.add(message);
                        }

                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            if (messageList.size() > 0) {
                                recyclerView.scrollToPosition(messageList.size() - 1);
                            }
                        });

                    } catch (JSONException e) {
                        Log.e("CHAT", "JSON error", e);
                    }
                }
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show();
            return;
        }


        if (chatId == null || chatId.isEmpty() || "new".equals(chatId)) {
            Toast.makeText(this, "Ошибка: чат не создан", Toast.LENGTH_SHORT).show();
            return;
        }


        JSONObject messageData = new JSONObject();
        try {
            messageData.put("chat_id", chatId);
            messageData.put("sender_id", currentUserId);
            messageData.put("message", messageText);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка создания сообщения", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                messageData.toString(),
                MediaType.get("application/json; charset=utf-8")
        );


        Request request = new Request.Builder()
                .url(SUPABASE_URL_MESSAGES)
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Ошибка отправки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("CHAT_SEND", "Send failed", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "empty body";

                Log.d("CHAT_SEND", "Response code: " + response.code());
                Log.d("CHAT_SEND", "Response body: " + responseBody);

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {

                        etMessage.setText("");


                        updateLastMessage(messageText);


                        loadMessages();

                        Toast.makeText(ChatActivity.this, "Сообщение отправлено", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMsg = "Ошибка " + response.code() + ": " + responseBody;
                        Toast.makeText(ChatActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        Log.e("CHAT_SEND", errorMsg);
                    }
                });
            }
        });
    }

    private void updateLastMessage(String lastMessage) {
        JSONObject updateData = new JSONObject();
        try {
            updateData.put("last_message", lastMessage);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                updateData.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        String url = SUPABASE_URL_CHATS + "?id=eq." + chatId;

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CHAT", "Failed to update last message", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Log.d("CHAT", "Last message updated: " + response.code());
            }
        });
    }
}
