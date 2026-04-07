package com.example.proiectfinal.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proiectfinal.R;
import com.example.proiectfinal.adapters.LocationAdapter;
import com.example.proiectfinal.models.Location;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerFavorites;
    private TextView tvEmptyFavorites;
    private LocationAdapter favoritesAdapter;
    private List<Location> favoriteLocationsList;

    private FirebaseAuth auth;
    private DatabaseReference myFavoritesRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerFavorites = view.findViewById(R.id.recycler_favorites);
        tvEmptyFavorites = view.findViewById(R.id.tv_empty_favorites);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // Verificăm dacă e logat
        if (currentUser == null) {
            tvEmptyFavorites.setText("Trebuie să te loghezi pentru a vedea favoritele!");
            tvEmptyFavorites.setVisibility(View.VISIBLE);
            return view;
        }

        // Setăm calea către folderul lui de favorite
        myFavoritesRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid()).child("favorites");

        setupRecyclerView();
        fetchFavoritesFromFirebase();

        return view;
    }

    private void setupRecyclerView() {
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        favoriteLocationsList = new ArrayList<>();

        // Magie! Folosim fix același adaptor ca pe Home!
        favoritesAdapter = new LocationAdapter(getContext(), favoriteLocationsList, location -> {
            // La click, deschidem detaliile exact ca pe pagina principală
            Intent intent = new Intent(getActivity(), LocationDetailsActivity.class);
            intent.putExtra(LocationDetailsActivity.EXTRA_LOCATION, location);
            startActivity(intent);
        });
        favoritesAdapter.setHideStats(true);
        recyclerFavorites.setAdapter(favoritesAdapter);
    }

    private void fetchFavoritesFromFirebase() {
        // Folosim ValueEventListener ca să se actualizeze instant dacă dăm "back" după ce am scos o inimioară
        myFavoritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteLocationsList.clear();

                for (DataSnapshot locSnapshot : snapshot.getChildren()) {
                    Location loc = locSnapshot.getValue(Location.class);
                    if (loc != null) {
                        favoriteLocationsList.add(loc);
                    }
                }

                // Verificăm dacă lista e goală pentru a arăta mesajul prietenos
                if (favoriteLocationsList.isEmpty()) {
                    tvEmptyFavorites.setVisibility(View.VISIBLE);
                    recyclerFavorites.setVisibility(View.GONE);
                } else {
                    tvEmptyFavorites.setVisibility(View.GONE);
                    recyclerFavorites.setVisibility(View.VISIBLE);
                }

                // Trimitem datele la adaptor
                favoritesAdapter.updateData(favoriteLocationsList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Eroare la încărcarea favoritelor.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}