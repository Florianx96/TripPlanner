package com.example.proiectfinal.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proiectfinal.R;
import com.example.proiectfinal.adapters.LocationAdapter;
import com.example.proiectfinal.firebase.FirebaseHelper;
import com.example.proiectfinal.models.Location;
import com.example.proiectfinal.models.Trip;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TripDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP = "extra_trip";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private TextView tvTitle, tvEmpty;
    private RecyclerView recyclerLocations;
    private Button btnViewMap;
    private Button btnAutoSort;
    private ImageButton btnBack;

    // UI Punct de plecare
    private TextView tvStartAddress;
    private Button btnGetLocation;

    private Trip currentTrip;
    private LocationAdapter locationAdapter;
    private List<Location> locationList;

    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;

    // Google GPS Client
    private FusedLocationProviderClient fusedLocationClient;
    private double startLat = 0.0;
    private double startLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);

        currentTrip = (Trip) getIntent().getSerializableExtra(EXTRA_TRIP);
        if (currentTrip == null) {
            Toast.makeText(this, "Eroare: Plan invalid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvTitle = findViewById(R.id.tv_trip_detail_title);
        tvEmpty = findViewById(R.id.tv_empty_trip_locations);
        recyclerLocations = findViewById(R.id.recycler_trip_locations);
        btnViewMap = findViewById(R.id.btn_view_map);
        btnBack = findViewById(R.id.btn_back_trip);

        btnAutoSort = findViewById(R.id.btn_auto_sort);
        btnAutoSort.setOnClickListener(v -> sortLocationsAutomatically());

        // Initializare UI GPS
        tvStartAddress = findViewById(R.id.tv_start_location_address);
        btnGetLocation = findViewById(R.id.btn_get_my_location);

        tvTitle.setText(currentTrip.getName());
        btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        loadLocationsFromFolder();


        // Verificăm automat dacă avem deja permisiunea de locație
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Dacă a dat permisiunea în altă parte a aplicației, o luăm direct!
            getCurrentLocation();
        } else {
            // Dacă nu, lăsăm textul default și așteptăm să apese el
            tvStartAddress.setText("Punct de plecare nestabilit");
        }

        // Când apeși pe Modifică locația de start
        btnGetLocation.setOnClickListener(v -> showStartLocationDialog());

        btnViewMap.setOnClickListener(v -> {
            if (locationList.isEmpty()) {
                Toast.makeText(this, "Adaugă locații înainte să vezi harta!", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, TripMapActivity.class);
                intent.putExtra("locations_list", new ArrayList<>(locationList));
                // Trimitem si coordonatele de plecare catre harta!
                intent.putExtra("start_lat", startLat);
                intent.putExtra("start_lng", startLng);
                startActivity(intent);
            }
        });
    }

    // ---  DRAG & DROP ---
    private void setupRecyclerView() {
        recyclerLocations.setLayoutManager(new LinearLayoutManager(this));
        locationList = new ArrayList<>();

        locationAdapter = new LocationAdapter(this, locationList, location -> {
            Intent intent = new Intent(this, LocationDetailsActivity.class);
            intent.putExtra(LocationDetailsActivity.EXTRA_LOCATION, location);
            startActivity(intent);
        });

        locationAdapter.setOnLocationDeleteListener(location -> deleteLocationFromTrip(location));
        recyclerLocations.setAdapter(locationAdapter);

        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback simpleCallback = new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                androidx.recyclerview.widget.ItemTouchHelper.UP | androidx.recyclerview.widget.ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                java.util.Collections.swap(locationList, fromPosition, toPosition);
                locationAdapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public void onSelectedChanged(@androidx.annotation.Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.setScaleX(1.03f);
                    viewHolder.itemView.setScaleY(1.03f);
                    viewHolder.itemView.setAlpha(0.7f);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setScaleX(1.0f);
                viewHolder.itemView.setScaleY(1.0f);
                viewHolder.itemView.setAlpha(1.0f);
                saveNewOrderToFirebase();
            }
        };
        new androidx.recyclerview.widget.ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerLocations);
    }

    private void saveNewOrderToFirebase() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference locationsRef = firebaseHelper.getRef().child(user.getUid()).child("trips").child(currentTrip.getId()).child("locations");
            for (int i = 0; i < locationList.size(); i++) {
                Location loc = locationList.get(i);
                loc.setOrderIndex(i);
                locationsRef.child(loc.getId()).setValue(loc);
            }
        }
    }

    private void loadLocationsFromFolder() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference locationsRef = firebaseHelper.getRef().child(user.getUid()).child("trips").child(currentTrip.getId()).child("locations");
            locationsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Location> tempNewList = new ArrayList<>();
                    for (DataSnapshot locSnap : snapshot.getChildren()) {
                        Location loc = locSnap.getValue(Location.class);
                        if (loc != null) tempNewList.add(loc);
                    }
                    java.util.Collections.sort(tempNewList, (l1, l2) -> Integer.compare(l1.getOrderIndex(), l2.getOrderIndex()));

                    locationAdapter.updateData(tempNewList);
                    locationList.clear();
                    locationList.addAll(tempNewList);

                    if (tempNewList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerLocations.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerLocations.setVisibility(View.VISIBLE);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    private void deleteLocationFromTrip(Location location) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference locRef = firebaseHelper.getRef().child(user.getUid()).child("trips").child(currentTrip.getId()).child("locations").child(location.getId());
            locRef.removeValue().addOnSuccessListener(aVoid -> Toast.makeText(this, "Locație ștearsă!", Toast.LENGTH_SHORT).show());
        }
    }

    // --- SISTEMUL GPS (PUNCTUL DE PLECARE) ---

    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Cerem permisiunea (apare pop-up-ul acela de sistem)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Avem voie, deci pornim căutarea
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            Toast.makeText(this, "Avem nevoie de locație pentru a calcula traseul de la tine.", Toast.LENGTH_LONG).show();
            tvStartAddress.setText("Locație respinsă");
        }
    }

    @SuppressWarnings("MissingPermission")
    private void getCurrentLocation() {
        tvStartAddress.setText("Se caută locația ta...");
        // Extragem locația hardware
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                startLat = location.getLatitude();
                startLng = location.getLongitude();
                // O transformăm în Nume de Oraș / Stradă
                getAddressFromLocation(startLat, startLng);
            } else {
                tvStartAddress.setText("Asigură-te că GPS-ul e pornit");
            }
        });
    }

    // Reverse Geocoding: De la numere la Text
    private void getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address obj = addresses.get(0);
                String city = obj.getLocality();
                String street = obj.getThoroughfare();

                if (city != null && street != null) {
                    tvStartAddress.setText(street + ", " + city);
                } else if (city != null) {
                    tvStartAddress.setText(city);
                } else {
                    tvStartAddress.setText(obj.getAddressLine(0));
                }
            } else {
                tvStartAddress.setText("Locație găsită (fără adresă exactă)");
            }
        } catch (IOException e) {
            e.printStackTrace();
            tvStartAddress.setText("Locație găsită (eroare rețea)");
        }
    }

    // Algoritmul NEAREST NEIGHBOR (Cel mai apropiat vecin)
    private void sortLocationsAutomatically() {
        if (locationList == null || locationList.size() < 2) {
            Toast.makeText(this, "Ai nevoie de cel puțin 2 locații pentru a optimiza traseul.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Location> unvisited = new ArrayList<>(locationList);
        List<Location> optimizedRoute = new ArrayList<>();

        double currentLat = startLat;
        double currentLng = startLng;

        // Dacă GPS-ul e oprit / Punctul de plecare lipsește, luăm prima atracție ca punct de start
        if (currentLat == 0.0 || currentLng == 0.0) {
            currentLat = unvisited.get(0).getLatitude();
            currentLng = unvisited.get(0).getLongitude();
        }

        while (!unvisited.isEmpty()) {
            Location nearest = null;
            double minDistance = Double.MAX_VALUE;

            // Căutăm care e cea mai apropiată locație de noi în acest moment
            for (Location loc : unvisited) {
                float[] results = new float[1];
                android.location.Location.distanceBetween(
                        currentLat, currentLng,
                        loc.getLatitude(), loc.getLongitude(),
                        results
                );
                double distance = results[0];

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = loc;
                }
            }

            // Mutăm locația găsită în lista optimizată
            if (nearest != null) {
                optimizedRoute.add(nearest);
                unvisited.remove(nearest);
                // "Sărim" la ea pentru a o căuta pe următoarea din acest punct
                currentLat = nearest.getLatitude();
                currentLng = nearest.getLongitude();
            }
        }

        // Actualizăm interfața cu noua listă sortată
        locationList.clear();
        locationList.addAll(optimizedRoute);
        locationAdapter.updateData(locationList);

        // Salvăm instant în Firebase! (Funcția asta o aveam deja scrisă de la Drag&Drop)
        saveNewOrderToFirebase();

        Toast.makeText(this, "Traseu optimizat cu succes! ⚡", Toast.LENGTH_SHORT).show();
    }

    // ==========================================
    // PUNCT DE PLECARE FLEXIBIL
    // ==========================================
    private void showStartLocationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Punct de Plecare");
        builder.setMessage("De unde dorești să începi acest traseu?");

        // Creăm un câmp de text unde poate scrie orașul
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(" Ex: Brașov, București...");

        // Adăugăm puțin spațiu stânga-dreapta pentru aspect
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        // Opțiunea 1: Oraș introdus manual
        builder.setPositiveButton("Caută Oraș", (dialog, which) -> {
            String cityName = input.getText().toString().trim();
            if (!cityName.isEmpty()) {
                getCoordinatesFromCityName(cityName);
            } else {
                Toast.makeText(this, "Te rog să introduci un oraș!", Toast.LENGTH_SHORT).show();
            }
        });

        // Opțiunea 2: Folosește senzorul GPS
        builder.setNeutralButton("Folosește GPS", (dialog, which) -> {
            checkLocationPermissionAndGetLocation(); // Funcția pe care o aveam deja!
        });

        builder.setNegativeButton("Anulează", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Funcția care transformă Textul (Ex: "Cluj") în Coordonate GPS
    private void getCoordinatesFromCityName(String cityName) {
        tvStartAddress.setText("Se caută: " + cityName + "...");

        android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());

        // Android necesită ca apelurile de rețea/căutare să fie protejate de un try-catch
        try {
            java.util.List<android.location.Address> addresses = geocoder.getFromLocationName(cityName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address address = addresses.get(0);

                // Am găsit orașul! Salvăm coordonatele în variabilele noastre
                startLat = address.getLatitude();
                startLng = address.getLongitude();

                // Actualizăm interfața
                tvStartAddress.setText( cityName + " (Manual)");
                Toast.makeText(this, "Punct de plecare actualizat!", Toast.LENGTH_SHORT).show();

            } else {
                tvStartAddress.setText("Oraș negăsit");
                Toast.makeText(this, "Nu am putut găsi acest oraș. Încearcă alt nume.", Toast.LENGTH_LONG).show();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            tvStartAddress.setText("Eroare de conexiune");
            Toast.makeText(this, "Verifică conexiunea la internet!", Toast.LENGTH_SHORT).show();
        }
    }
}