package com.example.proiectfinal.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proiectfinal.R;
import com.example.proiectfinal.models.Location;

import java.util.List;

public class LocationCarouselAdapter extends RecyclerView.Adapter<LocationCarouselAdapter.CarouselViewHolder> {

    private Context context;
    private List<Location> locationList;
    private OnItemClickListener listener;

    // --- NOU: Variabile pentru sistemul GPS ---
    private double userLat = 0.0;
    private double userLng = 0.0;
    private boolean isGpsEnabled = false;

    public interface OnItemClickListener {
        void onItemClick(Location location);
    }

    public LocationCarouselAdapter(Context context, List<Location> locationList, OnItemClickListener listener) {
        this.context = context;
        this.locationList = locationList;
        this.listener = listener;
    }

    public void updateData(List<Location> newLocations) {
        this.locationList = newLocations;
        notifyDataSetChanged();
    }

    // --- NOU: Funcția care primește coordonatele tale de la HomeFragment ---
    public void setUserLocation(double lat, double lng, boolean isEnabled) {
        this.userLat = lat;
        this.userLng = lng;
        this.isGpsEnabled = isEnabled;
    }

    // --- NOU: Algoritmul Haversine pentru a calcula distanța din mers ---
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

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_location_carousel, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        Location location = locationList.get(position);

        holder.tvTitle.setText(location.getName());
        holder.tvCategory.setText(location.getCategory() + " • " + location.getCounty());

        // K-Formatter (ex: 1500 devine 1.5k)
        holder.tvLikes.setText(formatNumber(location.getFavoritesCount()));
        holder.tvViews.setText(formatNumber(location.getViews()));

        // Încărcăm Hero Image cu Glide
        if (location.getImageUrl() != null && !location.getImageUrl().isEmpty()) {
            Glide.with(context).load(location.getImageUrl()).centerCrop().into(holder.ivImage);
        }

        // --- NOU: Logica pentru afișarea distanței "Aproximativ X km" ---
        if (isGpsEnabled) {
            double distance = calculateDistanceInKm(userLat, userLng, location.getLatitude(), location.getLongitude());
            int roundedDistance = (int) Math.round(distance);
            holder.tvDistance.setText("~" + roundedDistance + " km");
            holder.tvDistance.setVisibility(View.VISIBLE);
        } else {
            holder.tvDistance.setVisibility(View.GONE); // O ascundem dacă GPS-ul e oprit
        }

        // Click pentru a merge la Detalii + Logica ta originală de Firebase
        holder.itemView.setOnClickListener(v -> {

            // 1. Incrementăm vizualizările local
            int newViews = location.getViews() + 1;
            location.setViews(newViews);
            notifyItemChanged(position); // Actualizează vizual numărul de vizualizări instant

            // 2. Trimitem modificarea către Firebase (Logica ta păstrată)
            if (location.getId() != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("locations")
                        .document(location.getId())
                        .update("views", newViews);
            }

            // 3. Declanșăm interfața pentru a deschide detaliile
            if (listener != null) {
                listener.onItemClick(location);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    // Funcția care formatează numerele mari
    private String formatNumber(int number) {
        if (number >= 1000000) return (number / 1000000) + "." + ((number % 1000000) / 100000) + "M";
        if (number >= 1000) return (number / 1000) + "." + ((number % 1000) / 100) + "k";
        return String.valueOf(number);
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvCategory, tvLikes, tvViews, tvDistance;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_carousel_image);
            tvTitle = itemView.findViewById(R.id.tv_carousel_title);
            tvCategory = itemView.findViewById(R.id.tv_carousel_category);
            tvLikes = itemView.findViewById(R.id.tv_carousel_likes);
            tvViews = itemView.findViewById(R.id.tv_carousel_views);
            tvDistance = itemView.findViewById(R.id.tv_carousel_distance);
        }
    }
}