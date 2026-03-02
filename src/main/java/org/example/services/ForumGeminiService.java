package org.example.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Service d'IA utilisant l'API Gemini pour le forum TalentFlow.
 * - Génération de réponses IA
 * - Détection de tonalité des posts
 */
public class ForumGeminiService {

    private static final String API_KEY = "AIzaSyDIWBdbTTJYXpZOT0AcaV_AzetTPsC1RwY";
    private static final String MODEL = "gemini-2.5-flash";
    private static final OkHttpClient client = new OkHttpClient();

    /** Génère une réponse IA via Gemini */
    public static String generateResponse(String prompt) throws IOException {
        String url = "https://generativelanguage.googleapis.com/v1/models/"
                + MODEL + ":generateContent?key=" + API_KEY;

        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONArray partsArray = new JSONArray();
        partsArray.put(textPart);

        JSONObject contentObject = new JSONObject();
        contentObject.put("parts", partsArray);

        JSONArray contentsArray = new JSONArray();
        contentsArray.put(contentObject);

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("contents", contentsArray);

        RequestBody body = RequestBody.create(
                bodyJson.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                return "Erreur Gemini API (" + response.code() + "):\n" + responseBody;
            }
            return extractText(responseBody);
        }
    }

    private static String extractText(String jsonResponse) {
        JSONObject json = new JSONObject(jsonResponse);
        if (!json.has("candidates")) {
            return "L'IA n'a pas retourné de réponse utilisable.";
        }
        JSONArray candidates = json.getJSONArray("candidates");
        JSONObject firstCandidate = candidates.getJSONObject(0);
        JSONObject content = firstCandidate.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        return parts.getJSONObject(0).getString("text");
    }

    /** Détecte la tonalité d'un texte (pos/neutral/neg) */
    public static String detectTone(String text) throws IOException {
        String prompt = """
                Classify the tone of the following text.
                Respond with ONLY one word:
                POSITIVE, NEUTRAL, or HOSTILE.
                
                Text:
                """ + text;

        String response = generateResponse(prompt).trim().toUpperCase();
        if (response.contains("POSITIVE")) return "pos";
        if (response.contains("HOSTILE")) return "neg";
        return "neutral";
    }
}
