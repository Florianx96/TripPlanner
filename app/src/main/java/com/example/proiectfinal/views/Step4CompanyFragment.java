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

public class Step4CompanyFragment extends Fragment {

    private AiWizardViewModel viewModel;
    private View selectedView = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step4_company, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(AiWizardViewModel.class);

        setupOption(view.findViewById(R.id.option_solo),    "Solo");
        setupOption(view.findViewById(R.id.option_cuplu),   "Cuplu");
        setupOption(view.findViewById(R.id.option_familie), "Familie cu copii");
        setupOption(view.findViewById(R.id.option_grup),    "Grup de prieteni");

        restoreSelection(view);
        return view;
    }

    private void setupOption(View optionView, String value) {
        optionView.setOnClickListener(v -> {
            if (selectedView != null) selectedView.setBackgroundResource(R.drawable.bg_option_normal);
            optionView.setBackgroundResource(R.drawable.bg_option_selected);
            selectedView = optionView;
            viewModel.setCompany(value);
        });
    }

    private void restoreSelection(View root) {
        String saved = viewModel.getAiRequest().getCompany();
        if (saved == null) return;
        int id;
        switch (saved) {
            case "Solo":              id = R.id.option_solo;    break;
            case "Cuplu":             id = R.id.option_cuplu;   break;
            case "Familie cu copii":  id = R.id.option_familie; break;
            case "Grup de prieteni":  id = R.id.option_grup;    break;
            default: return;
        }
        View v = root.findViewById(id);
        v.setBackgroundResource(R.drawable.bg_option_selected);
        selectedView = v;
    }
}