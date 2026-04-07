package com.example.proiectfinal.views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proiectfinal.R;
import com.example.proiectfinal.adapters.AvatarAdapter;
import com.example.proiectfinal.utils.ImageCompressorHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.ArrayList;
import java.util.List;

public class AccountSettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private CardView btnChangeAvatar;
    private ImageView ivAvatar;
    private TextView tvGreetingName, tvNameValue, tvEmailValue, btnLogout;
    private LinearLayout rowEditName, rowChangePassword;

    private FirebaseAuth auth;

    // Lansatorul care primește poza decupată de la ecranul tău Custom
    private final ActivityResultLauncher<Intent> customCropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String croppedUriString = result.getData().getStringExtra("CROPPED_URI");
                    if (croppedUriString != null) {
                        processAndUploadImage(Uri.parse(croppedUriString));
                    }
                }
            });

    // Lansatorul clasic pentru a alege poza din telefon
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Intent intent = new Intent(this, CustomCropActivity.class);
                    intent.putExtra("IMAGE_URI", uri.toString());
                    customCropLauncher.launch(intent);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_account_settings);
            auth = FirebaseAuth.getInstance();
            initViews();
            populateUserData();
            setupClickListeners();
        } catch (Exception e) {
            Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_settings);
        btnChangeAvatar = findViewById(R.id.btn_change_avatar);
        ivAvatar = findViewById(R.id.iv_profile_avatar);
        tvGreetingName = findViewById(R.id.tv_greeting_name);
        tvNameValue = findViewById(R.id.tv_settings_name_value);
        tvEmailValue = findViewById(R.id.tv_settings_email_value);
        rowEditName = findViewById(R.id.row_edit_name);
        rowChangePassword = findViewById(R.id.row_change_password);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void populateUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            String name = user.getDisplayName() != null ? user.getDisplayName() : "Exploratorule";
            tvGreetingName.setText("Salut, " + name + "!");
            tvNameValue.setText(name);
            tvEmailValue.setText(email != null ? email : "Fără email");

            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivAvatar);
            }
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnChangeAvatar.setOnClickListener(v -> showAvatarPickerBottomSheet());

        rowEditName.setOnClickListener(v -> showEditNameDialog());
        rowChangePassword.setOnClickListener(v -> Toast.makeText(this, "Resetare parolă în curând!", Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showEditNameDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(tvNameValue.getText().toString());
        input.setSelection(input.getText().length());

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Schimbă Numele")
                .setMessage("Introdu numele tău complet sau un nickname:")
                .setView(container)
                .setPositiveButton("Salvează", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateNameInFirebase(newName);
                    } else {
                        Toast.makeText(this, "Numele nu poate fi gol!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Anulează", null)
                .show();
    }

    private void updateNameInFirebase(String newName) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates =
                    new UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            tvNameValue.setText(newName);
                            tvGreetingName.setText("Salut, " + newName + "!");
                            Toast.makeText(this, "Nume actualizat cu succes! 🎉", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Eroare la actualizare.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // ========================================================
    // LOGICA PENTRU SELECTAREA POZEI (GALERIE VS AI)
    // ========================================================
    private void showAvatarPickerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_avatar, null);
        dialog.setContentView(sheetView);

        // Opțiunea 1: Galerie (Codul tău original)
        sheetView.findViewById(R.id.btn_pick_gallery).setOnClickListener(v -> {
            dialog.dismiss();
            galleryLauncher.launch("image/*");
        });

        // Opțiunea 2: Avatare Predefinite (Aici am legat funcția nouă)
        sheetView.findViewById(R.id.btn_pick_predefined).setOnClickListener(v -> {
            dialog.dismiss();
            openPredefinedAvatarsSheet();
        });

        dialog.show();
    }

    // NOU: Funcția care deschide grila cu avatare
    private void openPredefinedAvatarsSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        // Atenție: folosesc numele din screenshot-ul tău!
        android.view.View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_avatars_profile, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        List<Integer> avatars = new ArrayList<>();
        avatars.add(R.drawable.avatar_1);
        avatars.add(R.drawable.avatar_2);
        avatars.add(R.drawable.avatar_3);
        avatars.add(R.drawable.avatar_4);
        avatars.add(R.drawable.avatar_5);
        avatars.add(R.drawable.avatar_6);
        avatars.add(R.drawable.avatar_7);
        avatars.add(R.drawable.avatar_8);

        RecyclerView recyclerAvatars = bottomSheetView.findViewById(R.id.recycler_avatars);
        MaterialButton btnSave = bottomSheetView.findViewById(R.id.btn_save_avatar);

        final int[] selectedAvatarId = {-1};

        recyclerAvatars.setLayoutManager(new GridLayoutManager(this, 3));
        AvatarAdapter adapter = new AvatarAdapter(this, avatars, avatarResId -> {
            selectedAvatarId[0] = avatarResId;
            btnSave.setEnabled(true);
        });
        recyclerAvatars.setAdapter(adapter);

        btnSave.setOnClickListener(v -> {
            if (selectedAvatarId[0] != -1) {
                String path = "android.resource://" + getPackageName() + "/" + selectedAvatarId[0];
                Uri imageUri = Uri.parse(path);

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setPhotoUri(imageUri)
                            .build();

                    user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Avatar actualizat cu succes!", Toast.LENGTH_SHORT).show();
                            bottomSheetDialog.dismiss();

                            // Actualizăm poza și pe ecranul curent instantaneu
                            Glide.with(AccountSettingsActivity.this)
                                    .load(selectedAvatarId[0])
                                    .circleCrop()
                                    .into(ivAvatar);
                        } else {
                            Toast.makeText(this, "Eroare la salvarea avatarului.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        bottomSheetDialog.show();
    }

    // ========================================================
    // LOGICA VECHE PENTRU CLOUDINARY (GALERIE) - Rămâne neatinsă
    // ========================================================
    private void processAndUploadImage(Uri imageUri) {
        byte[] compressedImageBytes = ImageCompressorHelper.compressImageFromUri(this, imageUri);

        if (compressedImageBytes == null) {
            Toast.makeText(this, "Eroare la procesarea imaginii.", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Se actualizează poza de profil... ⏳");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String cloudName = getString(R.string.cloudinary_cloud_name);
        String uploadPreset = getString(R.string.cloudinary_upload_preset);
        String userUid = auth.getCurrentUser().getUid();

        String uniquePublicId = userUid + "_" + System.currentTimeMillis();
        String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";

        okhttp3.MultipartBody requestBody = new okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", "Avatar")
                .addFormDataPart("public_id", uniquePublicId)
                .addFormDataPart("file", uniquePublicId + ".jpg",
                        okhttp3.RequestBody.create(compressedImageBytes, okhttp3.MediaType.parse("image/jpeg")))
                .build();

        okhttp3.Request request = new okhttp3.Request.Builder().url(url).post(requestBody).build();

        new okhttp3.OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(AccountSettingsActivity.this, "Eroare de conexiune la Cloudinary", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                String responseData = response.body().string();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        try {
                            org.json.JSONObject jsonObject = new org.json.JSONObject(responseData);
                            saveNewAvatarUrlToFirebase(jsonObject.getString("secure_url"));
                        } catch (Exception e) {}
                    } else {
                        Toast.makeText(AccountSettingsActivity.this, "Eroare la upload.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveNewAvatarUrlToFirebase(String photoUrl) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(photoUrl))
                    .build();

            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(AccountSettingsActivity.this, "Poză actualizată! 🎉", Toast.LENGTH_SHORT).show();
                    Glide.with(AccountSettingsActivity.this).load(photoUrl).circleCrop().into(ivAvatar);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Exploratorule";
            tvGreetingName.setText("Salut, " + name + "!");
            tvNameValue.setText(name);

            if (currentUser.getPhotoUrl() != null && ivAvatar != null) {
                Glide.with(this).load(currentUser.getPhotoUrl()).circleCrop().into(ivAvatar);
            }
        }
    }
}