package com.example.proiectfinal.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proiectfinal.R;
import com.example.proiectfinal.utils.EmailSender;
import com.example.proiectfinal.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Random;

public class VerifyEmailActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private EditText editCode;

    // Datele primite de la ecranul precedent
    private String userEmail;
    private String userPass;
    private String userName;
    private String expectedCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        editCode = findViewById(R.id.edit_verification_code);
        Button btnCheckVerification = findViewById(R.id.btn_check_verification);
        TextView tvResendEmail = findViewById(R.id.tv_resend_email);

        // Preluam datele date de RegisterActivity
        userEmail = getIntent().getStringExtra("email");
        userPass = getIntent().getStringExtra("password");
        userName = getIntent().getStringExtra("name");
        expectedCode = getIntent().getStringExtra("verificationCode");

        btnCheckVerification.setOnClickListener(v -> verifyCodeAndCreateAccount());
        tvResendEmail.setOnClickListener(v -> resendVerificationEmail());
    }

    private void verifyCodeAndCreateAccount() {
        String enteredCode = editCode.getText().toString().trim();

        if (enteredCode.isEmpty()) {
            Toast.makeText(this, "Te rugăm să introduci codul!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredCode.equals(expectedCode)) {
            // COD CORECT! Acum cream efectiv contul de Firebase.
            Toast.makeText(this, "Cod corect! Creăm contul...", Toast.LENGTH_SHORT).show();
            createFirebaseAccount();
        } else {
            Toast.makeText(this, "Cod incorect! Te rugăm să verifici email-ul.", Toast.LENGTH_LONG).show();
        }
    }

    private void createFirebaseAccount() {
        auth.createUserWithEmailAndPassword(userEmail, userPass)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(userName)
                                .build();

                        user.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                            // Am terminat, contul este valid si inregistrat!
                            Toast.makeText(VerifyEmailActivity.this, "Cont creat cu succes! 🎉", Toast.LENGTH_SHORT).show();
                            proceedToApp(user.getEmail());
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Eroare creare cont: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void resendVerificationEmail() {
        Toast.makeText(this, "Se retrimite codul...", Toast.LENGTH_SHORT).show();

        // Generăm un alt cod pentru securitate
        expectedCode = String.format("%06d", new Random().nextInt(999999));

        EmailSender.sendVerificationCode(userEmail, expectedCode, new EmailSender.EmailCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(VerifyEmailActivity.this, "Noul cod a fost trimis!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(VerifyEmailActivity.this, "Eroare la retrimitere: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void proceedToApp(String email) {
        sessionManager.saveSession(email, true);
        sessionManager.setLoggedIn(true);

        Intent intent = new Intent(VerifyEmailActivity.this, PinActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}