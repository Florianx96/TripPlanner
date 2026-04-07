package com.example.proiectfinal.utils;

import android.content.Context;
import android.content.SharedPreferences;
public class SessionManager {
    private static final String PREF_NAME="user_session";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER= "remember";
    private static final String KEY_FINGERPRINT= "use_fingerprint";
    private static final String KEY_LOGGED_IN = "logged_in";


    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    public void saveSession(String email,boolean remember) {
        editor.putString(KEY_EMAIL, email);
        editor.putBoolean(KEY_REMEMBER, remember);
        editor.apply();
    }

    public void setLoggedIn(boolean loggedIn){
        editor.putBoolean(KEY_LOGGED_IN, loggedIn);
        editor.apply();
    }
    public boolean isLoggedIn(){
        return prefs.getBoolean(KEY_LOGGED_IN,false);
    }
    public boolean isRemember() {
        return prefs.getBoolean(KEY_REMEMBER,false);
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public void savePin(String pin) {
        if (pin == null) {
            editor.remove("PIN_CODE");
        } else {
            editor.putString("PIN_CODE", pin);
        }
        editor.apply();
    }

    public String getPin() {
        return prefs.getString("PIN_CODE",null);
    }
    public boolean hasPin() {
        return prefs.contains("PIN_CODE");
    }

    public void setUseFingerprint(boolean enable) {
        editor.putBoolean(KEY_FINGERPRINT,enable);
        editor.apply();
    }
    public boolean isFingerprintEnabled() {
        return prefs.getBoolean(KEY_FINGERPRINT,false);
    }
}
