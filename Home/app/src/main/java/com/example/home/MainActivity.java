package com.example.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_PETS;
    private static final String SUPABASE_API_KEY = SupabaseConfig.SUPABASE_API_KEY;

    private RecyclerView recyclerView;
    private ChipGroup chipGroupFilter;
    private List<Pet> petList = new ArrayList<>();
    private List<Pet> filteredPetList = new ArrayList<>();
    private boolean isAdminEditMode = false;


    private Set<String> activeFilters = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("confing_check", "MainActivity onCreate started");
        Log.d("confing_check", "Using URL: " + SupabaseConfig.SUPABASE_BASE_URL);
        Log.d("confing_check", "API Key exists: " + (SupabaseConfig.SUPABASE_API_KEY != null));

        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewPets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chipGroupFilter = findViewById(R.id.chipGroupFilter);

        // Роль пользователя
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userRole = prefs.getString("user_role", "Client");
        boolean isAdmin = "Manager".equals(userRole);

        isAdminEditMode = getIntent().getBooleanExtra("admin_edit_mode", false);

        // Настройка UI в зависимости от роли
        setupRoleBasedUI(isAdmin);

        if (isAdminEditMode) {
            setTitle("Редактирование питомцев");
            Toast.makeText(this, "Долгое нажатие на питомца для редактирования", Toast.LENGTH_LONG).show();
        }

        setupFilterChips();
        loadPetsFromSupabase();
    }

    private void setupRoleBasedUI(boolean isAdmin) {

        ImageButton btnFavorites = findViewById(R.id.btnFavorites);
        ImageButton btnChats = findViewById(R.id.btnChats);
        ImageButton btnProfile = findViewById(R.id.btnProfile);

        if (isAdmin) {

            btnFavorites.setVisibility(View.GONE);
            btnChats.setVisibility(View.GONE);
            btnProfile.setVisibility(View.GONE);


            TextView filtersTitle = findViewById(R.id.textFiltersTitle);
            if (filtersTitle != null) {
                filtersTitle.setVisibility(View.VISIBLE);
            }
        } else {

            btnFavorites.setVisibility(View.VISIBLE);
            btnChats.setVisibility(View.VISIBLE);
            btnProfile.setVisibility(View.VISIBLE);

            btnFavorites.setOnClickListener(v -> {
                startActivity(new Intent(this, FavoritesActivity.class));
            });

            btnChats.setOnClickListener(v -> {
                startActivity(new Intent(this, ChatListActivity.class));
            });

            btnProfile.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            });
        }
    }

    private void setupFilterChips() {

        chipGroupFilter.removeAllViews();


        chipGroupFilter.setSingleSelection(false);


        String[] filterNames = {"Все", "Собаки", "Кошки", "Маленькие", "Средние",
                "Большие", "Короткош.", "Среднеш.", "Длиннош."};

        String[] filterKeys = {"all", "dog", "cat", "small", "medium",
                "large", "short", "medium_hair", "long"};


        ColorStateList strokeColor = ColorStateList.valueOf(Color.parseColor("#E0E0E0"));


        for (int i = 0; i < filterNames.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(filterNames[i]);
            chip.setCheckable(true);
            chip.setTag(filterKeys[i]);


            String key = filterKeys[i];
            if (key.equals("dog") || key.equals("cat")) {

                chip.setTextColor(Color.parseColor("#4CAF50"));
            } else if (key.equals("small") || key.equals("medium") || key.equals("large")) {

                chip.setTextColor(Color.parseColor("#2196F3"));
            } else if (key.equals("short") || key.equals("medium_hair") || key.equals("long")) {

                chip.setTextColor(Color.parseColor("#9C27B0"));
            } else {

                chip.setTextColor(Color.parseColor("#607D8B"));
            }


            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeColor(strokeColor);
            chip.setChipStrokeWidth(1.5f);


            chip.setCloseIconVisible(false);


            if (key.equals("all")) {
                chip.setChecked(true);
                activeFilters.add("all");
            }

            chip.setOnClickListener(v -> {
                Chip clickedChip = (Chip) v;
                String filterKey = (String) clickedChip.getTag();

                if (filterKey.equals("all")) {

                    if (clickedChip.isChecked()) {
                        clearAllFiltersExceptAll();
                        chip.setChecked(true);
                    }
                } else {

                    Chip allChip = findChipByTag("all");
                    if (allChip != null && allChip.isChecked()) {
                        allChip.setChecked(false);
                        activeFilters.remove("all");
                    }


                    if (clickedChip.isChecked()) {
                        activeFilters.add(filterKey);
                    } else {
                        activeFilters.remove(filterKey);
                    }
                }

                filterPets();
            });

            chipGroupFilter.addView(chip);
        }
    }

    private Chip findChipByTag(String tag) {
        for (int i = 0; i < chipGroupFilter.getChildCount(); i++) {
            View child = chipGroupFilter.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (tag.equals(chip.getTag())) {
                    return chip;
                }
            }
        }
        return null;
    }

    private void clearAllFiltersExceptAll() {

        for (int i = 0; i < chipGroupFilter.getChildCount(); i++) {
            View child = chipGroupFilter.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                String tag = (String) chip.getTag();
                if (!tag.equals("all")) {
                    chip.setChecked(false);
                }
            }
        }


        activeFilters.clear();
        activeFilters.add("all");
    }

    private void filterPets() {
        filteredPetList.clear();


        if (activeFilters.isEmpty() || activeFilters.contains("all")) {
            filteredPetList.addAll(petList);
        } else {

            boolean filterByType = activeFilters.contains("dog") || activeFilters.contains("cat");
            boolean filterBySize = activeFilters.contains("small") || activeFilters.contains("medium") || activeFilters.contains("large");
            boolean filterByHair = activeFilters.contains("short") || activeFilters.contains("medium_hair") || activeFilters.contains("long");

            for (Pet pet : petList) {
                boolean matches = true;


                if (filterByType) {
                    boolean typeMatches = false;
                    if (activeFilters.contains("dog") && pet.getType().equals("dog")) typeMatches = true;
                    if (activeFilters.contains("cat") && pet.getType().equals("cat")) typeMatches = true;
                    if (!typeMatches) matches = false;
                }


                if (filterBySize) {
                    boolean sizeMatches = false;
                    if (activeFilters.contains("small") && pet.getSize().equals("Маленький")) sizeMatches = true;
                    if (activeFilters.contains("medium") && pet.getSize().equals("Средний")) sizeMatches = true;
                    if (activeFilters.contains("large") && pet.getSize().equals("Большой")) sizeMatches = true;
                    if (!sizeMatches) matches = false;
                }


                if (filterByHair) {
                    boolean hairMatches = false;
                    if (activeFilters.contains("short") && pet.getHairLength().equals("Короткошерстный")) hairMatches = true;
                    if (activeFilters.contains("medium_hair") && pet.getHairLength().equals("Среднешерстный")) hairMatches = true;
                    if (activeFilters.contains("long") && pet.getHairLength().equals("Длинношерстный")) hairMatches = true;
                    if (!hairMatches) matches = false;
                }

                if (matches) {
                    filteredPetList.add(pet);
                }
            }
        }

        setupRecyclerView();


        String message;
        if (filteredPetList.size() == petList.size()) {
            message = "Все питомцы: " + filteredPetList.size();
        } else {
            message = "Найдено: " + filteredPetList.size();
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void loadPetsFromSupabase() {
        Log.d("МОИ_ЛОГИ", "Загрузка питомцев из базы данных");
        OkHttpClient client = new OkHttpClient();

        String optimizedUrl = SUPABASE_URL + "?select=id,name,type,breed,age,price,image_url&limit=50";
        Request request = new Request.Builder()
                .url(optimizedUrl)
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                    Log.e("PETS", "Connection error", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    Log.d("МОИ_ЛОГИ", "Успешно! Ответ от Supabase: " + response.code());
                    try {
                        JSONArray petsArray = new JSONArray(responseBody);
                        Log.d("МОИ_ЛОГИ", "Загружено питомцев: " + petsArray.length());
                        petList.clear();

                        for (int i = 0; i < petsArray.length(); i++) {
                            JSONObject petJson = petsArray.getJSONObject(i);
                            Pet pet = new Pet(
                                    petJson.optString("id", ""),
                                    petJson.optString("name", "Неизвестно"),
                                    petJson.optString("type", "other"),
                                    petJson.optString("breed", "Неизвестно"),
                                    petJson.optInt("age", 0),
                                    petJson.optString("gender", "male"),
                                    petJson.optString("description", "Нет описания"),
                                    petJson.optDouble("price", 0.0),
                                    petJson.optString("image_url", ""),
                                    petJson.optString("size", ""),
                                    petJson.optString("hair_length", ""),
                                    petJson.optString("color", ""),
                                    petJson.optString("address", ""),
                                    petJson.optString("phone", ""),
                                    petJson.optString("created_date", "")
                            );
                            petList.add(pet);
                        }

                        runOnUiThread(() -> {
                            filterPets();
                            if (isAdminEditMode) {
                                Toast.makeText(MainActivity.this,
                                        "Загружено: " + petList.size() + " питомцев",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

                    } catch (JSONException e) {
                        Log.e("PETS", "JSON error", e);
                    }
                }
            }
        });
    }

    private void setupRecyclerView() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userRole = prefs.getString("user_role", "Client");
        boolean isAdmin = "Manager".equals(userRole);


        PetAdapter adapter = new PetAdapter(filteredPetList, this, isAdminEditMode, isAdmin);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }
}