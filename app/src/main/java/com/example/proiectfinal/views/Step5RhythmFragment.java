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

public class Step5RhythmFragment extends Fragment {

    private AiWizardViewModel viewModel;
    private View selectedView = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step5_rhythm, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(AiWizardViewModel.class);

        setupOption(view.findViewById(R.id.option_relaxat), "Relaxat");
        setupOption(view.findViewById(R.id.option_activ),   "Activ");
        setupOption(view.findViewById(R.id.option_intens),  "Intens");

        restoreSelection(view);
        return view;
    }

    private void setupOption(View optionView, String value) {
        optionView.setOnClickListener(v -> {
            if (selectedView != null) selectedView.setBackgroundResource(R.drawable.bg_option_normal);
            optionView.setBackgroundResource(R.drawable.bg_option_selected);
            selectedView = optionView;
            viewModel.setRhythm(value);
        });
    }

    private void restoreSelection(View root) {
        String saved = viewModel.getAiRequest().getRhythm();
        if (saved == null) return;
        int id;
        switch (saved) {
            case "Relaxat": id = R.id.option_relaxat; break;
            case "Activ":   id = R.id.option_activ;   break;
            case "Intens":  id = R.id.option_intens;  break;
            default: return;
        }
        View v = root.findViewById(id);
        v.setBackgroundResource(R.drawable.bg_option_selected);
        selectedView = v;
    }
}