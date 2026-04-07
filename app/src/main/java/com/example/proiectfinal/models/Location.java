package com.example.proiectfinal.models;

import java.io.Serializable;

public class Location implements Serializable {

    private String id;
    private String name;
    private String city;
    private String county;
    private String category;
    private String description;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private int orderIndex = 0; // Folosit pentru a memora ordinea în plan
    private int views = 0; // Contorul de vizualizări
    private int favoritesCount;


    // Constructor gol necesar pentru Firebase
    public Location() {
    }

    public Location(String id, String name, String city, String county, String category, String description, double latitude, double longitude, String imageUrl) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.county = county;
        this.category = category;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCounty() { return county; }
    public void setCounty(String county) { this.county = county; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    public int getFavoritesCount() { return favoritesCount; }

    public void setFavoritesCount(int favoritesCount) { this.favoritesCount = favoritesCount; }
}