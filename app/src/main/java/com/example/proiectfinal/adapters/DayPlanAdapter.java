package com.example.proiectfinal.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proiectfinal.R;
import com.example.proiectfinal.models.AiPlan;

import java.util.List;

public class DayPlanAdapter extends RecyclerView.Adapter<DayPlanAdapter.DayViewHolder> {

    private final Context context;
    private final List<AiPlan.DayPlan> days;

    public DayPlanAdapter(Context context, List<AiPlan.DayPlan> days) {
        this.context = context;
        this.days = days;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_day_plan, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        AiPlan.DayPlan day = days.get(position);

        holder.tvDayTitle.setText("Ziua " + day.getDayNumber());

        // Construim dinamic lista locațiilor pentru fiecare zi
        holder.layoutLocations.removeAllViews();

        if (day.getLocations() != null) {
            for (AiPlan.LocationItem loc : day.getLocations()) {
                View locView = LayoutInflater.from(context)
                        .inflate(R.layout.item_location_ai, holder.layoutLocations, false);

                TextView tvName     = locView.findViewById(R.id.tv_ai_loc_name);
                TextView tvCity     = locView.findViewById(R.id.tv_ai_loc_city);
                TextView tvPrice    = locView.findViewById(R.id.tv_ai_loc_price);
                TextView tvDuration = locView.findViewById(R.id.tv_ai_loc_duration);
                TextView tvTip      = locView.findViewById(R.id.tv_ai_loc_tip);

                tvName.setText(loc.getName());
                tvCity.setText("📍 " + loc.getCity() + ", " + loc.getCounty());
                tvPrice.setText("💰 " + (loc.getEntryPrice() != null ? loc.getEntryPrice() : "Gratuit"));
                tvDuration.setText("⏱ " + (loc.getVisitDuration() != null ? loc.getVisitDuration() : "N/A"));

                if (loc.getTip() != null && !loc.getTip().isEmpty()) {
                    tvTip.setText("💡 " + loc.getTip());
                    tvTip.setVisibility(View.VISIBLE);
                } else {
                    tvTip.setVisibility(View.GONE);
                }

                holder.layoutLocations.addView(locView);
            }
        }

        // Hotel
        if (day.getHotelRecommendation() != null) {
            holder.tvHotel.setText("🏨 " + day.getHotelRecommendation());
        }

        // Buget estimat
        if (day.getEstimatedDayBudget() != null) {
            holder.tvBudget.setText("💵 Buget estimat: " + day.getEstimatedDayBudget());
        }

        // Sfat al zilei
        if (day.getDayTip() != null && !day.getDayTip().isEmpty()) {
            holder.tvDayTip.setText("✨ " + day.getDayTip());
            holder.tvDayTip.setVisibility(View.VISIBLE);
        } else {
            holder.tvDayTip.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayTitle, tvHotel, tvBudget, tvDayTip;
        LinearLayout layoutLocations;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayTitle      = itemView.findViewById(R.id.tv_day_title);
            layoutLocations = itemView.findViewById(R.id.layout_day_locations);
            tvHotel         = itemView.findViewById(R.id.tv_day_hotel);
            tvBudget        = itemView.findViewById(R.id.tv_day_budget);
            tvDayTip        = itemView.findViewById(R.id.tv_day_tip);
        }
    }
}