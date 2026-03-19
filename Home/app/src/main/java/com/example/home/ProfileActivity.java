package com.example.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ProfileActivity extends AppCompatActivity {

    private static final int EDIT_PROFILE_REQUEST = 1;

    private TextView textUserName, textUserRole, textUserEmail;
    private CardView btnEditProfile, btnBackToMain, btnLogoutProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        textUserName = findViewById(R.id.textUserName);
        textUserRole = findViewById(R.id.textUserRole);
        textUserEmail = findViewById(R.id.textUserEmail);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnBackToMain = findViewById(R.id.btnBackToMain);
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile);

        loadUserData();

        btnEditProfile.setOnClickListener(v -> {

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String role = prefs.getString("user_role", "Client");

            if ("Manager".equals(role)) {
                Toast.makeText(this, "Менеджеры не могут редактировать профиль", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivityForResult(intent, EDIT_PROFILE_REQUEST);
            }
        });

        btnBackToMain.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        btnLogoutProfile.setOnClickListener(v -> {
            showLogoutConfirmation();
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Мой профиль");
        }
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выход из аккаунта")
                .setMessage("Вы точно хотите выйти из аккаунта?")
                .setPositiveButton("Да, выйти", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();

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

        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            loadUserData();
        }
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        String username = prefs.getString("username", "Пользователь");
        String role = prefs.getString("user_role", "Client");
        String userId = prefs.getString("user_id", "");

        textUserName.setText(username);
        textUserRole.setText(role.equals("Manager") ? "Менеджер" : "Клиент");
        textUserEmail.setText("ID: " + userId);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}