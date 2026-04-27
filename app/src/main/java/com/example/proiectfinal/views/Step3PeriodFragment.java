package com.example.proiectfinal.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.proiectfinal.R;
import com.example.proiectfinal.viewmodels.AiWizardViewModel;

public class Step3PeriodFragment extends Fragment {

    private AiWizardViewModel viewModel;
    private int daysCount = 1;
    private TextView tvDaysCount;

    private static final String[] MONTHS = {
            "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
            "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_step3_period, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(AiWizardViewModel.class);

        // --- Spinner luni ---
        Spinner spinnerMonth = view.findViewById(R.id.spinner_month);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                MONTHS
        );
        spinnerMonth.setAdapter(adapter);

        // Restaurăm luna selectată
        String savedMonth = viewModel.getAiRequest().getMonth();
        if (savedMonth != null) {
            for (int i = 0; i < MONTHS.length; i++) {
                if (MONTHS[i].equals(savedMonth)) {
                    spinnerMonth.setSelection(i);
                    break;
                }
            }
        }

        // Salvăm luna la fiecare selecție
        spinnerMonth.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                viewModel.setPeriod(MONTHS[pos], daysCount);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // --- Selector zile +/- ---
        tvDaysCount = view.findViewById(R.id.tv_days_count);

        // Restaurăm numărul de zile
        int savedDays = viewModel.getAiRequest().getDaysCount();
        daysCount = savedDays > 0 ? savedDays : 1;
        tvDaysCount.setText(String.valueOf(daysCount));

        view.findViewById(R.id.btn_days_minus).setOnClickListener(v -> {
            if (daysCount > 1) {
                daysCount--;
                tvDaysCount.setText(String.valueOf(daysCount));
                viewModel.setPeriod(MONTHS[spinnerMonth.getSelectedItemPosition()], daysCount);
            }
        });

        view.findViewById(R.id.btn_days_plus).setOnClickListener(v -> {
            if (daysCount < 14) {
                daysCount++;
                tvDaysCount.setText(String.valueOf(daysCount));
                viewModel.setPeriod(MONTHS[spinnerMonth.getSelectedItemPosition()], daysCount);
            }
        });

        // Inițializăm cu valorile curente
        viewModel.setPeriod(MONTHS[spinnerMonth.getSelectedItemPosition()], daysCount);

        return view;
    }
}