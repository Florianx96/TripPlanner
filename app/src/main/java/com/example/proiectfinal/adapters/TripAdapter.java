package com.example.proiectfinal.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proiectfinal.R;
import com.example.proiectfinal.models.Trip;

import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private Context context;
    private List<Trip> tripList;
    private OnTripClickListener clickListener;
    private OnTripDeleteListener deleteListener;

    // Interfețe pentru a ști când apeși pe un folder sau pe butonul de Delete
    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public interface OnTripDeleteListener {
        void onDeleteClick(Trip trip);
    }

    public TripAdapter(Context context, List<Trip> tripList, OnTripClickListener clickListener, OnTripDeleteListener deleteListener) {
        this.context = context;
        this.tripList = tripList;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trip_folder, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.tvTripName.setText(trip.getName());

        // Când apeși pe rând
        holder.itemView.setOnClickListener(v -> clickListener.onTripClick(trip));
        // Când apeși pe coșul de gunoi
        holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(trip));
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTripName;
        ImageView btnDelete;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tv_trip_folder_name);
            btnDelete = itemView.findViewById(R.id.btn_delete_trip);
        }
    }
}