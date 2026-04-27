package com.example.proiectfinal.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.proiectfinal.R;
import com.example.proiectfinal.viewmodels.AiWizardViewModel;

public class Step1TypeFragment extends Fragment {

    private AiWizardViewModel viewModel;
    private View selectedView = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step1_type, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(AiWizardViewModel.class);

        setupOption(view.findViewById(R.id.option_munte),    "Munte");
        setupOption(view.findViewById(R.id.option_mare),     "Mare");
        setupOption(view.findViewById(R.id.option_city),     "City Break");
        setupOption(view.findViewById(R.id.option_aventura), "Aventura");
        setupOption(view.findViewById(R.id.option_cultural), "Cultural");

        // Restaurăm selecția dacă userul s-a întors înapoi
        restoreSelection(view);

        return view;
    }

    private void setupOption(View optionView, String value) {
        optionView.setOnClickListener(v -> {
            if (selectedView != null) {
                selectedView.setBackgroundResource(R.drawable.bg_option_normal);
            }
            optionView.setBackgroundResource(R.drawable.bg_option_selected);
            selectedView = optionView;
            viewModel.setVacationType(value);
        });
    }

    private void restoreSelection(View root) {
        String saved = viewModel.getAiRequest().getVacationType();
        if (saved == null) return;
        int id;
        switch (saved) {
            case "Munte":      id = R.id.option_munte;    break;
            case "Mare":       id = R.id.option_mare;     break;
            case "City Break": id = R.id.option_city;     break;
            case "Aventura":   id = R.id.option_aventura; break;
            case "Cultural":   id = R.id.option_cultural; break;
            default: return;
        }
        View v = root.findViewById(id);
        v.setBackgroundResource(R.drawable.bg_option_selected);
        selectedView = v;
    }
}