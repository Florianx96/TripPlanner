package com.example.proiectfinal.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proiectfinal.R;

public class AiIntroFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ai_intro, container, false);

        Button btnGetStarted = view.findViewById(R.id.btn_get_started);

        btnGetStarted.setOnClickListener(v -> {
            // Pasul 3: deschidem WizardContainerActivity
            Intent intent = new Intent(getActivity(), WizardContainerActivity.class);
            startActivity(intent);
        });

        return view;
    }
}