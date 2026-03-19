package com.example.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.squareup.picasso.Picasso;

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

public class FavoritesActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = SupabaseConfig.SUPABASE_BASE_URL + SupabaseConfig.TABLE_PETS;
    private static final String SUPABASE_API_KEY = SupabaseConfig.SUPABASE_API_KEY;
    private RecyclerView recyclerView;
    private List<Pet> petList = new ArrayList<>();
    private List<Pet> favoritePets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        setTitle("Избранные питомцы");

        recyclerView = findViewById(R.id.recyclerViewPets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        TextView mainTitle = findViewById(R.id.textMainTitle);
        if (mainTitle != null) {
            mainTitle.setText("Избранные");
        }


        findViewById(R.id.chipGroupFilter).setVisibility(View.GONE);
        findViewById(R.id.btnFavorites).setVisibility(View.GONE);
        findViewById(R.id.btnChats).setVisibility(View.GONE);
        findViewById(R.id.btnProfile).setVisibility(View.GONE);

        TextView filtersTitle = findViewById(R.id.textFiltersTitle);
        if (filtersTitle != null) {
            filtersTitle.setVisibility(View.GONE);
        }


        loadPetsFromSupabase();
    }

    private void loadPetsFromSupabase() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(SUPABASE_URL)
                .get()
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(FavoritesActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    try {
                        JSONArray petsArray = new JSONArray(responseBody);
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

                            filterFavoritePets();
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void filterFavoritePets() {
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", "");

        SharedPreferences favoritesPrefs = getSharedPreferences("FavoritesPrefs", MODE_PRIVATE);
        String favoritesKey = "favorite_ids_" + userId;
        Set<String> favoriteIds = favoritesPrefs.getStringSet(favoritesKey, new HashSet<>());

        favoritePets.clear();

        for (Pet pet : petList) {
            if (favoriteIds.contains(pet.getId())) {
                favoritePets.add(pet);
            }
        }

        if (favoritePets.isEmpty()) {
            Toast.makeText(this, "В избранном пока нет питомцев", Toast.LENGTH_SHORT).show();
        }

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        FavoritesAdapter adapter = new FavoritesAdapter(favoritePets, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!petList.isEmpty()) {
            filterFavoritePets();
        }
    }


    private static class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder> {

        private List<Pet> favoritePets;
        private FavoritesActivity context;

        public FavoritesAdapter(List<Pet> favoritePets, FavoritesActivity context) {
            this.favoritePets = favoritePets;
            this.context = context;
        }

        @NonNull
        @Override
        public FavoritesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pet, parent, false);
            return new FavoritesViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FavoritesViewHolder holder, int position) {
            Pet pet = favoritePets.get(position);

            holder.textName.setText(pet.getName());
            holder.textBreed.setText(pet.getBreed());
            holder.textAge.setText(pet.getAge() + " месяцев");
            holder.textPrice.setText(pet.getPrice() + " руб.");

            holder.imageFavorite.setImageResource(R.drawable.ic_favorite);


            if (pet.getImageUrl() != null && !pet.getImageUrl().isEmpty()) {
                Picasso.get()
                        .load(pet.getImageUrl())
                        .placeholder(R.drawable.placeholder_pet)
                        .error(R.drawable.placeholder_pet)
                        .into(holder.imagePet);
            } else {
                holder.imagePet.setImageResource(R.drawable.placeholder_pet);
            }

            holder.imageFavorite.setOnClickListener(v -> {
                toggleFavorite(pet.getId(), holder.imageFavorite);
            });

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, PetDetailActivity.class);
                intent.putExtra("pet_id", pet.getId());
                intent.putExtra("pet_name", pet.getName());
                intent.putExtra("pet_type", pet.getType());
                intent.putExtra("pet_breed", pet.getBreed());
                intent.putExtra("pet_age", pet.getAge());
                intent.putExtra("pet_gender", pet.getGender());
                intent.putExtra("pet_description", pet.getDescription());
                intent.putExtra("pet_price", pet.getPrice());
                intent.putExtra("pet_image_url", pet.getImageUrl());
                intent.putExtra("pet_size", pet.getSize());
                intent.putExtra("pet_hair_length", pet.getHairLength());
                intent.putExtra("pet_color", pet.getColor());
                intent.putExtra("pet_address", pet.getAddress());
                intent.putExtra("pet_phone", pet.getPhone());
                intent.putExtra("pet_created_date", pet.getCreatedDate());
                context.startActivity(intent);
            });
        }

        private void toggleFavorite(String petId, ImageView heartIcon) {
            SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = userPrefs.getString("user_id", "");

            SharedPreferences favoritesPrefs = context.getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = favoritesPrefs.edit();


            String favoritesKey = "favorite_ids_" + userId;

            Set<String> favorites = favoritesPrefs.getStringSet(favoritesKey, new HashSet<>());
            Set<String> updatedFavorites = new HashSet<>(favorites);

            if (updatedFavorites.contains(petId)) {
                updatedFavorites.remove(petId);
                heartIcon.setImageResource(R.drawable.ic_favorite_border);
                Toast.makeText(context, "Удалено из избранного", Toast.LENGTH_SHORT).show();


                context.filterFavoritePets();
            }

            editor.putStringSet(favoritesKey, updatedFavorites);
            editor.apply();
        }

        @Override
        public int getItemCount() {
            return favoritePets.size();
        }

        static class FavoritesViewHolder extends RecyclerView.ViewHolder {
            ImageView imagePet;
            TextView textName, textBreed, textAge, textPrice;
            ImageView imageFavorite;

            public FavoritesViewHolder(@NonNull View itemView) {
                super(itemView);
                imagePet = itemView.findViewById(R.id.imagePet);
                textName = itemView.findViewById(R.id.textName);
                textBreed = itemView.findViewById(R.id.textBreed);
                textAge = itemView.findViewById(R.id.textAge);
                textPrice = itemView.findViewById(R.id.textPrice);
                imageFavorite = itemView.findViewById(R.id.imageFavorite);
            }
        }
    }
}