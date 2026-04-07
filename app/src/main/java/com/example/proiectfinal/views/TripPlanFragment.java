package com.example.proiectfinal.views;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proiectfinal.R;
import com.example.proiectfinal.adapters.TripAdapter;
import com.example.proiectfinal.firebase.FirebaseHelper;
import com.example.proiectfinal.models.Trip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TripPlanFragment extends Fragment {

    private RecyclerView recyclerTrips;
    private TextView tvEmptyTrips;
    private FloatingActionButton fabAddTrip;

    private TripAdapter tripAdapter;
    private List<Trip> tripList;
    private FirebaseHelper firebaseHelper;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_plan, container, false);

        recyclerTrips = view.findViewById(R.id.recycler_trips);
        tvEmptyTrips = view.findViewById(R.id.tv_empty_trips);
        fabAddTrip = view.findViewById(R.id.fab_add_trip);

        recyclerTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        tripList = new ArrayList<>();

        firebaseHelper = new FirebaseHelper();
        auth = FirebaseAuth.getInstance();

        // 1. Setăm adaptorul pentru foldere
        tripAdapter = new TripAdapter(getContext(), tripList,
                trip -> {
                    // ACȚIUNE: Ce se întâmplă când dăm click pe un folder
                    // Deschidem pagina cu Detaliile Planului și îi trimitem tot folderul!
                    android.content.Intent intent = new android.content.Intent(getActivity(), TripDetailsActivity.class);
                    intent.putExtra(TripDetailsActivity.EXTRA_TRIP, trip);
                    startActivity(intent);;

                },
                trip -> {
                    // ACȚIUNE: Ce se întâmplă când dăm click pe Ștergere (Gunoi)
                    deleteTrip(trip);
                }
        );
        recyclerTrips.setAdapter(tripAdapter);

        // 2. Acțiunea butonului + (Plus)
        fabAddTrip.setOnClickListener(v -> showAddTripDialog());

        // 3. Citim planurile din Firebase
        loadTripsFromFirebase();

        return view;
    }

    // Funcție care arată un dialog ca să scrii numele noului plan
    private void showAddTripDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Plan Nou de Călătorie");

        final EditText input = new EditText(getContext());
        input.setHint(" Ex: City Break Alba Iulia");
        builder.setView(input);

        builder.setPositiveButton("Creează", (dialog, which) -> {
            String tripName = input.getText().toString().trim();
            if (!tripName.isEmpty()) {
                createNewTrip(tripName);
            } else {
                Toast.makeText(getContext(), "Numele nu poate fi gol!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Anulează", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Salvăm noul plan în Firebase (la nodul "trips")
    private void createNewTrip(String tripName) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference tripsRef = firebaseHelper.getRef().child(user.getUid()).child("trips");
            String tripId = tripsRef.push().getKey(); // Generăm un ID unic garantat de Firebase

            Trip newTrip = new Trip(tripId, tripName);

            if (tripId != null) {
                tripsRef.child(tripId).setValue(newTrip).addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Plan creat cu succes!", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    // Citim planurile live din baza de date
    private void loadTripsFromFirebase() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference tripsRef = firebaseHelper.getRef().child(user.getUid()).child("trips");
            tripsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    tripList.clear();
                    for (DataSnapshot tripSnapshot : snapshot.getChildren()) {
                        Trip trip = tripSnapshot.getValue(Trip.class);
                        if (trip != null) {
                            tripList.add(trip);
                        }
                    }
                    tripAdapter.notifyDataSetChanged();

                    // Dacă lista e goală, arătăm mesajul. Dacă nu, arătăm lista.
                    if (tripList.isEmpty()) {
                        tvEmptyTrips.setVisibility(View.VISIBLE);
                        recyclerTrips.setVisibility(View.GONE);
                    } else {
                        tvEmptyTrips.setVisibility(View.GONE);
                        recyclerTrips.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else {
            // Dacă userul e vizitator neconectat
            tvEmptyTrips.setVisibility(View.VISIBLE);
            tvEmptyTrips.setText("Trebuie să te loghezi pentru a crea planuri.");
            fabAddTrip.setVisibility(View.GONE);
        }
    }

    // Funcția de ștergere a folderului
    private void deleteTrip(Trip trip) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference tripRef = firebaseHelper.getRef().child(user.getUid()).child("trips").child(trip.getId());
            tripRef.removeValue().addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Plan șters!", Toast.LENGTH_SHORT).show();
            });
        }
    }
}