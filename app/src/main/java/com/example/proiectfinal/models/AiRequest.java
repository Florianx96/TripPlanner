package com.example.proiectfinal.models;

public class AiRequest {

    // Pasul 1: Tipul vacanței
    private String vacationType; // "Munte", "Mare", "City Break", "Aventura", "Cultural"

    // Pasul 2: Bugetul
    private String budget; // "Economic", "Mediu", "Premium"

    // Pasul 3: Perioada
    private String month;    // "Iulie"
    private int daysCount;   // 3

    // Pasul 4: Cu cine călătorești
    private String company;  // "Solo", "Cuplu", "Familie cu copii", "Grup de prieteni"

    // Pasul 5: Ritmul
    private String rhythm;   // "Relaxat", "Activ", "Intens"

    // Constructor gol
    public AiRequest() {}

    // Getters & Setters
    public String getVacationType() { return vacationType; }
    public void setVacationType(String vacationType) { this.vacationType = vacationType; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public int getDaysCount() { return daysCount; }
    public void setDaysCount(int daysCount) { this.daysCount = daysCount; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getRhythm() { return rhythm; }
    public void setRhythm(String rhythm) { this.rhythm = rhythm; }

    // Metodă utilă — transformă tipul ales de user în categoriile din Firebase
    // Asta e "podul" dintre wizard și baza ta de date
    public String[] getFirebaseCategories() {
        switch (vacationType) {
            case "Munte":
                return new String[]{"Natură", "Aventură & Relaxare"};
            case "Mare":
                return new String[]{"Natură", "Aventură & Relaxare"};
            case "City Break":
                return new String[]{"Cultură", "Istorie"};
            case "Aventura":
                return new String[]{"Aventură & Relaxare", "Natură"};
            case "Cultural":
                return new String[]{"Castel & Cetate", "Mănăstiri & Biserici", "Istorie", "Cultură"};
            default:
                return new String[]{"Natură", "Cultură", "Istorie"};
        }
    }

    // Metodă utilă — numărul recomandat de locații per zi în funcție de ritm
    public int getLocationsPerDay() {
        switch (rhythm) {
            case "Relaxat": return 1;
            case "Activ":   return 2;
            case "Intens":  return 3;
            default:        return 2;
        }
    }
}