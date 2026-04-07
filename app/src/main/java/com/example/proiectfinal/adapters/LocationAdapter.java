package com.example.proiectfinal.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proiectfinal.R;
import com.example.proiectfinal.models.Location;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    // Interfata pentru gestionarea click-urilor pe elemente
    public interface OnLocationClickListener {
        void onLocationClick(Location location);
    }

    private final List<Location> locationList;      // Lista curenta (poate fi filtrata)
    private final List<Location> locationListFull;  // Lista completa (originala)
    private final Context context;
    private final OnLocationClickListener clickListener;

    public LocationAdapter(Context context, List<Location> locationList, OnLocationClickListener clickListener) {
        this.context = context;
        this.locationList = locationList;
        // Facem o copie a listei pentru a nu o pierde la filtrare
        this.locationListFull = new ArrayList<>(locationList);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Cream view-ul pentru un singur rand
        View view = LayoutInflater.from(context).inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locationList.get(position);

        holder.tvLocationName.setText(location.getName());
        holder.tvLocationCity.setText("📍 " + location.getCity() + ", " + location.getCounty());
        holder.tvLocationCategory.setText(location.getCategory());

        // --- 1. Vizibilitatea Statisticilor (Views & Favs) ---
        if (deleteListener != null || hideStats) {
            // Suntem în Trip Plan SAU în Favorite -> Ascundem
            holder.tvLocationViews.setVisibility(View.GONE);
            holder.tvLocationFavorites.setVisibility(View.GONE);
        } else {
            // Suntem pe Home -> Le arătăm și le formatăm
            holder.tvLocationViews.setVisibility(View.VISIBLE);
            holder.tvLocationFavorites.setVisibility(View.VISIBLE);
            holder.tvLocationViews.setText(formatNumber(location.getViews()));
            holder.tvLocationFavorites.setText(formatNumber(location.getFavoritesCount()));
        }

        // --- 2. Vizibilitatea Butonului de Ștergere ---
        if (deleteListener != null) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(location));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // --- 3. Gestionarea Click-ului pe Card ---
        holder.itemView.setOnClickListener(v -> {
            // DOAR dacă suntem pe Home (nu avem Delete și nu e ascuns), creștem view-ul
            if (deleteListener == null && !hideStats) {
                int newViews = location.getViews() + 1;
                location.setViews(newViews);
                notifyItemChanged(position);

                if (location.getId() != null) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("locations")
                            .document(location.getId())
                            .update("views", newViews);
                }
            }

            // Mergem la pagina de detalii indiferent de unde am dat click
            if (clickListener != null) clickListener.onLocationClick(location);
        });

        // 3. Incarcare Imagine cu Glide
        loadImage(holder.ivLocationImage, location.getImageUrl());

    }

    private void loadImage(ImageView imageView, String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(context)
                    .load(url)
                    .error(R.drawable.background_japan) // Imagine in caz de eroare
                    .placeholder(R.drawable.image_not_loaded) // Imagine in timpul incarcarii (optional)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.image_not_loaded);
        }
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    // --- VARIABILE PENTRU FILTRARE MULTIPLĂ ---
    private String currentSearchText = "";
    private String currentCategory = "Toate";
    private String currentCounty = "Toate";
    private boolean isTopMode = false; // NOU: Ne spune dacă lista este cea de Top 10

    public void setTopMode(boolean topMode) {
        this.isTopMode = topMode;
    }

    private boolean hideStats = false; // Ne spune dacă trebuie să ascundem vizualizările și favoritele

    public void setHideStats(boolean hideStats) {
        this.hideStats = hideStats;
    }

    // --- LOGICĂ DE FILTRARE ---
    public void applyFilters(String searchText, String category, String county) {
        this.currentSearchText = searchText == null ? "" : searchText.toLowerCase().trim();
        this.currentCategory = category == null ? "Toate" : category;
        this.currentCounty = county == null ? "Toate" : county;

        List<Location> filteredList = new ArrayList<>();

        for (Location loc : locationListFull) {
            // 1. Verificăm dacă se potrivește cu ce a scris în SearchBar
            boolean matchesSearch = currentSearchText.isEmpty() ||
                    removeAccents(loc.getName().toLowerCase()).contains(removeAccents(currentSearchText)) ||
                    (loc.getCity() != null && removeAccents(loc.getCity().toLowerCase()).contains(removeAccents(currentSearchText)));

            // 2. Verificăm dacă se potrivește Categoria
            boolean matchesCategory = currentCategory.equals("Toate") ||
                    (loc.getCategory() != null && loc.getCategory().equals(currentCategory));

            // 3. Verificăm dacă se potrivește Județul
            boolean matchesCounty = currentCounty.equals("Toate") ||
                    (loc.getCounty() != null && loc.getCounty().equals(currentCounty));

            // Dacă locația trece de TOATE cele 3 teste, o adăugăm pe ecran!
            if (matchesSearch && matchesCategory && matchesCounty) {
                filteredList.add(loc);
            }
        }


        // Actualizăm lista care se vede pe ecran
        this.locationList.clear();
        this.locationList.addAll(filteredList);
        notifyDataSetChanged();
    }

    // NOUA Funcție supremă de filtrare (CORECTATĂ)
    public void applyFiltersAdvanced(String searchText, String mainCategory, String category, String county) {
        // 1. Creăm lista temporară aici
        List<Location> filteredList = new ArrayList<>();

        // 2. Preluăm textul și eliminăm spațiile în plus
        String lowerCaseQuery = searchText == null ? "" : searchText.toLowerCase().trim();

        // Mapare: Ce sub-categorii țin de Turism, Food sau Relax?
        java.util.List<String> foodCategories = java.util.Arrays.asList("Restaurant Tradițional", "Restaurant Premium", "Cafenea", "Fast Food", "Patiserie & Gelaterie", "Shaormerie");
        java.util.List<String> relaxCategories = java.util.Arrays.asList("Parc de Distracții", "Spa & Wellness", "Escape Room", "Piscina & Aqua Park", "Club & Bar");

        // 3. Folosim lista TA originală (locationListFull)
        for (Location loc : locationListFull) {
            String locCategory = loc.getCategory() != null ? loc.getCategory() : "";

            // 1. Verificăm Căutarea Text (Folosim și funcția ta genială de removeAccents!)
            boolean matchText = lowerCaseQuery.isEmpty() ||
                    removeAccents(loc.getName().toLowerCase()).contains(removeAccents(lowerCaseQuery)) ||
                    (loc.getCity() != null && removeAccents(loc.getCity().toLowerCase()).contains(removeAccents(lowerCaseQuery)));

            // 2. Verificăm Județul
            boolean matchCounty = county.equals("Toate") || (loc.getCounty() != null && loc.getCounty().equalsIgnoreCase(county));

            // 3. Verificăm Categoria mică (Sub-categoria)
            boolean matchCategory = category.equals("Toate") || locCategory.equalsIgnoreCase(category);

            // 4. Verificăm Super-Categoria (Main Category)
            boolean matchMainCategory = false;

            if (mainCategory.equals("Turism")) {
                // Orice locație care NU e Food și NU e Relax, este automat Turism
                if (!foodCategories.contains(locCategory) && !relaxCategories.contains(locCategory)) {
                    matchMainCategory = true;
                }
            } else if (mainCategory.equals("Food")) {
                if (foodCategories.contains(locCategory)) {
                    matchMainCategory = true;
                }
            } else if (mainCategory.equals("Relax")) {
                if (relaxCategories.contains(locCategory)) {
                    matchMainCategory = true;
                }
            }

            // Dacă toate filtrele sunt valide, o adăugăm în lista temporară!
            if (matchText && matchCounty && matchCategory && matchMainCategory) {
                filteredList.add(loc);
                }
            }
            // --- LOGICA PENTRU TOP 10 ---
            if (isTopMode) {
                // Sortăm lista filtrată descrescător după vizualizări
                java.util.Collections.sort(filteredList, (l1, l2) -> Integer.compare(l2.getViews(), l1.getViews()));
                // Păstrăm doar primele 10
                if (filteredList.size() > 10) {
                    filteredList = new ArrayList<>(filteredList.subList(0, 10));
                }
            }

        // 4. LA FINAL: Golim ecranul și punem DOAR locațiile care au trecut de filtre!
        this.locationList.clear();
        this.locationList.addAll(filteredList);
        notifyDataSetChanged();
    }

    // Functie pentru eliminarea diacriticelor
    private String removeAccents(String input) {
        if (input == null) return "";

        // Descompune caracterele (ex: ș devine s + semnul sedilă)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // Elimina semnele diacritice folosind Regex
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public interface OnLocationDeleteListener {
        void onDeleteClick(Location location);
    }

    private OnLocationDeleteListener deleteListener;

    public void setOnLocationDeleteListener(OnLocationDeleteListener listener) {
        this.deleteListener = listener;
    }
    // Metodă nouă pentru a actualiza ambele liste corect când vin date de la Firebase
    public void updateData(List<Location> noileLocatii) {
        this.locationListFull.clear();
        this.locationListFull.addAll(noileLocatii);

        List<Location> initialShowList = new ArrayList<>(noileLocatii);

        // Dacă e Adaptorul de Top, sortăm și tăiem la 10!
        if (isTopMode) {
            java.util.Collections.sort(initialShowList, (l1, l2) -> Integer.compare(l2.getViews(), l1.getViews()));
            if (initialShowList.size() > 10) {
                initialShowList = new ArrayList<>(initialShowList.subList(0, 10));
            }
        }

        this.locationList.clear();
        this.locationList.addAll(initialShowList);
        notifyDataSetChanged();
    }
    // Algoritmul pentru prescurtarea numerelor (ex: 1500 -> 1.5k)
    private String formatNumber(int number) {
        if (number < 1000) {
            return String.valueOf(number); // 999 rămâne 999
        } else if (number < 1000000) {
            // Împărțim la 1000 și punem o zecimală. Ex: 1500 -> 1.5k
            String formatted = String.format(java.util.Locale.US, "%.1fk", number / 1000.0);
            return formatted.replace(".0k", "k"); // 1000 -> 1k (nu 1.0k)
        } else {
            String formatted = String.format(java.util.Locale.US, "%.1fM", number / 1000000.0);
            return formatted.replace(".0M", "M");
        }
    }


    // --- ViewHolder Class ---
    public static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocationName;
        TextView tvLocationCity;
        TextView tvLocationCategory;
        ImageView ivLocationImage;
        ImageView btnDelete;

        TextView tvLocationViews;
        TextView tvLocationFavorites;
        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            tvLocationCity = itemView.findViewById(R.id.tvLocationCity);
            tvLocationCategory = itemView.findViewById(R.id.tvLocationCategory);
            ivLocationImage = itemView.findViewById(R.id.ivLocationImage);
            btnDelete = itemView.findViewById(R.id.btn_delete_location);
            tvLocationViews = itemView.findViewById(R.id.tvLocationViews);
            tvLocationFavorites = itemView.findViewById(R.id.tvLocationFavorites);
        }
    }
}