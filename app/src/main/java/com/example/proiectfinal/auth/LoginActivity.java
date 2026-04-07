package com.example.proiectfinal.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;



import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import com.example.proiectfinal.R;
import com.example.proiectfinal.firebase.FirebaseHelper;
import com.example.proiectfinal.utils.SessionManager;
import com.google.firebase.database.DatabaseReference;


public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private EditText editEmail, editPassword;
    private Button btnLogin, btnGoToRegister;
    private LinearLayout btnGoogleSignIn;
    private CheckBox checkRemeber;
    private SessionManager sessionManager;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        checkRemeber=findViewById(R.id.checkRemember);

        ImageView ivTogglePassword= findViewById(R.id.ivTogglePassword);

        ivTogglePassword.setOnClickListener(v-> {
            if (editPassword.getTransformationMethod() instanceof PasswordTransformationMethod){
                editPassword.setTransformationMethod(null);
                ivTogglePassword.setImageResource(R.drawable.ic_eye_on);
            }else {
                editPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_eye_off);
            }
            editPassword.setSelection(editPassword.getText().length());
        });



        sessionManager=new SessionManager(this);
        auth=FirebaseAuth.getInstance();

        if (!sessionManager.isRemember() && FirebaseAuth.getInstance().getCurrentUser() !=null) {
            startActivity(new Intent(LoginActivity.this, PinActivity.class));
            finish();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                                .build();
        googleSignInClient = GoogleSignIn.getClient(this,gso);


        btnLogin.setOnClickListener(v-> doLogin());
        btnGoToRegister.setOnClickListener(v->
                startActivity(new Intent(this,RegisterActivity.class))
        );
        btnGoogleSignIn.setOnClickListener(v-> startGoogleSignIn());
    }

    private void startGoogleSignIn(){
        Intent signInIntent= googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent,RC_GOOGLE_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode== RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task= GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account= task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e ) {
                Toast.makeText(this, "Google sign in failed"+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        String email= account.getEmail();
        String idToken= account.getIdToken();
        AuthCredential googleCredential = GoogleAuthProvider.getCredential(idToken,null);

        //Verificam metodele de autentificare pentru email
        auth.fetchSignInMethodsForEmail(email).addOnSuccessListener(result-> {
             boolean hasPassword= result.getSignInMethods().contains("password");

             if (hasPassword) {
                 //Cerem parola pentru linking
                 EditText passwordInput= new EditText(this);
                 passwordInput.setHint("Parola cont existent");

                 new AlertDialog.Builder(this)
                         .setTitle("Cont existent")
                                 .setMessage("Exista un cont cu email si parola. Introdu parola pentru a lega contul Google.")
                                         .setView(passwordInput)
                                                 .setPositiveButton("Ok",(dialog, which) -> {
                                                     String password= passwordInput.getText().toString().trim();
                                                     if (!password.isEmpty()) {
                                                         //Login cu email+parola
                                                         auth.signInWithEmailAndPassword(email,password)
                                                                 .addOnSuccessListener(authResult -> {
                                                                     //linking Google
                                                                     authResult.getUser().linkWithCredential(googleCredential)
                                                                             .addOnSuccessListener(linkResult ->proccedAfterLogin(email))
                                                                             .addOnFailureListener(e->
                                                                                     Toast.makeText(this, "Eroare linking: "+ e.getMessage(), Toast.LENGTH_SHORT).show());
                                                                 })
                                                                 .addOnFailureListener(e->
                                                                         Toast.makeText(this, "Parola incorecta sau eroare login: "+ e.getMessage(), Toast.LENGTH_SHORT).show());
                                                     }
                                                 }).setNegativeButton("Cancel",(dialog, which) -> {
                                                     googleSignInClient.signOut();
                                                     dialog.dismiss();
                                                 }).setCancelable(false)
                                                    .show();

             }else {
                 auth.signInWithCredential(googleCredential)
                         .addOnSuccessListener(authResult -> proccedAfterLogin(email))
                         .addOnFailureListener(e->
                                 Toast.makeText(this, "Autentificare Google eusata!", Toast.LENGTH_SHORT).show());
             }
        }).addOnFailureListener(e->
                Toast.makeText(this, "Eroare verificare email: "+ e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void proccedAfterLogin(String email) {
        sessionManager.saveSession(email,checkRemeber.isChecked());
        sessionManager.setLoggedIn(true);

        String uid= auth.getCurrentUser().getUid();
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        DatabaseReference pinRef = firebaseHelper.getRef().child(uid).child("pin");

        pinRef.get().addOnSuccessListener(snapshot-> {
            if (snapshot.exists()) {
                sessionManager.savePin(snapshot.getValue(String.class));

                DatabaseReference fpRef = firebaseHelper.getRef().child(uid).child("useFingerprint");
                fpRef.get().addOnSuccessListener(fp-> {
                    boolean useFingerprint = fp.exists() && Boolean.TRUE.equals(fp.getValue(Boolean.class));
                    sessionManager.setUseFingerprint(useFingerprint);

                    startActivity(new Intent(LoginActivity.this,PinActivity.class));
                    finish();
                });
            }else {
                startActivity(new Intent(LoginActivity.this,PinActivity.class));
                finish();
            }
        });
    }


    private void doLogin(){
        String email= editEmail.getText().toString().trim();
        String password=editPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Completeaza toate campurile!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email,password)
                .addOnSuccessListener(a-> proccedAfterLogin(email))
                .addOnFailureListener(e-> Toast.makeText(this, "Eroare: "+ e.getMessage(), Toast.LENGTH_SHORT).show());


    }
}