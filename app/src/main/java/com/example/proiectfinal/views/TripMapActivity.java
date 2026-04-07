package com.example.proiectfinal.views;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.proiectfinal.R;
import com.example.proiectfinal.models.Location;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TripMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ArrayList<Location> locationsToDisplay;
    private double startLat, startLng;
    private GoogleMap mMap;

    // UI Elements
    private CardView cardRouteInfo;
    private TextView tvDistance, tvDuration;
    private ExtendedFloatingActionButton btnFuelCalculator, btnNavigate;

    // Variabile pentru traseu
    private double totalDistanceKm = 0.0;
    private String apiKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_map);

        locationsToDisplay = (ArrayList<Location>) getIntent().getSerializableExtra("locations_list");
        startLat = getIntent().getDoubleExtra("start_lat", 0.0);
        startLng = getIntent().getDoubleExtra("start_lng", 0.0);

        if (locationsToDisplay == null || locationsToDisplay.isEmpty()) {
            Toast.makeText(this, "Nu există locații de afișat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Folosim noua cheie dedicată strict pentru Directions API (Rute)
        apiKey = getString(R.string.directions_api_key);

        // Inițializare UI
        cardRouteInfo = findViewById(R.id.card_route_info);
        tvDistance = findViewById(R.id.tv_route_distance);
        tvDuration = findViewById(R.id.tv_route_duration);
        btnFuelCalculator = findViewById(R.id.btn_fuel_calculator);
        btnNavigate = findViewById(R.id.btn_start_navigation);
        ImageButton btnBack = findViewById(R.id.btn_back_from_map);

        btnBack.setOnClickListener(v -> finish());
        btnNavigate.setOnClickListener(v -> openGoogleMapsForNavigation());
        btnFuelCalculator.setOnClickListener(v -> openFuelCalculator());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Unealta magică de la Google pentru a crea Pioni cu Text!
        com.google.maps.android.ui.IconGenerator iconFactory = new com.google.maps.android.ui.IconGenerator(this);

        // 1. Plasăm Punctul de Plecare (Start)
        if (startLat != 0.0 && startLng != 0.0) {
            LatLng startPos = new LatLng(startLat, startLng);

            // Facem iconița de start Verde
            iconFactory.setStyle(com.google.maps.android.ui.IconGenerator.STYLE_GREEN);
            android.graphics.Bitmap startBitmap = iconFactory.makeIcon("START");

            mMap.addMarker(new MarkerOptions()
                    .position(startPos)
                    .title("Punct de Plecare")
                    .icon(BitmapDescriptorFactory.fromBitmap(startBitmap)));

            builder.include(startPos);
        }

        // 2. Plasăm Atracțiile cu numere (1, 2, 3...)
        iconFactory.setStyle(com.google.maps.android.ui.IconGenerator.STYLE_BLUE); // Le facem Albastre sau STYLE_RED
        int stepNumber = 1; // Contorul nostru

        for (Location loc : locationsToDisplay) {
            if (loc.getLatitude() != 0.0 && loc.getLongitude() != 0.0) {
                LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());

                // Generăm imaginea cu numărul pasului curent
                android.graphics.Bitmap numberBitmap = iconFactory.makeIcon(String.valueOf(stepNumber));

                mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(stepNumber + ". " + loc.getName()) // Adăugăm numărul și în titlu
                        .snippet(loc.getCategory())
                        .icon(BitmapDescriptorFactory.fromBitmap(numberBitmap)));

                builder.include(pos);
                stepNumber++; // Creștem numărul pentru următoarea atracție
            }
        }

        // 3. Centrarea camerei (rămâne la fel)
        mMap.setOnMapLoadedCallback(() -> {
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 4. APELĂM GOOGLE DIRECTIONS API PENTRU DRUMURI REALE (rămâne la fel)
        fetchRealRouteFromGoogle();
    }

    // ==========================================
    //  RUTARE (DIRECTIONS API)
    // ==========================================
    private void fetchRealRouteFromGoogle() {
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e("MapDebug", "API Key-ul este GOAL! Verifica strings.xml");
            return;
        }

        // Punctul de Origine
        String origin = startLat + "," + startLng;
        if (startLat == 0.0 || startLng == 0.0) {
            origin = locationsToDisplay.get(0).getLatitude() + "," + locationsToDisplay.get(0).getLongitude();
        }

        // Punctul Final (Ultima atracție)
        Location lastLoc = locationsToDisplay.get(locationsToDisplay.size() - 1);
        String destination = lastLoc.getLatitude() + "," + lastLoc.getLongitude();

        // Opririle Intermediare (Waypoints)
        StringBuilder waypoints = new StringBuilder("optimize:false");
        for (int i = 0; i < locationsToDisplay.size() - 1; i++) {
            Location loc = locationsToDisplay.get(i);
            if (startLat == 0.0 && i == 0) continue;
            waypoints.append("|").append(loc.getLatitude()).append(",").append(loc.getLongitude());
        }

        String url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + origin +
                "&destination=" + destination +
                "&waypoints=" + waypoints.toString() +
                "&key=" + apiKey;

        Log.d("MapDebug", "Trimit cererea la URL-ul: " + url); // Sa vedem daca link-ul e construit bine

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MapDebug", "Eroare FIZICA de rețea (Fara net?): " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d("MapDebug", "RASPUNS GOOGLE: " + responseData); // AICI VEDEM ADEVARUL!
                    parseDirectionsJSON(responseData);
                } else {
                    Log.e("MapDebug", "Eroare HTTP: " + response.code());
                }
            }
        });
    }

    private void parseDirectionsJSON(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String status = jsonObject.getString("status");

            if (status.equals("ZERO_RESULTS")) {
                // Dacă drumul rutier e imposibil, desenăm linia dreaptă!
                drawStraightLineFallback();
                return;
            }

            if (!status.equals("OK")) {
                Log.e("MapDebug", "Eroare Google Status: " + status);
                return;
            }

            JSONArray routes = jsonObject.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONArray legs = route.getJSONArray("legs");
                int totalDistMeters = 0;
                int totalDurationSec = 0;

                for (int i = 0; i < legs.length(); i++) {
                    JSONObject leg = legs.getJSONObject(i);
                    totalDistMeters += leg.getJSONObject("distance").getInt("value");
                    totalDurationSec += leg.getJSONObject("duration").getInt("value");
                }

                totalDistanceKm = totalDistMeters / 1000.0;
                String finalDistance = String.format("%.1f", totalDistanceKm) + " km";
                String finalDuration = (totalDurationSec / 3600) + "h " + ((totalDurationSec % 3600) / 60) + "m";

                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPath = overviewPolyline.getString("points");
                List<LatLng> decodedPath = PolyUtil.decode(encodedPath);

                new Handler(Looper.getMainLooper()).post(() -> {
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(decodedPath)
                            .width(14f)
                            .color(Color.parseColor("#00ADC7"))
                            .geodesic(true);
                    mMap.addPolyline(polylineOptions);

                    tvDistance.setText(finalDistance);
                    tvDuration.setText(finalDuration);
                    cardRouteInfo.setVisibility(View.VISIBLE);
                    btnFuelCalculator.setVisibility(View.VISIBLE);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   // Metoda care desenează linii drepte între puncte (Fallback)
    private void drawStraightLineFallback() {
        new Handler(Looper.getMainLooper()).post(() -> {
            PolylineOptions options = new PolylineOptions()
                    .width(8f)
                    .color(Color.GRAY)
                    // Facem linia punctată pentru a indica un traseu "estimat"
                    .pattern(java.util.Arrays.asList(new com.google.android.gms.maps.model.Dash(20), new com.google.android.gms.maps.model.Gap(20)));

            if (startLat != 0.0 && startLng != 0.0) {
                options.add(new LatLng(startLat, startLng));
            }

            for (Location loc : locationsToDisplay) {
                options.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
            }

            mMap.addPolyline(options);

            // Calculăm o distanță aeriană brută pentru info card
            tvDistance.setText("Traseu direct");
            tvDuration.setText("N/A");
            cardRouteInfo.setVisibility(View.VISIBLE);

            Toast.makeText(this, "Drum rutier indisponibil. Afișăm traseul direct.", Toast.LENGTH_LONG).show();
        });
    }

    // ==========================================
    // CALCULATORUL DE COSTURI
    // ==========================================
    private void openFuelCalculator() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        // 1. Încărcăm design-ul pe care tocmai l-am creat
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_fuel_calculator, null);
        dialog.setContentView(bottomSheetView);

        // 2. Găsim elementele din noul XML
        EditText etConsumption = bottomSheetView.findViewById(R.id.et_fuel_consumption);
        EditText etPrice = bottomSheetView.findViewById(R.id.et_fuel_price);
        Button btnCalc = bottomSheetView.findViewById(R.id.btn_calculate_cost);
        TextView tvResult = bottomSheetView.findViewById(R.id.tv_calc_result);

        // 3. Setăm ce se întâmplă când apasă pe buton
        btnCalc.setOnClickListener(v -> {
            String consStr = etConsumption.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (consStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Completează ambele câmpuri!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Transformăm textul în numere cu virgulă (Double)
                double consumption = Double.parseDouble(consStr);
                double price = Double.parseDouble(priceStr);

                // Formula magică: (Distanță / 100) * Consum * Preț_Litru
                double totalCost = (totalDistanceKm / 100.0) * consumption * price;

                // Afișăm rezultatul și facem textul vizibil
                String rezultat = String.format("Cost estimat: %.2f RON\n(pentru %.1f km)", totalCost, totalDistanceKm);
                tvResult.setText(rezultat);
                tvResult.setVisibility(View.VISIBLE);

            } catch (Exception e) {
                // În caz că utilizatorul introduce virgula "," în loc de punct "."
                Toast.makeText(this, "Introduceți numere valide (folosiți punctul . pentru zecimale)", Toast.LENGTH_LONG).show();
            }
        });

        // 4. Afișăm fereastra
        dialog.show();
    }


    //MODIFICĂM openGoogleMapsForNavigation pentru a deveni un SELECTOR (Maps vs Waze)
    private void openGoogleMapsForNavigation() {
        if (locationsToDisplay.isEmpty()) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Alege aplicația de navigare");
        String[] apps = {"Google Maps (Traseu complet)", "Waze (Către prima locație)"};

        builder.setItems(apps, (dialog, which) -> {
            if (which == 0) {
                // Opțiunea Google Maps (Suportă mai multe opriri)
                launchGoogleMaps();
            } else {
                // Opțiunea Waze (Suportă doar o destinație o dată)
                launchWaze();
            }
        });
        builder.show();
    }

    private void launchGoogleMaps() {
        StringBuilder urlBuilder = new StringBuilder("https://www.google.com/maps/dir/?api=1");

        // Originea
        String origin = (startLat != 0.0) ? startLat + "," + startLng : locationsToDisplay.get(0).getLatitude() + "," + locationsToDisplay.get(0).getLongitude();
        urlBuilder.append("&origin=").append(origin);

        // Destinația (Ultima din listă)
        Location last = locationsToDisplay.get(locationsToDisplay.size() - 1);
        urlBuilder.append("&destination=").append(last.getLatitude()).append(",").append(last.getLongitude());

        // Opriri intermediare
        if (locationsToDisplay.size() > 1) {
            urlBuilder.append("&waypoints=");
            for (int i = 0; i < locationsToDisplay.size() - 1; i++) {
                Location loc = locationsToDisplay.get(i);
                urlBuilder.append(loc.getLatitude()).append(",").append(loc.getLongitude()).append("|");
            }
        }
        urlBuilder.append("&travelmode=driving");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlBuilder.toString()));
        intent.setPackage("com.google.android.apps.maps");
        startActivity(intent);
    }

    private void launchWaze() {
        // Waze ne permite să trimitem utilizatorul către o singură locație prin URL.
        // Îl trimitem către PRIMA locație din planul tău.
        Location firstTarget = locationsToDisplay.get(0);
        String wazeUrl = "https://waze.com/ul?ll=" + firstTarget.getLatitude() + "," + firstTarget.getLongitude() + "&navigate=yes";

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(wazeUrl));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Waze nu este instalat!", Toast.LENGTH_SHORT).show();
            // Fallback la browser dacă nu are aplicația
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(wazeUrl)));
        }
    }
}
