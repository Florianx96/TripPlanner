package com.example.proiectfinal.firebase;

import com.example.proiectfinal.models.AiPlan;
import com.example.proiectfinal.models.AiRequest;
import com.example.proiectfinal.models.Location;
import com.example.proiectfinal.viewmodels.AiWizardViewModel;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiHelper {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-latest:generateContent?key=";

    private final String apiKey;
    private final OkHttpClient client;
    private final FirebaseHelper firebaseHelper;

    // Callback returnat către WizardContainerActivity
    public interface GeminiCallback {
        void onSuccess(AiPlan plan);
        void onFailure(String error);
    }

    public GeminiHelper(String apiKey) {
        this.apiKey = apiKey;
        this.firebaseHelper = new FirebaseHelper();
        // Timeout generos — Gemini poate dura 15-30 secunde
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    // -----------------------------------------------
    // METODA PRINCIPALĂ — apelată din WizardContainerActivity
    // -----------------------------------------------
    public void generatePlan(AiRequest request,
                             AiWizardViewModel viewModel,
                             GeminiCallback callback) {

        viewModel.setLoading(true);

        // PASUL 1: Interogăm Firebase pentru locațiile relevante
        firebaseHelper.readLocationsByCategories(
                request.getFirebaseCategories(),
                new FirebaseHelper.LocationsCallback() {

                    @Override
                    public void onCallback(List<Location> locations) {
                        if (locations.isEmpty()) {
                            // Dacă nu găsim nimic specific, luăm toate locațiile
                            firebaseHelper.readLocations(new FirebaseHelper.LocationsCallback() {
                                @Override
                                public void onCallback(List<Location> allLocations) {
                                    proceedWithLocations(allLocations, request, viewModel, callback);
                                }
                                @Override
                                public void onFailure(String error) {
                                    viewModel.setError("Eroare Firebase: " + error);
                                    callback.onFailure(error);
                                }
                            });
                        } else {
                            proceedWithLocations(locations, request, viewModel, callback);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        viewModel.setError("Eroare Firebase: " + error);
                        callback.onFailure(error);
                    }
                });
    }

    // -----------------------------------------------
    // PASUL 2: Construim promptul și apelăm Gemini
    // -----------------------------------------------
    private void proceedWithLocations(List<Location> locations,
                                      AiRequest request,
                                      AiWizardViewModel viewModel,
                                      GeminiCallback callback) {

        String prompt = buildPrompt(request, locations);
        callGeminiApi(prompt, viewModel, callback);
    }

    // -----------------------------------------------
    // PROMPT ENGINEERING — inima sistemului
    // -----------------------------------------------
    private String buildPrompt(AiRequest request, List<Location> locations) {

        // Construim lista locațiilor disponibile
        StringBuilder locList = new StringBuilder();
        for (Location loc : locations) {
            locList.append("  - ").append(loc.getName())
                    .append(" | Oraș: ").append(loc.getCity())
                    .append(" | Județ: ").append(loc.getCounty())
                    .append(" | Categorie: ").append(loc.getCategory())
                    .append("\n");
        }

        return "Ești un ghid turistic expert specializat în România." +
                " Răspunde EXCLUSIV cu JSON valid, fără text extra, fără ```json, fără explicații.\n\n" +

                "PREFERINȚELE UTILIZATORULUI:\n" +
                "- Tip vacanță: " + request.getVacationType() + "\n" +
                "- Buget: " + request.getBudget() + "\n" +
                "- Luna: " + request.getMonth() + "\n" +
                "- Număr zile: " + request.getDaysCount() + "\n" +
                "- Companie: " + request.getCompany() + "\n" +
                "- Ritm: " + request.getRhythm() +
                " (" + request.getLocationsPerDay() + " locație/locații pe zi)\n\n" +

                "LOCAȚIILE DISPONIBILE ÎN SISTEMUL NOSTRU:\n" +
                locList.toString() + "\n" +

                "REGULI STRICTE:\n" +
                "1. Folosește STRICT locațiile din lista de mai sus, cu numele exact\n" +
                "2. Include exact " + request.getLocationsPerDay() + " locație/zi\n" +
                "3. Nu repeta aceeași locație în zile diferite\n" +
                "4. Grupează locațiile geografic (aceeași zonă în aceeași zi)\n" +
                "5. Estimează prețuri reale de intrare în RON pentru " + request.getMonth() + "\n" +
                "6. Recomandă hoteluri reale existente în România, cu preț în RON/noapte\n" +
                "7. Adaptează totul pentru: " + request.getCompany() + "\n" +
                "8. Buget " + request.getBudget() + ": " + getBudgetGuideline(request.getBudget()) + "\n\n" +

                "FORMAT JSON OBLIGATORIU (respectă exact structura):\n" +
                "{\n" +
                "  \"days\": [\n" +
                "    {\n" +
                "      \"dayNumber\": 1,\n" +
                "      \"locations\": [\n" +
                "        {\n" +
                "          \"name\": \"Numele exact din lista noastră\",\n" +
                "          \"city\": \"Orașul\",\n" +
                "          \"county\": \"Județul\",\n" +
                "          \"entryPrice\": \"~XX RON/persoană\",\n" +
                "          \"visitDuration\": \"X-Y ore\",\n" +
                "          \"tip\": \"Sfat practic de vizitare\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"hotelRecommendation\": \"Numele Hotelului ★★★ — ~XXX RON/noapte\",\n" +
                "      \"estimatedDayBudget\": \"XXX-YYY RON/persoană\",\n" +
                "      \"dayTip\": \"Sfat general pentru această zi\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"generalTips\": \"2-3 sfaturi generale pentru această vacanță\"\n" +
                "}";
    }

    private String getBudgetGuideline(String budget) {
        switch (budget) {
            case "Economic": return "pensiuni, prețuri sub 400 RON/noapte";
            case "Mediu":    return "hoteluri 3 stele, 400-800 RON/noapte";
            case "Premium":  return "hoteluri 4-5 stele, peste 800 RON/noapte";
            default:         return "hoteluri cu raport calitate-preț bun";
        }
    }

    // -----------------------------------------------
    // PASUL 3: Apel HTTP către Gemini API
    // -----------------------------------------------
    private void callGeminiApi(String prompt,
                               AiWizardViewModel viewModel,
                               GeminiCallback callback) {

        // Construim body-ul cererii
        JSONObject requestBody = new JSONObject();
        try {
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            requestBody.put("contents", contents);

            // Configurare generare — temperature mai mică = răspuns mai consistent
            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.4);
            genConfig.put("maxOutputTokens", 3000);
            requestBody.put("generationConfig", genConfig);

        } catch (JSONException e) {
            viewModel.setError("Eroare internă: " + e.getMessage());
            callback.onFailure(e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(GEMINI_URL + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                viewModel.setError("Eroare de rețea. Verifică internetul.");
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String err = "Eroare server: " + response.code();
                    viewModel.setError(err);
                    callback.onFailure(err);
                    return;
                }
                String responseData = response.body().string();
                parseGeminiResponse(responseData, viewModel, callback);
            }
        });
    }

    // -----------------------------------------------
    // PASUL 4: Parsăm răspunsul JSON de la Gemini
    // -----------------------------------------------
    private void parseGeminiResponse(String responseData,
                                     AiWizardViewModel viewModel,
                                     GeminiCallback callback) {
        try {
            // Extragem textul din răspunsul Gemini
            JSONObject jsonResponse = new JSONObject(responseData);
            String text = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            // Curățăm markdown-ul dacă Gemini l-a adăugat
            text = text.trim();
            if (text.startsWith("```json")) text = text.substring(7);
            if (text.startsWith("```"))     text = text.substring(3);
            if (text.endsWith("```"))       text = text.substring(0, text.length() - 3);
            text = text.trim();

            // Transformăm JSON-ul în obiect AiPlan cu Gson
            Gson gson = new Gson();
            AiPlan plan = gson.fromJson(text, AiPlan.class);

            if (plan == null || plan.getDays() == null) {
                viewModel.setError("Răspuns invalid de la AI. Încearcă din nou.");
                callback.onFailure("Plan null");
                return;
            }

            viewModel.setPlanResult(plan);
            callback.onSuccess(plan);

        } catch (Exception e) {
            viewModel.setError("Eroare procesare răspuns AI. Încearcă din nou.");
            callback.onFailure(e.getMessage());
        }
    }
}