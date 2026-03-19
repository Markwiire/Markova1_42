package com.example.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import okhttp3.*;

public class AddPetActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_PETS;
    private static final String SUPABASE_API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    private EditText etAddPetName, etAddPetBreed, etAddPetAge, etAddPetPrice, etAddPetDescription;
    private EditText etAddPetColor, etAddPetAddress, etAddPetPhone, etAddPetImage;
    private Spinner spinnerAddPetType, spinnerAddPetGender, spinnerAddPetSize, spinnerAddHairLength;
    private Button btnSubmitAddPet;
    private CardView btnBackToAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userRole = prefs.getString("user_role", "Client");
        String username = prefs.getString("username", "");

        Log.d("access_check", "Попытка открыть AddPetActivity");
        Log.d("access_check", "Пользователь: " + username);
        Log.d("access_check", "Роль: " + userRole);

        if (!"Manager".equals(userRole)) {
            Log.w("access_check", "ДОСТУП ЗАПРЕЩЕН! Требуется роль Manager, а у пользователя " + userRole);
            Toast.makeText(this, "Доступ запрещен. Только для менеджеров.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d("access_check", "Доступ разрешен");




        setContentView(R.layout.activity_add_pet);

        initViews();
        setupSpinners();
        setupSubmitButton();
        setupBackButton();
    }

    private void initViews() {
        etAddPetName = findViewById(R.id.etAddPetName);
        etAddPetBreed = findViewById(R.id.etAddPetBreed);
        etAddPetAge = findViewById(R.id.etAddPetAge);
        etAddPetPrice = findViewById(R.id.etAddPetPrice);
        etAddPetDescription = findViewById(R.id.etAddPetDescription);
        etAddPetColor = findViewById(R.id.etAddPetColor);
        etAddPetAddress = findViewById(R.id.etAddPetAddress);
        etAddPetPhone = findViewById(R.id.etAddPetPhone);
        etAddPetImage = findViewById(R.id.etAddPetImage);

        spinnerAddPetType = findViewById(R.id.spinnerAddPetType);
        spinnerAddPetGender = findViewById(R.id.spinnerAddPetGender);
        spinnerAddPetSize = findViewById(R.id.spinnerAddPetSize);
        spinnerAddHairLength = findViewById(R.id.spinnerAddHairLength);

        btnSubmitAddPet = findViewById(R.id.btnSubmitAddPet);
        btnBackToAdmin = findViewById(R.id.btnBackToAdmin);
    }

    private void setupSpinners() {

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.pet_types,
                android.R.layout.simple_spinner_item
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAddPetType.setAdapter(typeAdapter);


        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.pet_genders,
                android.R.layout.simple_spinner_item
        );
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAddPetGender.setAdapter(genderAdapter);


        ArrayAdapter<CharSequence> sizeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.pet_sizes,
                android.R.layout.simple_spinner_item
        );
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAddPetSize.setAdapter(sizeAdapter);


        ArrayAdapter<CharSequence> hairAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.hair_lengths,
                android.R.layout.simple_spinner_item
        );
        hairAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAddHairLength.setAdapter(hairAdapter);
    }

    private void setupSubmitButton() {
        btnSubmitAddPet.setOnClickListener(v -> addPetToDatabase());
    }

    private void setupBackButton() {
        btnBackToAdmin.setOnClickListener(v -> {

            finish();
        });
    }

    private void addPetToDatabase() {


        String name = etAddPetName.getText().toString().trim();
        String type = spinnerAddPetType.getSelectedItem().toString();
        String breed = etAddPetBreed.getText().toString().trim();
        String ageStr = etAddPetAge.getText().toString().trim();
        String gender = spinnerAddPetGender.getSelectedItem().toString();
        String size = spinnerAddPetSize.getSelectedItem().toString();
        String hairLength = spinnerAddHairLength.getSelectedItem().toString();
        String color = etAddPetColor.getText().toString().trim();
        String priceStr = etAddPetPrice.getText().toString().trim();
        String address = etAddPetAddress.getText().toString().trim();
        String phone = etAddPetPhone.getText().toString().trim();

        String description = etAddPetDescription.getText().toString().trim();


        if (name.isEmpty() || breed.isEmpty() || ageStr.isEmpty() || priceStr.isEmpty() ||
                color.isEmpty() || address.isEmpty() || phone.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(ageStr);
            double price = Double.parseDouble(priceStr);


            String dbType = "dog";
            if ("Кошка".equals(type)) {
                dbType = "cat";
            }

            String dbGender = "male";
            if ("Девочка".equals(gender)) {
                dbGender = "female";
            }


            String imageUrl = etAddPetImage.getText().toString().trim();


            JSONObject petData = new JSONObject();
            petData.put("name", name);
            petData.put("type", dbType);
            petData.put("breed", breed);
            petData.put("age", age);
            petData.put("gender", dbGender);
            petData.put("size", size);
            petData.put("hair_length", hairLength);
            petData.put("color", color);
            petData.put("price", price);
            petData.put("address", address);
            petData.put("phone", phone);
            petData.put("image_url", imageUrl);
            petData.put("description", description);
            petData.put("created_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));


            sendPetToSupabase(petData);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Возраст и цена должны быть числами", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "Ошибка создания данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendPetToSupabase(JSONObject petData) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                petData.toString(),
                MediaType.get("application/json; charset=utf-8")
        );


        android.util.Log.d("ADD_PET_DEBUG", "Отправляемые данные: " + petData.toString());

        Request request = new Request.Builder()
                .url(SUPABASE_URL)
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
                    Toast.makeText(AddPetActivity.this, "Ошибка соединения: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    android.util.Log.e("ADD_PET_DEBUG", "Ошибка сети", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "empty body";

                android.util.Log.d("ADD_PET_DEBUG", "Код ответа: " + response.code());
                android.util.Log.d("ADD_PET_DEBUG", "Тело ответа: " + responseBody);

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddPetActivity.this, "Питомец успешно добавлен!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String errorMsg = "Ошибка " + response.code() + ": " + responseBody;
                        Toast.makeText(AddPetActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        android.util.Log.e("ADD_PET_DEBUG", errorMsg);
                    }
                });
            }
        });
    }
}