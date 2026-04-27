package com.example.proiectfinal.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.proiectfinal.R;
import com.example.proiectfinal.firebase.GeminiHelper;
import com.example.proiectfinal.models.AiPlan;
import com.example.proiectfinal.models.AiRequest;
import com.example.proiectfinal.viewmodels.AiWizardViewModel;

public class WizardContainerActivity extends AppCompatActivity {

    private int currentStep = 1;
    private static final int TOTAL_STEPS = 5;

    private Button btnNext, btnBack;
    private TextView tvStep;
    private ProgressBar progressBar;
    private View loadingOverlay;

    private AiWizardViewModel viewModel;
    private GeminiHelper geminiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_container);

        viewModel = new ViewModelProvider(this).get(AiWizardViewModel.class);

        String apiKey = getString(R.string.gemini_api_key);
        geminiHelper = new GeminiHelper(apiKey);

        btnNext      = findViewById(R.id.btn_wizard_next);
        btnBack      = findViewById(R.id.btn_wizard_back);
        tvStep       = findViewById(R.id.tv_wizard_step);
        progressBar  = findViewById(R.id.progress_wizard);
        loadingOverlay = findViewById(R.id.loading_overlay);

        loadStep(currentStep);

        btnNext.setOnClickListener(v -> goToNextStep());
        btnBack.setOnClickListener(v -> goToPreviousStep());
    }

    private void loadStep(int step) {
        Fragment fragment;
        switch (step) {
            case 1:  fragment = new Step1TypeFragment();    break;
            case 2:  fragment = new Step2BudgetFragment();  break;
            case 3:  fragment = new Step3PeriodFragment();  break;
            case 4:  fragment = new Step4CompanyFragment(); break;
            case 5:  fragment = new Step5RhythmFragment();  break;
            default: fragment = new Step1TypeFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.wizard_fragment_container, fragment)
                .commit();

        tvStep.setText("Pasul " + step + " din " + TOTAL_STEPS);
        progressBar.setProgress(step);
        btnBack.setVisibility(step == 1 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(step == TOTAL_STEPS ? "Generează Plan 🚀" : "Următorul →");
    }

    private void goToNextStep() {
        if (!isCurrentStepValid()) {
            Toast.makeText(this, "Alege o opțiune înainte de a continua!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentStep < TOTAL_STEPS) {
            currentStep++;
            loadStep(currentStep);
        } else {
            startAiGeneration();
        }
    }

    private void goToPreviousStep() {
        if (currentStep > 1) {
            currentStep--;
            loadStep(currentStep);
        }
    }

    private boolean isCurrentStepValid() {
        AiRequest req = viewModel.getAiRequest();
        switch (currentStep) {
            case 1: return req.getVacationType() != null;
            case 2: return req.getBudget() != null;
            case 3: return req.getMonth() != null && req.getDaysCount() > 0;
            case 4: return req.getCompany() != null;
            case 5: return req.getRhythm() != null;
            default: return false;
        }
    }

    // -----------------------------------------------
    // GENERAREA PLANULUI — apelul real către AI
    // -----------------------------------------------
    private void startAiGeneration() {
        showLoading(true);

        geminiHelper.generatePlan(
                viewModel.getAiRequest(),
                viewModel,
                new GeminiHelper.GeminiCallback() {

                    @Override
                    public void onSuccess(AiPlan plan) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            // Deschidem ecranul de rezultat
                            Intent intent = new Intent(
                                    WizardContainerActivity.this,
                                    AiResultActivity.class
                            );
                            intent.putExtra("ai_plan", plan);
                            intent.putExtra("vacation_type", viewModel.getAiRequest().getVacationType());
                            intent.putExtra("days_count", viewModel.getAiRequest().getDaysCount());
                            startActivity(intent);
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(
                                    WizardContainerActivity.this,
                                    "Eroare: " + error + "\nVerifică internetul și încearcă din nou.",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        btnNext.setEnabled(!show);
        btnBack.setEnabled(!show);
    }

    @Override
    public void onBackPressed() {
        if (loadingOverlay.getVisibility() == View.VISIBLE) return; // Blocăm back în timp ce se generează
        if (currentStep > 1) {
            currentStep--;
            loadStep(currentStep);
        } else {
            super.onBackPressed();
        }
    }
}