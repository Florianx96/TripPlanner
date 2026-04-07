package com.example.proiectfinal.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.proiectfinal.R;
import com.example.proiectfinal.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // 1. Inițializăm elementele vizuale
        ImageButton btnBack = findViewById(R.id.btn_back_main_settings);
        ImageView ivAvatar = findViewById(R.id.iv_settings_main_avatar);
        TextView tvName = findViewById(R.id.tv_settings_main_name);
        TextView tvEmail = findViewById(R.id.tv_settings_main_email);

        LinearLayout rowAccountSettings = findViewById(R.id.row_account_settings);
        LinearLayout rowAppearance = findViewById(R.id.row_appearance);
        LinearLayout rowAuthAction = findViewById(R.id.row_auth_action);

        TextView tvAuthText = findViewById(R.id.tv_auth_text);
        ImageView ivAuthIcon = findViewById(R.id.iv_auth_icon);

        // 2. Populăm datele utilizatorului
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = (currentUser.getEmail() != null) ? currentUser.getEmail().split("@")[0] : "Călătorule";
            }
            tvName.setText(name);
            tvEmail.setText(currentUser.getEmail());

            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this).load(currentUser.getPhotoUrl()).circleCrop().into(ivAvatar);
            }

            // Setăm butonul pe LOGOUT
            tvAuthText.setText("Deconectare");
            tvAuthText.setTextColor(android.graphics.Color.parseColor("#E53935")); // Roșu
            ivAuthIcon.setColorFilter(android.graphics.Color.parseColor("#E53935"));

        } else {
            // Utilizator NElogat
            tvName.setText("Vizitator");
            tvEmail.setText("Conectează-te pentru a salva planuri");
            rowAccountSettings.setVisibility(View.GONE); // Ascundem setările contului dacă nu are cont

            // Setăm butonul pe LOGIN
            tvAuthText.setText("Conectare / Creare Cont");
            tvAuthText.setTextColor(android.graphics.Color.parseColor("#388E3C")); // Verde
            ivAuthIcon.setColorFilter(android.graphics.Color.parseColor("#388E3C"));
            ivAuthIcon.setImageResource(android.R.drawable.ic_menu_myplaces); // O iconiță diferită
        }

        // 3. Setăm click-urile
        btnBack.setOnClickListener(v -> finish());

        rowAccountSettings.setOnClickListener(v -> {
            // Deschidem pagina pe care am făcut-o anterior!
            startActivity(new Intent(SettingsActivity.this, AccountSettingsActivity.class));
        });

        rowAppearance.setOnClickListener(v -> {
            Toast.makeText(this, "Funcția Dark Mode va fi implementată curând!", Toast.LENGTH_SHORT).show();
        });

        rowAuthAction.setOnClickListener(v -> {
            if (currentUser != null) {
                // LOGOUT
                auth.signOut();
                Toast.makeText(this, "Te-ai deconectat cu succes.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Curăță tot istoricul paginilor
                startActivity(intent);
                finish();
            } else {
                // LOGIN
                startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Când ne întoarcem pe panoul principal, reîncărcăm poza!
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        ImageView ivAvatar = findViewById(R.id.iv_settings_main_avatar);

        if (currentUser != null && currentUser.getPhotoUrl() != null && ivAvatar != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .circleCrop()
                    .into(ivAvatar);
        }
    }
}