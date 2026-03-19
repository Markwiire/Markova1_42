package com.example.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatListActivity extends AppCompatActivity {

    private static final String SUPABASE_URL_CHATS = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_CHATS;
    private static final String SUPABASE_API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    private RecyclerView recyclerView;
    private ImageButton btnBack;
    private List<Chat> chatList = new ArrayList<>();
    private ChatAdapter adapter;
    private String currentUserId, currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);


        recyclerView = findViewById(R.id.recyclerViewChats);
        btnBack = findViewById(R.id.btnBack);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");
        currentUserRole = prefs.getString("user_role", "Client");


        setupBackButton();


        loadChats();
    }

    private void navigateBack(boolean closeActivity) {
        if ("Manager".equals(currentUserRole)) {
            Intent intent = new Intent(ChatListActivity.this, ManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else {
            Intent intent = new Intent(ChatListActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }

        if (closeActivity) {
            finish();
        }
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> navigateBack(true));
    }

    @Override
    public void onBackPressed() {
        navigateBack(false);
        super.onBackPressed();
    }

    private void loadChats() {
        OkHttpClient client = new OkHttpClient();
        String url;

        if ("Manager".equals(currentUserRole)) {

            url = "https://rifzmuphaemtlmrijaqr.supabase.co/rest/v1/chats" +
                    "?manager_id=eq." + currentUserId +
                    "&select=*,pets(name),users!chats_client_id_fkey(username)";
        } else {

            url = "https://rifzmuphaemtlmrijaqr.supabase.co/rest/v1/chats" +
                    "?client_id=eq." + currentUserId +
                    "&select=*,pets(name),users!chats_manager_id_fkey(username)";
        }

        Log.d("CHAT_LOAD", "Loading chats from: " + url);

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
                    Toast.makeText(ChatListActivity.this, "Ошибка загрузки чатов", Toast.LENGTH_SHORT).show();
                    Log.e("CHAT_LOAD", "Connection error", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "empty body";
                Log.d("CHAT_LOAD", "Response: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONArray chatsArray = new JSONArray(responseBody);
                        chatList.clear();

                        for (int i = 0; i < chatsArray.length(); i++) {
                            JSONObject chatJson = chatsArray.getJSONObject(i);


                            String petName = "Питомец";
                            if (chatJson.has("pets") && !chatJson.isNull("pets")) {
                                JSONObject petObj = chatJson.getJSONObject("pets");
                                if (petObj.has("name")) {
                                    petName = petObj.getString("name");
                                }
                            }


                            String otherUserLogin = "Manager".equals(currentUserRole) ? "Клиент" : "Менеджер";
                            if (chatJson.has("users") && !chatJson.isNull("users")) {
                                JSONObject userObj = chatJson.getJSONObject("users");
                                if (userObj.has("username")) {
                                    otherUserLogin = userObj.getString("username");
                                }
                            }


                            String displayClientName, displayManagerName;

                            if ("Manager".equals(currentUserRole)) {

                                displayClientName = otherUserLogin;
                                displayManagerName = "Вы";
                            } else {

                                displayClientName = "Вы";
                                displayManagerName = otherUserLogin;
                            }

                            Chat chat = new Chat(
                                    chatJson.getString("id"),
                                    chatJson.getString("pet_id"),
                                    petName,
                                    chatJson.getString("client_id"),
                                    displayClientName,
                                    chatJson.getString("manager_id"),
                                    displayManagerName,
                                    chatJson.getString("last_message"),
                                    chatJson.getString("last_message_time")
                            );
                            chatList.add(chat);
                        }

                        runOnUiThread(() -> {
                            adapter = new ChatAdapter(chatList, ChatListActivity.this, currentUserRole);
                            recyclerView.setAdapter(adapter);
                            if (chatList.isEmpty()) {
                                Toast.makeText(ChatListActivity.this, "У вас пока нет чатов", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } catch (JSONException e) {
                        Log.e("CHAT_LOAD", "JSON error", e);
                        runOnUiThread(() -> {
                            Toast.makeText(ChatListActivity.this, "Ошибка данных", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatListActivity.this, "Ошибка загрузки: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }


    private void loadPetNameForChat(JSONObject chatJson, int index, int total) {
        try {
            String petId = chatJson.getString("pet_id");

            OkHttpClient client = new OkHttpClient();
            String url = "https://rifzmuphaemtlmrijaqr.supabase.co/rest/v1/pets" +
                    "?id=eq." + petId + "&select=name";

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("PET_NAME", "Failed to load pet name for chat " + index, e);
                    addChatWithDefaultName(chatJson, index, total);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body() != null ? response.body().string() : "empty body";
                            JSONArray petsArray = new JSONArray(responseBody);

                            String petName;
                            if (petsArray.length() > 0) {
                                petName = petsArray.getJSONObject(0).getString("name");
                            } else {
                                petName = "Питомец";
                            }

                            addChatWithRealName(chatJson, petName, index, total);

                        } catch (JSONException e) {
                            Log.e("PET_NAME", "JSON error", e);
                            addChatWithDefaultName(chatJson, index, total);
                        }
                    } else {
                        addChatWithDefaultName(chatJson, index, total);
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("PET_NAME", "Error getting petId", e);
            addChatWithDefaultName(chatJson, index, total);
        }
    }


    private void addChatWithRealName(JSONObject chatJson, String petName, int index, int total) {
        try {
            String otherUserName = "Manager".equals(currentUserRole) ? "Клиент" : "Менеджер";

            Chat chat = new Chat(
                    chatJson.getString("id"),
                    chatJson.getString("pet_id"),
                    petName,
                    chatJson.getString("client_id"),
                    "Client".equals(currentUserRole) ? "Вы" : otherUserName,
                    chatJson.getString("manager_id"),
                    "Manager".equals(currentUserRole) ? "Вы" : otherUserName,
                    chatJson.getString("last_message"),
                    chatJson.getString("last_message_time")
            );

            chatList.add(chat);


            if (index == total - 1) {
                runOnUiThread(() -> {
                    adapter = new ChatAdapter(chatList, ChatListActivity.this, currentUserRole);
                    recyclerView.setAdapter(adapter);
                    Toast.makeText(ChatListActivity.this, "Загружено чатов: " + chatList.size(), Toast.LENGTH_SHORT).show();
                });
            }

        } catch (JSONException e) {
            Log.e("CHAT_ADD", "Error adding chat with real name", e);
            addChatWithDefaultName(chatJson, index, total);
        }
    }


    private void addChatWithDefaultName(JSONObject chatJson, int index, int total) {
        try {
            String otherUserName = "Manager".equals(currentUserRole) ? "Клиент" : "Менеджер";

            Chat chat = new Chat(
                    chatJson.getString("id"),
                    chatJson.getString("pet_id"),
                    "Питомец",
                    chatJson.getString("client_id"),
                    "Client".equals(currentUserRole) ? "Вы" : otherUserName,
                    chatJson.getString("manager_id"),
                    "Manager".equals(currentUserRole) ? "Вы" : otherUserName,
                    chatJson.getString("last_message"),
                    chatJson.getString("last_message_time")
            );

            chatList.add(chat);


            if (index == total - 1) {
                runOnUiThread(() -> {
                    adapter = new ChatAdapter(chatList, ChatListActivity.this, currentUserRole);
                    recyclerView.setAdapter(adapter);
                });
            }

        } catch (JSONException e) {
            Log.e("CHAT_ADD", "Error adding chat with default name", e);
        }
    }
}