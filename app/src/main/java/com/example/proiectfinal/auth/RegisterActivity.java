package com.example.proiectfinal.auth;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;



import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.example.proiectfinal.utils.EmailSender;
import java.util.Random;

import com.example.proiectfinal.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText editName,editEmail,editPassword,editConfirmPassword;
    private Button btnRegister,btnGoToLogin;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

       editName = findViewById(R.id.editName);
       editEmail=findViewById(R.id.editEmail);
       editPassword=findViewById(R.id.editPassword);
       editConfirmPassword=findViewById(R.id.editConfirmPassword);
       btnRegister=findViewById(R.id.btnRegister);
       btnGoToLogin=findViewById(R.id.btnGoToLogin);

       ImageView ivTogglePassword= findViewById(R.id.ivTogglePassword);
       ImageView ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);

       ivTogglePassword.setOnClickListener(v-> {
           if (editPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {

               editPassword.setTransformationMethod(null);
               ivTogglePassword.setImageResource(R.drawable.ic_eye_on);
           }else {
               editPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
               ivTogglePassword.setImageResource(R.drawable.ic_eye_off);
           }
           editPassword.setSelection(editPassword.getText().length());
       });

        ivToggleConfirmPassword.setOnClickListener(v-> {
            if (editConfirmPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {

                editConfirmPassword.setTransformationMethod(null);
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_on);
            }else {
                editConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off);
            }
            editConfirmPassword.setSelection(editConfirmPassword.getText().length());
        });

       auth=FirebaseAuth.getInstance();

       btnRegister.setOnClickListener(v-> doRegister());
       btnGoToLogin.setOnClickListener(v->
               startActivity(new Intent(this,LoginActivity.class))
       );
    }

    private void doRegister() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString().trim();
        String pass2 = editConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || pass2.isEmpty()) {
            Toast.makeText(this, "Completeaza toate campurile!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(pass2)) {
            Toast.makeText(this, "Parolele nu coincid!", Toast.LENGTH_SHORT).show();
            return;
        }

        //Verificam daca emailul exista deja
        auth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    boolean isGoogleUser = result.getSignInMethods().contains("google.com");
                    boolean isEmailPasswordUser= result.getSignInMethods().contains("password");

            if (isGoogleUser) {
                //email folosit cu google
                new AlertDialog.Builder(this)
                        .setTitle("Cont existent")
                        .setMessage("Acest email este deja folosit cu Google! Va rugam sa va logati cu Google.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .setCancelable(false) // nu poate fi inchis altfel decat cu OK
                        .show();
                return;
            }

            if (isEmailPasswordUser){
                //Email deja folosit cu cont email+parola
                new AlertDialog.Builder(this)
                        .setTitle("Cont existent")
                        .setMessage("Acest email exista deja in aplicatie! Va rugam sa va logati cu email si parola.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .setCancelable(false)
                        .show();
                return;
            }
            //daca nu exista inca cont -> cream cont
            sendVerificationEmail(email, pass, name);

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Eroare la verifcare email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    private void sendVerificationEmail(String email, String pass, String name) {
        Toast.makeText(this, "Se trimite codul pe email... Te rugăm să aștepți.", Toast.LENGTH_SHORT).show();
        btnRegister.setEnabled(false); // Dezactivăm butonul să nu dea click de 10 ori

        // Generăm un cod random de 6 cifre
        String generatedCode = String.format("%06d", new Random().nextInt(999999));

        // Trimitem emailul folosind clasa noastra utilitara
        EmailSender.sendVerificationCode(email, generatedCode, new EmailSender.EmailCallback() {
            @Override
            public void onSuccess() {
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Codul a fost trimis! Verifică email-ul.", Toast.LENGTH_LONG).show();

                // Pasăm toate datele către ecranul de verificare
                Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("password", pass);
                intent.putExtra("name", name);
                intent.putExtra("verificationCode", generatedCode);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Eroare la trimitere email: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
