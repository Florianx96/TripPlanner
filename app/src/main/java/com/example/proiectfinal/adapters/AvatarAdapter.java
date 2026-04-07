package com.example.proiectfinal.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proiectfinal.R;

import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private Context context;
    private List<Integer> avatarList; // Lista de ID-uri din R.drawable (ex: R.drawable.avatar_1)
    private int selectedPosition = -1; // Reține care poză este selectată acum
    private OnAvatarSelectedListener listener;

    // Interfața prin care anunțăm fereastra că utilizatorul a ales ceva
    public interface OnAvatarSelectedListener {
        void onAvatarSelected(int avatarResId);
    }

    public AvatarAdapter(Context context, List<Integer> avatarList, OnAvatarSelectedListener listener) {
        this.context = context;
        this.avatarList = avatarList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        int avatarResId = avatarList.get(position);

        // APLICĂM GLIDE PENTRU ROTUNJIREA MAGICĂ A POZELOR TALE AI
        Glide.with(context)
                .load(avatarResId)
                .circleCrop()
                .into(holder.ivAvatar);

        // Dacă asta e poza selectată, arătăm bifa. Altfel, o ascundem.
        if (selectedPosition == position) {
            holder.ivBifa.setVisibility(View.VISIBLE);
        } else {
            holder.ivBifa.setVisibility(View.GONE);
        }

        // Ce se întâmplă când dai click pe o poză
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = position; // Actualizăm selecția

            // Refacem doar cele 2 poze implicate ca să fim super rapizi (animație fluidă)
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            // Anunțăm fereastra ca să activeze butonul "Salvează"
            listener.onAvatarSelected(avatarResId);
        });
    }

    @Override
    public int getItemCount() {
        return avatarList.size();
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivBifa;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar_item);
            ivBifa = itemView.findViewById(R.id.iv_avatar_selected);
        }
    }
}