package com.example.proiectfinal.views;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.proiectfinal.R;
import com.example.proiectfinal.firebase.FirebaseHelper;
import com.example.proiectfinal.models.Location;
import com.example.proiectfinal.models.Trip;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LocationDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_LOCATION = "extra_location";

    // UI Components
    private TextView tvName, tvDescription, tvCity, tvCategory, tvViews, tvFavoritesCount;
    private ImageView ivImage;
    private ImageButton btnHome, btnFavoriteToggle;
    private Button btnAddToTrip, btnOpenFullMap;

    // Data & Firebase
    private Location currentLocation;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private FirebaseFirestore firestoreDB;

    // Starea Inimioarei
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_details);

        initFirebase();
        initViews();
        getDataFromIntent();

        if (currentLocation != null) {
            displayData();
            setupMiniMap();
            fetchLiveStatsFromFirestore();
            checkIfAlreadyFavorite(); // Verificăm dacă userul a dat deja like în trecut
        } else {
            Toast.makeText(this, "Eroare: Locația nu a putut fi încărcată.", Toast.LENGTH_LONG).show();
            finish();
        }

        setupButtons();
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();
        firestoreDB = FirebaseFirestore.getInstance();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvDetailLocationName);
        tvDescription = findViewById(R.id.tvDetailLocationDescription);
        tvCity = findViewById(R.id.tvDetailLocationCity);
        tvCategory = findViewById(R.id.tvDetailLocationCategory);
        tvViews = findViewById(R.id.tvDetailViews);
        tvFavoritesCount = findViewById(R.id.tvDetailFavoritesCount);
        ivImage = findViewById(R.id.ivDetailLocationImage);

        btnHome = findViewById(R.id.btnHome);
        btnFavoriteToggle = findViewById(R.id.btnFavoriteToggle);
        btnAddToTrip = findViewById(R.id.btnAddToTrip);
        btnOpenFullMap = findViewById(R.id.btnOpenFullMap);
    }

    private void getDataFromIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentLocation = (Location) extras.getSerializable(EXTRA_LOCATION);
        }
    }

    private void displayData() {
        tvName.setText(currentLocation.getName());
        tvDescription.setText(currentLocation.getDescription());
        tvCity.setText("📍 "+ currentLocation.getCity() + ", " + currentLocation.getCounty());
        tvCategory.setText(currentLocation.getCategory());

        if (currentLocation.getImageUrl() != null && !currentLocation.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentLocation.getImageUrl())
                    .placeholder(R.drawable.image_not_loaded)
                    .centerCrop()
                    .into(ivImage);
        }
    }

    private void setupButtons() {
        btnHome.setOnClickListener(v -> finish());
        btnOpenFullMap.setOnClickListener(v -> openInGoogleMapsApp());
        btnAddToTrip.setOnClickListener(v -> fetchTripsAndShowDialog()); // Logica ta veche, intactă!
        btnFavoriteToggle.setOnClickListener(v -> handleFavoriteToggle());
    }

    // ==========================================
    // MAGIA MINI-HĂRȚII
    // ==========================================
    private void setupMiniMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDetail);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (currentLocation.getLatitude() != 0.0 && currentLocation.getLongitude() != 0.0) {
            LatLng position = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(position).title(currentLocation.getName()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14f));
            googleMap.getUiSettings().setScrollGesturesEnabled(false); // Fără scroll accidental pe hartă
        }
    }

    private void openInGoogleMapsApp() {
        if (currentLocation.getLatitude() == 0.0 || currentLocation.getLongitude() == 0.0) return;
        Uri gmmIntentUri = Uri.parse("geo:" + currentLocation.getLatitude() + "," + currentLocation.getLongitude() + "?q=" + currentLocation.getLatitude() + "," + currentLocation.getLongitude() + "(" + Uri.encode(currentLocation.getName()) + ")");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) startActivity(mapIntent);
        else Toast.makeText(this, "Google Maps nu este instalat.", Toast.LENGTH_SHORT).show();
    }



    // Citește Live din Firestore vizualizările și numărul total de aprecieri
    private void fetchLiveStatsFromFirestore() {
        if (currentLocation.getId() == null) return;

        firestoreDB.collection("locations").document(currentLocation.getId())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    // 1. Vizualizări
                    Long viewsLong = snapshot.getLong("views");
                    int views = (viewsLong != null) ? viewsLong.intValue() : 0;
                    tvViews.setText(formatNumber(views) + " vizualizări");

                    // 2. Favorite
                    Long favsLong = snapshot.getLong("favoritesCount");
                    int favs = (favsLong != null) ? favsLong.intValue() : 0;
                    tvFavoritesCount.setText(formatNumber(favs) + " favorite");
                });
    }

    // Verifică dacă userul curent are deja locația la favorite ca să îi colorăm inima în Roșu la deschidere
    private void checkIfAlreadyFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || currentLocation.getId() == null) return;

        DatabaseReference favRef = firebaseHelper.getRef().child(user.getUid()).child("favorites").child(currentLocation.getId());
        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isFavorite = true;
                    btnFavoriteToggle.setColorFilter(android.graphics.Color.parseColor("#E53935")); // Roșu
                } else {
                    isFavorite = false;
                    btnFavoriteToggle.setColorFilter(android.graphics.Color.WHITE); // Alb
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Acțiunea când apasă pe inimioară
    private void handleFavoriteToggle() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Trebuie să fii logat pentru a adăuga la favorite!", Toast.LENGTH_SHORT).show();
            return;
        }

        isFavorite = !isFavorite;
        DatabaseReference myFavRef = firebaseHelper.getRef().child(user.getUid()).child("favorites").child(currentLocation.getId());

        if (isFavorite) {
            // 1. O facem ROȘIE
            btnFavoriteToggle.setColorFilter(android.graphics.Color.parseColor("#E53935"));
            Toast.makeText(this, "Adăugat la favorite ❤️", Toast.LENGTH_SHORT).show();

            // 2. Salvăm toată locația la profilul lui (pentru a o folosi ușor în FavoritesFragment)
            myFavRef.setValue(currentLocation);

            // 3. Adăugăm +1 la contorul global din Firestore
            firestoreDB.collection("locations").document(currentLocation.getId())
                    .update("favoritesCount", FieldValue.increment(1));
        } else {
            // 1. O facem ALBĂ
            btnFavoriteToggle.setColorFilter(android.graphics.Color.WHITE);
            Toast.makeText(this, "Șters din favorite 🤍", Toast.LENGTH_SHORT).show();

            // 2. Ștergem din profilul lui
            myFavRef.removeValue();

            // 3. Scădem -1 din contorul global din Firestore
            firestoreDB.collection("locations").document(currentLocation.getId())
                    .update("favoritesCount", FieldValue.increment(-1));
        }
    }


    // ==========================================
    // LOGICA PENTRU PLANURI MULTIPLE (Cea veche, intactă)
    // ==========================================
    private void fetchTripsAndShowDialog() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Trebuie sa fii logat pentru a salva o locatie.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference tripsRef = firebaseHelper.getRef().child(user.getUid()).child("trips");

        tripsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Trip> tripList = new ArrayList<>();
                List<Boolean> alreadyAddedList = new ArrayList<>();

                for (DataSnapshot tripSnapshot : snapshot.getChildren()) {
                    Trip trip = tripSnapshot.getValue(Trip.class);
                    if (trip != null) {
                        tripList.add(trip);
                        boolean isAlreadyInTrip = tripSnapshot.child("locations").hasChild(currentLocation.getId());
                        alreadyAddedList.add(isAlreadyInTrip);
                    }
                }

                if (tripList.isEmpty()) showCreateTripDialog();
                else showTripSelectionDialog(tripList, alreadyAddedList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LocationDetailsActivity.this, "Eroare la citire planuri", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTripSelectionDialog(List<Trip> trips, List<Boolean> alreadyAddedList) {
        String[] tripDisplayNames = new String[trips.size()];
        for (int i = 0; i < trips.size(); i++) {
            if (alreadyAddedList.get(i)) tripDisplayNames[i] = trips.get(i).getName() + "  ✅";
            else tripDisplayNames[i] = trips.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("În care plan vrei să o salvezi?");
        builder.setItems(tripDisplayNames, (dialog, which) -> {
            if (alreadyAddedList.get(which)) {
                Toast.makeText(this, "Locația este deja în acest plan!", Toast.LENGTH_SHORT).show();
            } else {
                Trip selectedTrip = trips.get(which);
                saveLocationToSpecificTrip(selectedTrip.getId(), selectedTrip.getName());
            }
        });

        builder.setNeutralButton("+ Creează Plan Nou", (dialog, which) -> showCreateTripDialog());
        builder.setNegativeButton("Anulează", null);
        builder.show();
    }

    private void showCreateTripDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nu ai niciun plan! Creează unul acum:");

        final EditText input = new EditText(this);
        input.setHint(" Ex: Excursie Munte");
        builder.setView(input);

        builder.setPositiveButton("Creează și Salvează", (dialog, which) -> {
            String tripName = input.getText().toString().trim();
            if (!tripName.isEmpty()) createNewTripAndAddLocation(tripName);
            else Toast.makeText(this, "Numele nu poate fi gol!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Anulează", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void createNewTripAndAddLocation(String tripName) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference tripsRef = firebaseHelper.getRef().child(user.getUid()).child("trips");
            String tripId = tripsRef.push().getKey();

            if (tripId != null) {
                Trip newTrip = new Trip(tripId, tripName);
                tripsRef.child(tripId).setValue(newTrip).addOnSuccessListener(aVoid -> saveLocationToSpecificTrip(tripId, tripName));
            }
        }
    }

    private void saveLocationToSpecificTrip(String tripId, String tripName) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference locationRef = firebaseHelper.getRef()
                    .child(user.getUid())
                    .child("trips")
                    .child(tripId)
                    .child("locations")
                    .child(currentLocation.getId());

            locationRef.setValue(currentLocation).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, currentLocation.getName() + " salvată în " + tripName + "!", Toast.LENGTH_LONG).show();
            });
        }
    }
    // Algoritmul pentru prescurtarea numerelor (ex: 1500 -> 1.5k)
    private String formatNumber(int number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            String formatted = String.format(java.util.Locale.US, "%.1fk", number / 1000.0);
            return formatted.replace(".0k", "k");
        } else {
            String formatted = String.format(java.util.Locale.US, "%.1fM", number / 1000000.0);
            return formatted.replace(".0M", "M");
        }
    }
}