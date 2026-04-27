package com.example.proiectfinal.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.proiectfinal.models.AiPlan;
import com.example.proiectfinal.models.AiRequest;

public class AiWizardViewModel extends ViewModel {

    // Obiectul care acumulează răspunsurile userului pe parcursul wizard-ului
    private final AiRequest aiRequest = new AiRequest();

    // LiveData pentru planul generat — fragmentul de rezultat îl observă
    private final MutableLiveData<AiPlan> generatedPlan = new MutableLiveData<>();

    // LiveData pentru starea de loading
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // LiveData pentru erori
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // -----------------------------------------------
    // Metode pentru setarea datelor din fiecare pas
    // -----------------------------------------------

    // Pasul 1
    public void setVacationType(String type) {
        aiRequest.setVacationType(type);
    }

    // Pasul 2
    public void setBudget(String budget) {
        aiRequest.setBudget(budget);
    }

    // Pasul 3
    public void setPeriod(String month, int days) {
        aiRequest.setMonth(month);
        aiRequest.setDaysCount(days);
    }

    // Pasul 4
    public void setCompany(String company) {
        aiRequest.setCompany(company);
    }

    // Pasul 5
    public void setRhythm(String rhythm) {
        aiRequest.setRhythm(rhythm);
    }

    // -----------------------------------------------
    // Getter pentru AiRequest complet (folosit de GeminiHelper)
    // -----------------------------------------------
    public AiRequest getAiRequest() {
        return aiRequest;
    }

    // -----------------------------------------------
    // LiveData Getters (fragmentele se abonează la acestea)
    // -----------------------------------------------
    public LiveData<AiPlan> getGeneratedPlan() {
        return generatedPlan;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // -----------------------------------------------
    // Metode apelate de GeminiHelper după răspuns
    // -----------------------------------------------
    public void setPlanResult(AiPlan plan) {
        generatedPlan.postValue(plan);
        isLoading.postValue(false);
    }

    public void setLoading(boolean loading) {
        isLoading.postValue(loading);
    }

    public void setError(String error) {
        errorMessage.postValue(error);
        isLoading.postValue(false);
    }

    // Validare — verificăm că userul a completat toți pașii înainte să apelăm AI-ul
    public boolean isRequestComplete() {
        return aiRequest.getVacationType() != null
                && aiRequest.getBudget() != null
                && aiRequest.getMonth() != null
                && aiRequest.getDaysCount() > 0
                && aiRequest.getCompany() != null
                && aiRequest.getRhythm() != null;
    }
}