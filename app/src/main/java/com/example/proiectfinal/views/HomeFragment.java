package com.example.proiectfinal.views;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.proiectfinal.R;
import com.example.proiectfinal.adapters.LocationAdapter;
import com.example.proiectfinal.adapters.LocationCarouselAdapter;
import com.example.proiectfinal.firebase.FirebaseHelper;
import com.example.proiectfinal.models.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerToate;
    private LocationAdapter locationAdapter;
    private List<Location> locationList;

    // UI Carusel
    private ViewPager2 viewPagerRecomandate;
    private LocationCarouselAdapter carouselAdapter;
    private TextView tvCarouselTitle;
    private ImageButton btnToggleGps;
    private TabLayout tabLayoutDots;
    private ProgressBar progressGps; // Cercul de Loading
    private boolean isGpsEnabled = false;

    // GPS & Locație
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 0.0;
    private double userLng = 0.0;
    private boolean hasUserLocation = false;

    // Launcher modern pentru a cere Permisiunea în timp real
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    fetchUserLocationAndFilter();
                } else {
                    Toast.makeText(getContext(), "Avem nevoie de permisiunea de locație pentru a găsi destinații apropiate.", Toast.LENGTH_LONG).show();
                    isGpsEnabled = false;
                    updateGpsUi();
                }
            });

    // Logica Auto-Swipe
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPagerRecomandate != null && carouselAdapter != null) {
                int count = carouselAdapter.getItemCount();
                if (count > 0) {
                    int nextItem = (viewPagerRecomandate.getCurrentItem() + 1) % count;
                    viewPagerRecomandate.setCurrentItem(nextItem, true);
                }
            }
        }
    };

    private List<Location> fullLocationsList = new ArrayList<>();
    private android.widget.TextView chipTurism, chipFood, chipRelax;
    private String currentMainCategory = "Turism";
    private FirebaseHelper firebaseHelper;

    private String currentSearchText = "";
    private String currentCategory = "Toate";
    private String currentCounty = "Toate";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Inițializăm GPS Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // --- Setare Profil ---
        TextView tvWelcome = view.findViewById(R.id.tv_welcome_message);
        android.widget.ImageView btnSettings = view.findViewById(R.id.btn_settings);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty() ? currentUser.getDisplayName() : "Călătorule";
            tvWelcome.setText("Salut, " + name + "!");
            if (currentUser.getPhotoUrl() != null) {
                com.bumptech.glide.Glide.with(this).load(currentUser.getPhotoUrl()).circleCrop().into(btnSettings);
            }
        }
        btnSettings.setOnClickListener(v -> startActivity(new android.content.Intent(getActivity(), SettingsActivity.class)));

        // --- Configurare Chips ---
        chipTurism = view.findViewById(R.id.chip_turism);
        chipFood = view.findViewById(R.id.chip_food);
        chipRelax = view.findViewById(R.id.chip_relax);
        chipTurism.setOnClickListener(v -> selectMainCategory("Turism"));
        chipFood.setOnClickListener(v -> selectMainCategory("Food"));
        chipRelax.setOnClickListener(v -> selectMainCategory("Relax"));

        recyclerToate = view.findViewById(R.id.recycler_toate);
        viewPagerRecomandate = view.findViewById(R.id.viewPager_recomandate);
        tvCarouselTitle = view.findViewById(R.id.tv_carousel_title_main);
        btnToggleGps = view.findViewById(R.id.btn_toggle_gps);
        tabLayoutDots = view.findViewById(R.id.tabLayout_dots);

        // Căutăm ProgressBar-ul adăugat în XML anterior
        progressGps = view.findViewById(R.id.progress_gps);

        ImageButton btnPrev = view.findViewById(R.id.btn_prev_carousel);
        ImageButton btnNext = view.findViewById(R.id.btn_next_carousel);

        // --- Lista Verticală ---
        recyclerToate.setLayoutManager(new LinearLayoutManager(getContext()));
        locationList = new ArrayList<>();
        locationAdapter = new LocationAdapter(getContext(), locationList, location -> {
            android.content.Intent intent = new android.content.Intent(getActivity(), LocationDetailsActivity.class);
            intent.putExtra(LocationDetailsActivity.EXTRA_LOCATION, location);
            startActivity(intent);
        });
        recyclerToate.setAdapter(locationAdapter);

        // --- Caruselul ---
        carouselAdapter = new LocationCarouselAdapter(getContext(), new ArrayList<>(), location -> {
            android.content.Intent intent = new android.content.Intent(getActivity(), LocationDetailsActivity.class);
            intent.putExtra(LocationDetailsActivity.EXTRA_LOCATION, location);
            startActivity(intent);
        });
        viewPagerRecomandate.setAdapter(carouselAdapter);

        new TabLayoutMediator(tabLayoutDots, viewPagerRecomandate, (tab, position) -> {}).attach();

        // Control Săgeți
        btnPrev.setOnClickListener(v -> {
            int current = viewPagerRecomandate.getCurrentItem();
            if (current > 0) viewPagerRecomandate.setCurrentItem(current - 1, true);
            else viewPagerRecomandate.setCurrentItem(carouselAdapter.getItemCount() - 1, true);
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPagerRecomandate.getCurrentItem();
            if (current < carouselAdapter.getItemCount() - 1) viewPagerRecomandate.setCurrentItem(current + 1, true);
            else viewPagerRecomandate.setCurrentItem(0, true);
        });

        viewPagerRecomandate.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 8000);
            }
        });

        btnToggleGps.setOnClickListener(v -> toggleGpsMode());

        setupSearchAndFilter(view);

        firebaseHelper = new FirebaseHelper();
        fetchLocationsFromFirebase();

        return view;
    }

    private void toggleGpsMode() {
        isGpsEnabled = !isGpsEnabled;
        updateGpsUi();

        if (isGpsEnabled) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocationAndFilter(); // Avem deja permisiunea
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION); // Cerem permisiunea cu pop-up
            }
        } else {
            // Dacă utilizatorul oprește GPS-ul, curățăm locația și reafișăm topul global
            hasUserLocation = false;
            filterCarousel();
        }
    }

    private void updateGpsUi() {
        if (isGpsEnabled) {
            tvCarouselTitle.setText("🔥 În apropierea ta (<100km)");
            btnToggleGps.setImageResource(R.drawable.ic_location_on);
            btnToggleGps.setColorFilter(android.graphics.Color.parseColor("#4A90E2"));
        } else {
            tvCarouselTitle.setText("🔥 Top Recomandări în România ");
            btnToggleGps.setImageResource(R.drawable.ic_location_off);
            btnToggleGps.setColorFilter(android.graphics.Color.parseColor("#888888"));
        }
    }

    @SuppressWarnings("MissingPermission")
    private void fetchUserLocationAndFilter() {
        // Activăm efectul de Loading
        progressGps.setVisibility(View.VISIBLE);
        viewPagerRecomandate.setAlpha(0.3f); // Estompăm caruselul cât se gândește

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            // Oprim efectul de Loading
            progressGps.setVisibility(View.GONE);
            viewPagerRecomandate.setAlpha(1.0f);

            if (location != null) {
                userLat = location.getLatitude();
                userLng = location.getLongitude();
                hasUserLocation = true;
                filterCarousel(); // Declanșăm matematica
            } else {
                Toast.makeText(getContext(), "Verifică dacă ai GPS-ul pornit din bara de sus a telefonului.", Toast.LENGTH_LONG).show();
                isGpsEnabled = false;
                updateGpsUi();
                filterCarousel();
            }
        });
    }

    // ALGORITMUL HAVERSINE PENTRU MATEMATICĂ (Aflăm distanța aeriană exactă)
    private double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Raza Pământului în km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 8000);

        if (firebaseHelper != null) {
            fetchLocationsFromFirebase();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    private void fetchLocationsFromFirebase() {
        firebaseHelper.readLocations(new FirebaseHelper.LocationsCallback() {
            @Override
            public void onCallback(List<Location> locations) {
                fullLocationsList.clear();
                fullLocationsList.addAll(locations);

                if (locationAdapter != null) {
                    locationAdapter.updateData(locations);
                    locationAdapter.applyFiltersAdvanced(currentSearchText, currentMainCategory, currentCategory, currentCounty);
                }
                filterCarousel();
            }
            @Override
            public void onFailure(String error) {
                if (getContext() != null) Toast.makeText(getContext(), "Eroare: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterCarousel() {
        if (carouselAdapter == null) return;
        List<Location> filteredList = new ArrayList<>();

        boolean appliedGpsFilter = isGpsEnabled && hasUserLocation;

        for (Location loc : fullLocationsList) {
            boolean matchesSearch = loc.getName().toLowerCase().contains(currentSearchText.toLowerCase());
            boolean matchesMain = false;
            String cat = loc.getCategory();
            if (currentMainCategory.equals("Food")) {
                matchesMain = cat.equals("Restaurant Tradițional") || cat.equals("Restaurant Premium") || cat.equals("Cafenea") || cat.equals("Fast Food") || cat.equals("Patiserie & Gelaterie") || cat.equals("Shaormerie");
            } else if (currentMainCategory.equals("Relax")) {
                matchesMain = cat.equals("Parc de Distracții") || cat.equals("Spa & Wellness") || cat.equals("Escape Room") || cat.equals("Piscina & Aqua Park") || cat.equals("Club & Bar");
            } else {
                matchesMain = cat.equals("Castel & Cetate") || cat.equals("Natură") || cat.equals("Istorie") || cat.equals("Cultură") || cat.equals("Aventură & Relaxare") || cat.equals("Mănăstiri & Biserici");
            }
            boolean matchesCategory = currentCategory.equals("Toate") || cat.equals(currentCategory);
            boolean matchesCounty = currentCounty.equals("Toate") || loc.getCounty().equals(currentCounty);

            if (matchesSearch && matchesMain && matchesCategory && matchesCounty) {
                // Dacă GPS-ul E PORNIT, adăugăm doar ce e la sub 100km!
                if (appliedGpsFilter) {
                    double dist = calculateDistanceInKm(userLat, userLng, loc.getLatitude(), loc.getLongitude());
                    if (dist <= 100.0) {
                        filteredList.add(loc);
                    }
                } else {
                    filteredList.add(loc);
                }
            }
        }

        // --- SISTEMUL INTELIGENT FALLBACK ---
        // Dacă aveam GPS-ul pornit dar nu a găsit absolut nimic în 100km:
        if (appliedGpsFilter && filteredList.isEmpty()) {
            Toast.makeText(getContext(), "Nu am găsit atracții la sub 100km de tine. Afișăm topul global!", Toast.LENGTH_LONG).show();
            isGpsEnabled = false; // Oprim GPS-ul automat
            updateGpsUi(); // Resetează interfața (titlul și icoana)
            hasUserLocation = false;
            filterCarousel(); // Se auto-apelează pentru a reîncărca lista normală
            return; // Oprim execuția curentă ca să nu se afișeze o listă goală
        }

        // --- SORTAREA ---
        if (appliedGpsFilter) {
            // Sortăm crescător după distanță (Cele mai apropiate apar primele)
            filteredList.sort((l1, l2) -> {
                double d1 = calculateDistanceInKm(userLat, userLng, l1.getLatitude(), l1.getLongitude());
                double d2 = calculateDistanceInKm(userLat, userLng, l2.getLatitude(), l2.getLongitude());
                return Double.compare(d1, d2);
            });
        } else {
            // Sortăm descrescător după vizualizări (Topul clasic)
            filteredList.sort((l1, l2) -> Integer.compare(l2.getViews(), l1.getViews()));
        }

        if (filteredList.size() > 6) filteredList = filteredList.subList(0, 6);

        // Trimitem coordonatele la Adaptor ca să poată afișa textul "~12 km"
        carouselAdapter.setUserLocation(userLat, userLng, appliedGpsFilter);

        int currentItemIndex = viewPagerRecomandate.getCurrentItem();
        carouselAdapter.updateData(filteredList);
        carouselAdapter.notifyDataSetChanged();

        if (currentItemIndex < filteredList.size() && currentItemIndex >= 0) {
            viewPagerRecomandate.setCurrentItem(currentItemIndex, false);
        }

        new TabLayoutMediator(tabLayoutDots, viewPagerRecomandate, (tab, position) -> {}).attach();
    }

    // --- Funcțiile tale de Filtrare (Neschimbate) ---
    private void setupSearchAndFilter(View view) {
        android.widget.EditText etSearch = view.findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString();
                if (locationAdapter != null) locationAdapter.applyFiltersAdvanced(currentSearchText, currentMainCategory, currentCategory, currentCounty);
                filterCarousel();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        view.findViewById(R.id.btn_filter).setOnClickListener(v -> {
            com.google.android.material.bottomsheet.BottomSheetDialog filterDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
            View filterView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_filter, null);
            filterDialog.setContentView(filterView);

            android.widget.Spinner spinCounty = filterView.findViewById(R.id.spinner_county);
            android.widget.Spinner spinCategory = filterView.findViewById(R.id.spinner_category);

            String[] counties = {"Toate", "Alba", "Arad", "Argeș", "Bacău", "Bihor", "Bistrița-Năsăud", "Botoșani", "Brașov", "Brăila", "București", "Buzău", "Caraș-Severin", "Călărași", "Cluj", "Constanța", "Covasna", "Dâmbovița", "Dolj", "Galați", "Giurgiu", "Gorj", "Harghita", "Hunedoara", "Ialomița", "Iași", "Ilfov", "Maramureș", "Mehedinți", "Mureș", "Neamț", "Olt", "Prahova", "Satu Mare", "Sălaj", "Sibiu", "Suceava", "Teleorman", "Timiș", "Tulcea", "Vaslui", "Vâlcea", "Vrancea"};
            spinCounty.setAdapter(new android.widget.ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, counties));

            String[] categories = currentMainCategory.equals("Food") ? new String[]{"Toate", "Restaurant Tradițional", "Restaurant Premium", "Cafenea", "Fast Food", "Patiserie & Gelaterie", "Shaormerie"} :
                    currentMainCategory.equals("Relax") ? new String[]{"Toate", "Parc de Distracții", "Spa & Wellness", "Escape Room", "Piscina & Aqua Park", "Club & Bar"} :
                            new String[]{"Toate", "Castel & Cetate", "Natură", "Istorie", "Cultură", "Aventură & Relaxare", "Mănăstiri & Biserici"};
            spinCategory.setAdapter(new android.widget.ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, categories));

            spinCounty.setSelection(java.util.Arrays.asList(counties).indexOf(currentCounty));
            int catIndex = java.util.Arrays.asList(categories).indexOf(currentCategory);
            spinCategory.setSelection(catIndex == -1 ? 0 : catIndex);

            filterView.findViewById(R.id.btn_apply_filter).setOnClickListener(v1 -> {
                currentCounty = spinCounty.getSelectedItem().toString();
                currentCategory = spinCategory.getSelectedItem().toString();
                if (locationAdapter != null) locationAdapter.applyFiltersAdvanced(currentSearchText, currentMainCategory, currentCategory, currentCounty);
                filterCarousel();
                filterDialog.dismiss();
            });

            filterView.findViewById(R.id.btn_reset_filter).setOnClickListener(v1 -> {
                currentCounty = "Toate"; currentCategory = "Toate";
                if (locationAdapter != null) locationAdapter.applyFiltersAdvanced(currentSearchText, currentMainCategory, currentCategory, currentCounty);
                filterCarousel();
                filterDialog.dismiss();
            });
            filterDialog.show();
        });
    }

    private void selectMainCategory(String category) {
        currentMainCategory = category;
        chipTurism.setBackgroundResource(R.drawable.bg_chip_unselected); chipTurism.setTextColor(android.graphics.Color.parseColor("#555555"));
        chipFood.setBackgroundResource(R.drawable.bg_chip_unselected); chipFood.setTextColor(android.graphics.Color.parseColor("#555555"));
        chipRelax.setBackgroundResource(R.drawable.bg_chip_unselected); chipRelax.setTextColor(android.graphics.Color.parseColor("#555555"));

        if (category.equals("Turism")) { chipTurism.setBackgroundResource(R.drawable.bg_chip_selected); chipTurism.setTextColor(android.graphics.Color.WHITE); }
        else if (category.equals("Food")) { chipFood.setBackgroundResource(R.drawable.bg_chip_selected); chipFood.setTextColor(android.graphics.Color.WHITE); }
        else if (category.equals("Relax")) { chipRelax.setBackgroundResource(R.drawable.bg_chip_selected); chipRelax.setTextColor(android.graphics.Color.WHITE); }

        currentCategory = "Toate";
        if (locationAdapter != null) locationAdapter.applyFiltersAdvanced(currentSearchText, currentMainCategory, currentCategory, currentCounty);
        filterCarousel();
    }
}