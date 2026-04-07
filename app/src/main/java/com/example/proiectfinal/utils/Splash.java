package com.example.proiectfinal.utils;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proiectfinal.views.MainActivity;
import com.example.proiectfinal.R;
import com.example.proiectfinal.auth.PinActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Splash extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() ->{

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            SessionManager session =new SessionManager(Splash.this);

            boolean remember = session.isRemember();
            boolean loggedIn = session.isLoggedIn();

            Intent intent;

            if (user == null) {
                // nu este logat il bagm in main activity
                intent =new Intent(Splash.this,MainActivity.class);
            }else {
                if(session.isRemember()) {
                    //a bifat tine-ma minte
                    intent= new Intent(Splash.this,MainActivity.class);
                }else{
                    //Nu a bifat -> cerem PIN inainte de a contiuna
                    intent = new Intent(Splash.this, PinActivity.class);
                }
            }
            startActivity(intent);
            finish();

        },SPLASH_TIME_OUT);


    }
}