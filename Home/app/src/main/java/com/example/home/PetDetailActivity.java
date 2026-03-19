package com.example.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PetDetailActivity extends AppCompatActivity {

    private static final String SUPABASE_URL_CHATS = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_CHATS;
    private static final String SUPABASE_API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    private ImageView imagePetDetail;
    private TextView textNameDetail, textBreedDetail, textAgeDetail, textGenderDetail;
    private TextView textPriceDetail, textDescriptionDetail, textAddress, textPhone, textCreatedDate;
    private ChipGroup chipGroup;
    private Button btnStartChat;
    private ImageView btnFavoriteDetail;
    private boolean isAdminViewing;

    private String petId, petName, petType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_detail);

        isAdminViewing = getIntent().getBooleanExtra("is_admin_viewing", false);
        initViews();


        btnStartChat = findViewById(R.id.btnStartChat);


        btnFavoriteDetail = findViewById(R.id.btnFavoriteDetail);

        displayPetDetails();
        setupRoleBasedUI();
    }
    private void setupRoleBasedUI() {
        if (isAdminViewing) {

            if (btnStartChat != null) {
                btnStartChat.setVisibility(View.GONE);
            }
            if (btnFavoriteDetail != null) {
                btnFavoriteDetail.setVisibility(View.GONE);
            }



        } else {

            if (btnStartChat != null) {
                btnStartChat.setVisibility(View.VISIBLE);
                btnStartChat.setOnClickListener(v -> {
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    String userId = prefs.getString("user_id", "");

                    if (userId.isEmpty()) {
                        Toast.makeText(this, "Для общения с менеджером войдите в аккаунт", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                    } else {
                        startChatForPet(petId, petName);
                    }
                });
            }

            if (btnFavoriteDetail != null) {
                btnFavoriteDetail.setVisibility(View.VISIBLE);

            }
        }
    }

    private void initViews() {
        imagePetDetail = findViewById(R.id.imagePetDetail);
        textNameDetail = findViewById(R.id.textNameDetail);
        textBreedDetail = findViewById(R.id.textBreedDetail);
        textAgeDetail = findViewById(R.id.textAgeDetail);
        textGenderDetail = findViewById(R.id.textGenderDetail);
        textPriceDetail = findViewById(R.id.textPriceDetail);
        textDescriptionDetail = findViewById(R.id.textDescriptionDetail);
        textAddress = findViewById(R.id.textAddress);
        textPhone = findViewById(R.id.textPhone);
        textCreatedDate = findViewById(R.id.textCreatedDate);
        chipGroup = findViewById(R.id.chipGroup);
    }

    private void displayPetDetails() {
        petName = getIntent().getStringExtra("pet_name");
        petType = getIntent().getStringExtra("pet_type");
        String petBreed = getIntent().getStringExtra("pet_breed");
        int petAge = getIntent().getIntExtra("pet_age", 0);
        String petGender = getIntent().getStringExtra("pet_gender");
        double petPrice = getIntent().getDoubleExtra("pet_price", 0);
        String petDescription = getIntent().getStringExtra("pet_description");
        String petImageUrl = getIntent().getStringExtra("pet_image_url");
        String petSize = getIntent().getStringExtra("pet_size");
        String petHairLength = getIntent().getStringExtra("pet_hair_length");
        String petColor = getIntent().getStringExtra("pet_color");
        String petAddress = getIntent().getStringExtra("pet_address");
        String petPhone = getIntent().getStringExtra("pet_phone");
        String petCreatedDate = getIntent().getStringExtra("pet_created_date");


        petId = getIntent().getStringExtra("pet_id");

        textNameDetail.setText(petName);
        textBreedDetail.setText(petBreed);
        textAgeDetail.setText(petAge + " месяцев");
        textGenderDetail.setText("male".equals(petGender) ? "Мальчик" : "Девочка");
        textPriceDetail.setText(petPrice + " руб.");
        textDescriptionDetail.setText(petDescription);
        textAddress.setText(petAddress);
        textPhone.setText(petPhone);
        textCreatedDate.setText(petCreatedDate);

        addChips(petType, petSize, petAge, petHairLength, petColor);

        if (petImageUrl != null && !petImageUrl.isEmpty()) {
            Log.d("МОИ_ЛОГИ", "Загружаем картинку по URL: " + petImageUrl);
            Picasso.get()
                    .load(petImageUrl)
                    .placeholder(R.drawable.placeholder_pet)
                    .error(R.drawable.placeholder_pet)
                    .into(imagePetDetail, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("МОИ_ЛОГИ", "Картинка загружена успешно");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.d("МОИ_ЛОГИ", "Ошибка загрузки картинки: " + e.getMessage());
                        }
                    });
        } else {
            Log.d("МОИ_ЛОГИ", "URL картинки пустой, ставим заглушку");
            imagePetDetail.setImageResource(R.drawable.placeholder_pet);
        }


        if (btnFavoriteDetail != null) {
            SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userId = userPrefs.getString("user_id", "");

            if (!userId.isEmpty()) {
                SharedPreferences favoritesPrefs = getSharedPreferences("FavoritesPrefs", MODE_PRIVATE);
                String favoritesKey = "favorite_ids_" + userId;
                Set<String> favorites = favoritesPrefs.getStringSet(favoritesKey, new HashSet<>());

                if (favorites.contains(petId)) {
                    btnFavoriteDetail.setImageResource(R.drawable.ic_favorite);
                } else {
                    btnFavoriteDetail.setImageResource(R.drawable.ic_favorite_border);
                }
            } else {
                btnFavoriteDetail.setImageResource(R.drawable.ic_favorite_border);
            }


            btnFavoriteDetail.setOnClickListener(v -> {
                toggleFavoriteInDetail();
            });
        }


        btnStartChat.setOnClickListener(v -> {

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userId = prefs.getString("user_id", "");

            if (userId.isEmpty()) {
                Toast.makeText(this, "Для общения с менеджером войдите в аккаунт", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                startChatForPet(petId, petName);
            }
        });
    }


    private void toggleFavoriteInDetail() {
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", "");

        SharedPreferences favoritesPrefs = getSharedPreferences("FavoritesPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = favoritesPrefs.edit();


        String favoritesKey = "favorite_ids_" + userId;

        Set<String> favorites = favoritesPrefs.getStringSet(favoritesKey, new HashSet<>());
        Set<String> updatedFavorites = new HashSet<>(favorites);

        if (updatedFavorites.contains(petId)) {
            updatedFavorites.remove(petId);
            btnFavoriteDetail.setImageResource(R.drawable.ic_favorite_border);
            Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
        } else {
            updatedFavorites.add(petId);
            btnFavoriteDetail.setImageResource(R.drawable.ic_favorite);
            Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
        }

        editor.putStringSet(favoritesKey, updatedFavorites);
        editor.apply();
    }


    private void startChatForPet(String petId, String petName) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        String userRole = prefs.getString("user_role", "Client");

        if ("Manager".equals(userRole)) {
            Toast.makeText(this, "Менеджеры не могут создавать чаты", Toast.LENGTH_SHORT).show();
            return;
        }


        String managerId;
        if (petType != null && "dog".equals(petType)) {
            managerId = "bab5ce91-d235-4672-a851-597c1fce679b"; // ID менеджера для собак
        } else {
            managerId = "46d51d9c-b480-4716-84c8-e4025026963b"; // ID менеджера для кошек
        }


        createOrOpenChat(petId, petName, userId, managerId);
    }


    private void createOrOpenChat(String petId, String petName, String clientId, String managerId) {

        OkHttpClient client = new OkHttpClient();
        String url = SUPABASE_URL_CHATS + "?pet_id=eq." + petId + "&client_id=eq." + clientId;

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
                    Toast.makeText(PetDetailActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                try {
                    JSONArray chatsArray = new JSONArray(responseBody);

                    runOnUiThread(() -> {
                        if (chatsArray.length() > 0) {

                            try {
                                JSONObject chat = chatsArray.getJSONObject(0);
                                openExistingChat(chat.getString("id"), petId, petName, clientId, managerId);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(PetDetailActivity.this, "Ошибка открытия чата", Toast.LENGTH_SHORT).show();
                            }
                        } else {

                            createNewChat(petId, petName, clientId, managerId);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(PetDetailActivity.this, "Ошибка данных", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }


    private void openExistingChat(String chatId, String petId, String petName, String clientId, String managerId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("pet_id", petId);
        intent.putExtra("pet_name", petName);
        intent.putExtra("client_id", clientId);
        intent.putExtra("manager_id", managerId);
        startActivity(intent);
    }


    private void createNewChat(String petId, String petName, String clientId, String managerId) {
        Log.d("МОИ_ЛОГИ", "Создание чата для питомца: " + petName);
        JSONObject chatData = new JSONObject();
        try {
            chatData.put("pet_id", petId);
            chatData.put("client_id", clientId);
            chatData.put("manager_id", managerId);
            chatData.put("last_message", "Чат начат");
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка создания чата", Toast.LENGTH_SHORT).show();
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
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(PetDetailActivity.this, "Ошибка создания чата", Toast.LENGTH_SHORT).show();
                    Log.e("CHAT_CREATE", "Network error", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "empty body";
                Log.d("МОИ_ЛОГИ", "Ответ сервера: код " + response.code());
                Log.d("CHAT_CREATE", "Response code: " + response.code());
                Log.d("CHAT_CREATE", "Response body: " + responseBody);

                if (response.isSuccessful()) {
                    try {

                        JSONArray resultArray = new JSONArray(responseBody);
                        if (resultArray.length() > 0) {
                            JSONObject createdChat = resultArray.getJSONObject(0);
                            String createdChatId = createdChat.getString("id");
                            Log.d("МОИ_ЛОГИ", "Чат создан с ID: " + createdChatId);

                            runOnUiThread(() -> {
                                Toast.makeText(PetDetailActivity.this, "Чат создан", Toast.LENGTH_SHORT).show();

                                openExistingChat(createdChatId, petId, petName, clientId, managerId);
                            });
                        } else {
                            throw new JSONException("Empty response array");
                        }

                    } catch (JSONException e) {
                        Log.e("CHAT_CREATE", "JSON parsing error", e);
                        runOnUiThread(() -> {
                            Toast.makeText(PetDetailActivity.this, "Ошибка обработки ответа", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(PetDetailActivity.this, "Ошибка создания чата: " + response.code(), Toast.LENGTH_SHORT).show();
                        Log.e("CHAT_CREATE", "Error response: " + responseBody);
                    });
                }
            }
        });
    }

    private void addChips(String type, String size, int age, String hairLength, String color) {
        if (type != null && !type.isEmpty()) {
            addChip("dog".equals(type) ? "Собака" : "Кошка", "#4CAF50");
        }
        if (size != null && !size.isEmpty()) {
            addChip(size, "#2196F3");
        }
        String ageText = age + " месяцев";
        addChip(ageText, "#FF9800");
        if (hairLength != null && !hairLength.isEmpty()) {
            addChip(hairLength, "#9C27B0");
        }
        if (color != null && !color.isEmpty()) {
            addChip(color, "#F44336");
        }
    }

    private void addChip(String text, String color) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setTextColor(android.graphics.Color.parseColor(color));
        chip.setChipStrokeColorResource(android.R.color.darker_gray);
        chip.setChipStrokeWidth(2f);
        chip.setClickable(false);
        chip.setFocusable(false);
        chipGroup.addView(chip);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }
}