package com.example.home;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class EditPetActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_PETS;
    private static final String SUPABASE_API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    private EditText etPetName, etPetBreed, etPetAge, etPetPrice, etPetDescription;
    private EditText etPetColor, etPetAddress, etPetPhone, etPetImage;
    private Spinner spinnerPetType, spinnerPetGender, spinnerPetSize, spinnerHairLength;
    private Button btnUpdatePet;
    private androidx.cardview.widget.CardView btnBackToAdmin;

    private String petId;
    private String originalName, originalBreed, originalAge, originalPrice, originalDescription;
    private String originalColor, originalAddress, originalPhone, originalImage;
    private String originalType, originalGender, originalSize, originalHairLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pet);

        petId = getIntent().getStringExtra("pet_id");
        if (petId == null) {
            Toast.makeText(this, "Ошибка: не получен ID питомца", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupSpinners();
        loadPetData();
    }

    private void initViews() {
        etPetName = findViewById(R.id.etEditPetName);
        etPetBreed = findViewById(R.id.etEditPetBreed);
        etPetAge = findViewById(R.id.etEditPetAge);
        etPetPrice = findViewById(R.id.etEditPetPrice);
        etPetDescription = findViewById(R.id.etEditPetDescription);
        etPetColor = findViewById(R.id.etEditPetColor);
        etPetAddress = findViewById(R.id.etEditPetAddress);
        etPetPhone = findViewById(R.id.etEditPetPhone);
        etPetImage = findViewById(R.id.etEditPetImage);

        spinnerPetType = findViewById(R.id.spinnerEditPetType);
        spinnerPetGender = findViewById(R.id.spinnerEditPetGender);
        spinnerPetSize = findViewById(R.id.spinnerEditPetSize);
        spinnerHairLength = findViewById(R.id.spinnerEditHairLength);

        btnUpdatePet = findViewById(R.id.btnSubmitEditPet);
        btnBackToAdmin = findViewById(R.id.btnBackToAdmin);

        btnUpdatePet.setOnClickListener(v -> updatePet());
        btnBackToAdmin.setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.pet_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPetType.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this, R.array.pet_genders, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPetGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> sizeAdapter = ArrayAdapter.createFromResource(
                this, R.array.pet_sizes, android.R.layout.simple_spinner_item);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPetSize.setAdapter(sizeAdapter);

        ArrayAdapter<CharSequence> hairAdapter = ArrayAdapter.createFromResource(
                this, R.array.hair_lengths, android.R.layout.simple_spinner_item);
        hairAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHairLength.setAdapter(hairAdapter);
    }

    private void loadPetData() {
        OkHttpClient client = new OkHttpClient();
        String url = SUPABASE_URL + "?id=eq." + petId;

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
                    Toast.makeText(EditPetActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        org.json.JSONArray petsArray = new org.json.JSONArray(responseBody);
                        if (petsArray.length() > 0) {
                            org.json.JSONObject pet = petsArray.getJSONObject(0);
                            runOnUiThread(() -> fillFormWithPetData(pet));
                        }
                    } catch (org.json.JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void fillFormWithPetData(org.json.JSONObject pet) {
        try {
            originalName = pet.getString("name");
            originalBreed = pet.getString("breed");
            originalAge = String.valueOf(pet.getInt("age"));
            originalPrice = String.valueOf(pet.getDouble("price"));
            originalDescription = pet.getString("description");
            originalColor = pet.getString("color");
            originalAddress = pet.getString("address");
            originalPhone = pet.getString("phone");
            originalImage = pet.getString("image_url");
            originalType = pet.getString("type");
            originalGender = pet.getString("gender");
            originalSize = pet.getString("size");
            originalHairLength = pet.getString("hair_length");

            etPetName.setText(originalName);
            etPetBreed.setText(originalBreed);
            etPetAge.setText(originalAge);
            etPetPrice.setText(originalPrice);
            etPetDescription.setText(originalDescription);
            etPetColor.setText(originalColor);
            etPetAddress.setText(originalAddress);
            etPetPhone.setText(originalPhone);
            etPetImage.setText(originalImage);

            spinnerPetType.setSelection("dog".equals(originalType) ? 0 : 1);
            spinnerPetGender.setSelection("male".equals(originalGender) ? 0 : 1);
            setSpinnerByValue(spinnerPetSize, originalSize);
            setSpinnerByValue(spinnerHairLength, originalHairLength);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
        }
    }

    private void setSpinnerByValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private boolean hasDataChanged() {
        String currentName = etPetName.getText().toString().trim();
        String currentBreed = etPetBreed.getText().toString().trim();
        String currentAge = etPetAge.getText().toString().trim();
        String currentPrice = etPetPrice.getText().toString().trim();
        String currentDescription = etPetDescription.getText().toString().trim();
        String currentColor = etPetColor.getText().toString().trim();
        String currentAddress = etPetAddress.getText().toString().trim();
        String currentPhone = etPetPhone.getText().toString().trim();
        String currentImage = etPetImage.getText().toString().trim();

        String currentType = "Собака".equals(spinnerPetType.getSelectedItem().toString()) ? "dog" : "cat";
        String currentGender = "Мальчик".equals(spinnerPetGender.getSelectedItem().toString()) ? "male" : "female";
        String currentSize = spinnerPetSize.getSelectedItem().toString();
        String currentHairLength = spinnerHairLength.getSelectedItem().toString();

        return !currentName.equals(originalName) ||
                !currentBreed.equals(originalBreed) ||
                !currentAge.equals(originalAge) ||
                !currentPrice.equals(originalPrice) ||
                !currentDescription.equals(originalDescription) ||
                !currentColor.equals(originalColor) ||
                !currentAddress.equals(originalAddress) ||
                !currentPhone.equals(originalPhone) ||
                !currentImage.equals(originalImage) ||
                !currentType.equals(originalType) ||
                !currentGender.equals(originalGender) ||
                !currentSize.equals(originalSize) ||
                !currentHairLength.equals(originalHairLength);
    }

    private void updatePet() {
        if (!hasDataChanged()) {
            Toast.makeText(this, "Данные не были изменены", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etPetName.getText().toString().trim();
        String breed = etPetBreed.getText().toString().trim();
        String ageStr = etPetAge.getText().toString().trim();
        String priceStr = etPetPrice.getText().toString().trim();
        String color = etPetColor.getText().toString().trim();
        String address = etPetAddress.getText().toString().trim();
        String phone = etPetPhone.getText().toString().trim();
        String description = etPetDescription.getText().toString().trim();

        if (name.isEmpty() || breed.isEmpty() || ageStr.isEmpty() || priceStr.isEmpty() ||
                color.isEmpty() || address.isEmpty() || phone.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(ageStr);
            double price = Double.parseDouble(priceStr);

            JSONObject updateData = new JSONObject();
            updateData.put("name", name);
            updateData.put("type", "Собака".equals(spinnerPetType.getSelectedItem().toString()) ? "dog" : "cat");
            updateData.put("breed", breed);
            updateData.put("age", age);
            updateData.put("gender", "Мальчик".equals(spinnerPetGender.getSelectedItem().toString()) ? "male" : "female");
            updateData.put("size", spinnerPetSize.getSelectedItem().toString());
            updateData.put("hair_length", spinnerHairLength.getSelectedItem().toString());
            updateData.put("color", color);
            updateData.put("price", price);
            updateData.put("address", address);
            updateData.put("phone", phone);
            updateData.put("image_url", etPetImage.getText().toString().trim());
            updateData.put("description", description);

            sendUpdateToServer(updateData);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Возраст и цена должны быть числами", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "Ошибка создания данных", Toast.LENGTH_SHORT).show();
            Log.e("EDIT_PET", "JSON error", e);
        }
    }

    private void sendUpdateToServer(JSONObject updateData) {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(
                updateData.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        String url = SUPABASE_URL + "?id=eq." + petId;

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
                        Toast.makeText(EditPetActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(EditPetActivity.this, "Данные питомца обновлены!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditPetActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}