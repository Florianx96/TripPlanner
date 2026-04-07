package com.example.proiectfinal.utils;

import android.content.Context;
import com.example.proiectfinal.models.Location;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class JsonAssetLoader {

    private static final String JSON_FILE_NAME= "location.json";

    //Metoda care citeste continutul unui fisier din folderul assets

    private static String loadJSONFromAsset(Context context){
        String json = null;
        try {
            InputStream is = context.getAssets().open(JSON_FILE_NAME);
            int size= is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        }catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    //metoda care  parseaza Json-ul si returneaza lista de obiecte Location

    public static List<Location> loadLocation(Context context) {
        String jsonString = loadJSONFromAsset(context);
        if (jsonString == null) {
            return Collections.emptyList();
        }

        Gson gson=new Gson();

        // Definim tipul de obiect pe care îl așteptam: o Lista de Location
        Type listType = new TypeToken<List<Location>>() {}.getType();

        try{
            return gson.fromJson(jsonString, listType);
        }catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

    }



}
