package com.example.proiectfinal.firebase;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.proiectfinal.models.Location;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {

    private final DatabaseReference ref;
    private final FirebaseFirestore db;

    public FirebaseHelper() {
        // Configurarea pentru Useri (Realtime Database) - Rămâne la fel
        ref = FirebaseDatabase.getInstance(
                "https://proiect-final-f448d-default-rtdb.europe-west1.firebasedatabase.app/"
        ).getReference("users");

        // Configurarea pentru Locații (Firestore)
        db = FirebaseFirestore.getInstance();
    }

    public DatabaseReference getRef() {
        return ref;
    }

    // --- INTERFAȚA PENTRU RĂSPUNS (CALLBACK) ---
    // Avem nevoie de asta pentru că citirea din internet durează câteva secunde
    public interface LocationsCallback {
        void onCallback(List<Location> list);
        void onFailure(String error);
    }

    // --- METODA DE CITIRE DIN FIRESTORE ---
    public void readLocations(final LocationsCallback callback) {
        db.collection("locations") // Numele colecției din Console
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Location> locationList = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Firestore transformă automat JSON-ul în obiect Location
                                Location loc = document.toObject(Location.class);
                                locationList.add(loc);
                            }

                            // Trimitem lista înapoi în MainActivity
                            callback.onCallback(locationList);
                        } else {
                            Log.w("Firestore", "Error getting documents.", task.getException());
                            callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                        }
                    }
                });
    }

    // Metodă nouă pentru AI — filtrează locațiile după mai multe categorii
    public void readLocationsByCategories(String[] categories, final LocationsCallback callback) {
        // Convertim array-ul în List pentru Firestore
        java.util.List<String> categoriesList = java.util.Arrays.asList(categories);

        db.collection("locations")
                .whereIn("category", categoriesList)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Location> locationList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Location loc = document.toObject(Location.class);
                                locationList.add(loc);
                            }
                            callback.onCallback(locationList);
                        } else {
                            callback.onFailure(task.getException() != null
                                    ? task.getException().getMessage() : "Eroare Firebase");
                        }
                    }
                });
    }
}