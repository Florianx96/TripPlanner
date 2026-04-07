package com.example.proiectfinal.views;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.canhub.cropper.CropImageView;
import com.example.proiectfinal.R;

import java.io.File;
import java.io.FileOutputStream;

public class CustomCropActivity extends AppCompatActivity {

    private CropImageView cropImageView;
    private Uri sourceUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_crop);

        cropImageView = findViewById(R.id.customCropImageView);
        ImageButton btnRotate = findViewById(R.id.btn_rotate);
        Button btnReset = findViewById(R.id.btn_reset);
        Button btnSave = findViewById(R.id.btn_save);
        ImageButton btnBackCrop = findViewById(R.id.btn_back_crop);

        btnBackCrop.setOnClickListener(v -> finish());

        // 1. Preluăm imaginea aleasă din Galerie
        String uriString = getIntent().getStringExtra("IMAGE_URI");
        if (uriString != null) {
            sourceUri = Uri.parse(uriString);
            cropImageView.setImageUriAsync(sourceUri);
        } else {
            Toast.makeText(this, "Eroare la încărcarea imaginii", Toast.LENGTH_SHORT).show();
            finish();
        }

        // --- BUTONUL DE ROTIRE ---
        btnRotate.setOnClickListener(v -> {
            cropImageView.rotateImage(90);
        });

        // --- BUTONUL DE RESET ---
        btnReset.setOnClickListener(v -> {
            cropImageView.setImageUriAsync(sourceUri); // Reîncarcă imaginea curată
        });

        // --- BUTONUL DE SALVARE (REPARAT 100%) ---
        btnSave.setOnClickListener(v -> {
            // Extragem imaginea decupată direct de pe ecran
            Bitmap croppedBitmap = cropImageView.getCroppedImage();

            if (croppedBitmap != null) {
                try {
                    // Creăm un fișier temporar unde să o salvăm
                    File tempFile = new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream out = new FileOutputStream(tempFile);

                    // Salvăm imaginea în fișier
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();

                    Uri destinationUri = Uri.fromFile(tempFile);

                    // O trimitem înapoi către AccountSettingsActivity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("CROPPED_URI", destinationUri.toString());
                    setResult(RESULT_OK, resultIntent);
                    finish();

                } catch (Exception e) {
                    Toast.makeText(this, "Eroare la salvare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Eroare la decupare!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}