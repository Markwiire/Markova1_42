package com.example.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_USERS;
    private static final String SUPABASE_API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    private EditText etNewUsername;
    private Button btnSave;
    private CardView btnCancel;
    private String currentUserId, currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String role = prefs.getString("user_role", "Client");

        if ("Manager".equals(role)) {

            Toast.makeText(this, "Менеджеры не могут редактировать профиль", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        setContentView(R.layout.activity_edit_profile);


        etNewUsername = findViewById(R.id.etNewUsername);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);


        loadCurrentUserData();

        btnSave.setOnClickListener(v -> saveProfileChanges());
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }


    private void loadCurrentUserData() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");
        currentUsername = prefs.getString("username", "");


        etNewUsername.setText(currentUsername);
        etNewUsername.setSelection(currentUsername.length());
    }

    private void saveProfileChanges() {
        String newUsername = etNewUsername.getText().toString().trim();


        if (newUsername.isEmpty()) {
            etNewUsername.setError("Введите имя пользователя");
            return;
        }

        if (newUsername.equals(currentUsername)) {
            Toast.makeText(this, "Имя не изменилось", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }


        checkUsernameUnique(newUsername);
    }

    private void checkUsernameUnique(String newUsername) {
        OkHttpClient client = new OkHttpClient();
        String url = SUPABASE_URL + "?username=eq." + newUsername + "&select=id";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(EditProfileActivity.this, "Ошибка проверки логина", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    try {
                        JSONArray users = new JSONArray(responseBody);

                        if (users.length() > 0) {
                            runOnUiThread(() ->
                                    Toast.makeText(EditProfileActivity.this, "Это имя пользователя уже занято", Toast.LENGTH_SHORT).show());
                        } else {
                            updateUsername(newUsername);
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() ->
                                Toast.makeText(EditProfileActivity.this, "Ошибка данных", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void updateUsername(String newUsername) {
        OkHttpClient client = new OkHttpClient();

        JSONObject updateData = new JSONObject();
        try {
            updateData.put("username", newUsername);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(
                updateData.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        String url = SUPABASE_URL + "?id=eq." + currentUserId;

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
                runOnUiThread(() ->
                        Toast.makeText(EditProfileActivity.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {

                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("username", newUsername);
                    editor.apply();

                    runOnUiThread(() -> {
                        Toast.makeText(EditProfileActivity.this, "Профиль обновлен", Toast.LENGTH_SHORT).show();


                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("new_username", newUsername);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(EditProfileActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        setResult(RESULT_CANCELED);
        finish();
        return true;
    }
}