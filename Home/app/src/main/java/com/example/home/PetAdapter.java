package com.example.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;



import com.squareup.picasso.Picasso;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private List<Pet> petList;
    private Context context;
    private boolean isEditMode;
    private boolean isAdmin;

    public PetAdapter(List<Pet> petList, Context context, boolean isEditMode, boolean isAdmin) {
        this.petList = petList;
        this.context = context;
        this.isEditMode = isEditMode;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);

        holder.textName.setText(pet.getName());
        holder.textBreed.setText(pet.getBreed());
        holder.textAge.setText(pet.getAge() + " месяцев");
        holder.textPrice.setText(pet.getPrice() + " руб.");

        // Загрузка изображения
        if (pet.getImageUrl() != null && !pet.getImageUrl().isEmpty()) {
            Picasso.get()
                    .load(pet.getImageUrl())
                    .placeholder(R.drawable.placeholder_pet)
                    .error(R.drawable.placeholder_pet)
                    .into(holder.imagePet);
        } else {
            holder.imagePet.setImageResource(R.drawable.placeholder_pet);
        }


        if (isAdmin) {

            holder.imageFavorite.setVisibility(View.GONE);
        } else {
            holder.imageFavorite.setVisibility(View.VISIBLE);



            // в избранном ли питомец
            SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userId = userPrefs.getString("user_id", "");

            if (!userId.isEmpty()) {
                SharedPreferences favoritesPrefs = context.getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);
                String favoritesKey = "favorite_ids_" + userId;
                Set<String> favorites = favoritesPrefs.getStringSet(favoritesKey, new HashSet<>());

                if (favorites.contains(pet.getId())) {
                    holder.imageFavorite.setImageResource(R.drawable.ic_favorite);
                } else {
                    holder.imageFavorite.setImageResource(R.drawable.ic_favorite_border);
                }
            }

            holder.imageFavorite.setOnClickListener(v -> {
                toggleFavorite(pet.getId(), holder.imageFavorite);
            });
        }


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


            intent.putExtra("is_admin_viewing", isAdmin);

            context.startActivity(intent);
        });


        if (isEditMode) {
            holder.itemView.setOnLongClickListener(v -> {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Редактирование питомца")
                        .setMessage("Вы хотите редактировать питомца " + pet.getName() + "?")
                        .setPositiveButton("Редактировать", (dialog, which) -> {

                            Intent intent = new Intent(context, EditPetActivity.class);
                            intent.putExtra("pet_id", pet.getId());
                            context.startActivity(intent);
                        })
                        .setNegativeButton("Отмена", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                if (positiveButton != null) {
                    positiveButton.setTextColor(context.getResources().getColor(android.R.color.holo_purple));
                }
                if (negativeButton != null) {
                    negativeButton.setTextColor(context.getResources().getColor(android.R.color.holo_purple));
                }

                return true;
            });
        }
    }

    private void toggleFavorite(String petId, ImageView heartIcon) {
        SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", "");

        if (userId.isEmpty()) {
            Toast.makeText(context, "Войдите в аккаунт", Toast.LENGTH_SHORT).show();
            context.startActivity(new Intent(context, LoginActivity.class));
            return;
        }

        SharedPreferences favoritesPrefs = context.getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = favoritesPrefs.edit();

        String favoritesKey = "favorite_ids_" + userId;
        Set<String> favorites = favoritesPrefs.getStringSet(favoritesKey, new HashSet<>());
        Set<String> updatedFavorites = new HashSet<>(favorites);

        if (updatedFavorites.contains(petId)) {
            updatedFavorites.remove(petId);
            heartIcon.setImageResource(R.drawable.ic_favorite_border);
            Toast.makeText(context, "Удалено из избранного", Toast.LENGTH_SHORT).show();
        } else {
            updatedFavorites.add(petId);
            heartIcon.setImageResource(R.drawable.ic_favorite);
            Toast.makeText(context, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
        }

        editor.putStringSet(favoritesKey, updatedFavorites);
        editor.apply();
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePet, imageFavorite;
        TextView textName, textBreed, textAge, textPrice;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePet = itemView.findViewById(R.id.imagePet);
            imageFavorite = itemView.findViewById(R.id.imageFavorite);
            textName = itemView.findViewById(R.id.textName);
            textBreed = itemView.findViewById(R.id.textBreed);
            textAge = itemView.findViewById(R.id.textAge);
            textPrice = itemView.findViewById(R.id.textPrice);
        }
    }
}