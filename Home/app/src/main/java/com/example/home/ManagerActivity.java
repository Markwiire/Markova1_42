package com.example.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manager_activity);

        CardView cardAddPet = findViewById(R.id.cardAddPet);
        CardView cardEditPets = findViewById(R.id.cardEditPets);
        CardView cardViewPets = findViewById(R.id.cardViewPets);
        Button btnLogout = findViewById(R.id.btnLogout);

        CardView cardViewChats = findViewById(R.id.cardViewChats);
        cardViewChats.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, ChatListActivity.class);
            startActivity(intent);
        });

        cardAddPet.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, AddPetActivity.class);
            startActivity(intent);
        });

        cardEditPets.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, MainActivity.class);
            intent.putExtra("admin_edit_mode", true);
            startActivity(intent);
        });

        cardViewPets.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerActivity.this, MainActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            showLogoutConfirmation();
        });
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выход из системы")
                .setMessage("Вы точно хотите выйти из системы?")
                .setPositiveButton("Да, выйти", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Делаем кнопки фиолетовыми
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(android.R.color.holo_purple));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(android.R.color.holo_purple));
        }
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ManagerActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}