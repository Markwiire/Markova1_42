package com.example.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatUtils {

    private static final String SUPABASE_URL_CHATS = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_CHATS;
    private static final String SUPABASE_API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    public static void createNewChat(Context context, String petId, String petName,
                                     String clientId, String managerId) {

        JSONObject chatData = new JSONObject();
        try {
            chatData.put("pet_id", petId);
            chatData.put("client_id", clientId);
            chatData.put("manager_id", managerId);
            chatData.put("last_message", "Чат начат");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                chatData.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(SUPABASE_URL_CHATS)
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Ошибка создания чата", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Чат создан", Toast.LENGTH_SHORT).show();

                    });
                }
            }
        });
    }
}
