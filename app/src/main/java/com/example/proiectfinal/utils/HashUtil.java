package com.example.proiectfinal.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.util.Base64;

public class HashUtil {

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported!", e);
        }
    }
}
