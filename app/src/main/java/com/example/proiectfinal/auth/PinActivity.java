package com.example.proiectfinal.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proiectfinal.firebase.FirebaseHelper;
import com.example.proiectfinal.utils.HashUtil;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proiectfinal.views.MainActivity;
import com.example.proiectfinal.R;
import com.example.proiectfinal.utils.SessionManager;

public class PinActivity extends AppCompatActivity {

    private EditText editPin;
    private Button btnConfirmPin;
    private TextView tvTitle;

    SessionManager sessionManager;
    boolean isCreatingPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        editPin=findViewById(R.id.editPin);
        btnConfirmPin=findViewById(R.id.btnConfirmPin);
        tvTitle=findViewById(R.id.tvTitle);
        sessionManager = new SessionManager(this);

        isCreatingPin = !sessionManager.hasPin();

        if (isCreatingPin) {
            tvTitle.setText("Creaza un PIN");
        } else {
            tvTitle.setText("Introdu PIN-ul");
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseHelper firebaseHelper = new FirebaseHelper();

        firebaseHelper.getRef().child(uid).child("pin").get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        // pin-ul a fost sters → fortam crearea lui
                        isCreatingPin = true;
                        tvTitle.setText("Creeaza un PIN");

                        // stergem si pin-ul local (ca sa nu mai creada aplicatia ca exista)
                        sessionManager.savePin(null);
                    }
                });

        btnConfirmPin.setOnClickListener(v->handlePin());

        boolean useFingerprint = sessionManager.isFingerprintEnabled();

        if (!isCreatingPin && useFingerprint) {
           startFingerprintAuth();
        }

    }
    private void handlePin(){
        String pin = editPin.getText().toString().trim();

        if(pin.length() != 4) {
            Toast.makeText(this, "PIN-ul trebuie sa aiba EXACT 4 cifre!", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashedPin = HashUtil.sha256(pin);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        DatabaseReference pinRef = firebaseHelper.getRef().child(uid).child("pin");

        if (isCreatingPin) {

            pinRef.setValue(hashedPin).addOnSuccessListener(a -> {
                sessionManager.savePin(hashedPin); // salvam hash-ul local
                sessionManager.setUseFingerprint(true); //activam fingerpint local

                //Salvam optiunea si in Firebase ca boolean (pentru diviceurile viitoare)
                DatabaseReference fpRef =firebaseHelper.getRef().child(uid).child("useFingerprint");
                fpRef.setValue(true).addOnCompleteListener(task -> {});
                Toast.makeText(this, "PIN creat cu succes!", Toast.LENGTH_SHORT).show();
                goToMain();
            });

        } else {
            String savedHashedPin = sessionManager.getPin();

            if (savedHashedPin.equals(hashedPin)) {
                Toast.makeText(this, "PIN corect!", Toast.LENGTH_SHORT).show();
                goToMain();
            } else {
                Toast.makeText(this, "PIN incorect!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void startFingerprintAuth(){

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt= new BiometricPrompt(this,
                executor,
                new  BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    Toast.makeText(PinActivity.this, "Autentificare reusita!", Toast.LENGTH_SHORT).show();
                    goToMain();
                    }
                    @Override
                    public void onAuthenticationFailed(){
                    super.onAuthenticationFailed();
                    Toast.makeText(PinActivity.this, "Autentificare esuata!", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo= new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autentificare cu amprenta")
                .setDescription("Foloseste senzorul de amprenta pentru a te autentifica.")
                .setNegativeButtonText("Anuleaza")
                .build();
        biometricPrompt.authenticate(promptInfo);

    }

    private void goToMain(){
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
