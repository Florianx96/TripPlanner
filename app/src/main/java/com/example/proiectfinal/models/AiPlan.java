package com.example.proiectfinal.models;

import java.util.List;

public class AiPlan implements java.io.Serializable{

    // Planul complet = o listă de zile
    private List<DayPlan> days;
    private String generalTips; // Sfaturi generale de la AI (opțional)

    public AiPlan() {}

    public AiPlan(List<DayPlan> days, String generalTips) {
        this.days = days;
        this.generalTips = generalTips;
    }

    public List<DayPlan> getDays() { return days; }
    public void setDays(List<DayPlan> days) { this.days = days; }

    public String getGeneralTips() { return generalTips; }
    public void setGeneralTips(String generalTips) { this.generalTips = generalTips; }

    // -----------------------------------------------
    // Clasa internă: O singură zi din plan
    // -----------------------------------------------
    public static class DayPlan implements java.io.Serializable{

        private int dayNumber;              // Ziua 1, Ziua 2...
        private List<LocationItem> locations; // Locațiile din acea zi
        private String hotelRecommendation; // "Hotel X — ~200 RON/noapte"
        private String estimatedDayBudget;  // "150-250 RON/persoană"
        private String dayTip;              // Sfat special pentru ziua asta

        public DayPlan() {}

        public int getDayNumber() { return dayNumber; }
        public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }

        public List<LocationItem> getLocations() { return locations; }
        public void setLocations(List<LocationItem> locations) { this.locations = locations; }

        public String getHotelRecommendation() { return hotelRecommendation; }
        public void setHotelRecommendation(String hotelRecommendation) { this.hotelRecommendation = hotelRecommendation; }

        public String getEstimatedDayBudget() { return estimatedDayBudget; }
        public void setEstimatedDayBudget(String estimatedDayBudget) { this.estimatedDayBudget = estimatedDayBudget; }

        public String getDayTip() { return dayTip; }
        public void setDayTip(String dayTip) { this.dayTip = dayTip; }
    }

    // -----------------------------------------------
    // Clasa internă: O locație din planul unei zile
    // -----------------------------------------------
    public static class LocationItem implements java.io.Serializable{

        private String name;          // "Castelul Peleș"
        private String city;          // "Sinaia"
        private String county;        // "Prahova"
        private String entryPrice;    // "~45 RON/persoană"
        private String visitDuration; // "2-3 ore"
        private String tip;           // Sfat specific locației

        public LocationItem() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getCounty() { return county; }
        public void setCounty(String county) { this.county = county; }

        public String getEntryPrice() { return entryPrice; }
        public void setEntryPrice(String entryPrice) { this.entryPrice = entryPrice; }

        public String getVisitDuration() { return visitDuration; }
        public void setVisitDuration(String visitDuration) { this.visitDuration = visitDuration; }

        public String getTip() { return tip; }
        public void setTip(String tip) { this.tip = tip; }
    }
}