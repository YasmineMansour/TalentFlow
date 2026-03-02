package org.example.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service Cloud pour le forum TalentFlow.
 * - Upload d'images via ImgBB
 * - Traduction via Google Translate
 * - Détection de tonalité via Gemini AI
 * - Assistant IA
 */
public class ForumCloudService {

    private static final String IMGBB_API_KEY = "a5e795583aab3d16d98569b05fb8c26e";

    // --- UPLOAD IMAGE VERS IMGBB ---
    public static String uploadImage(File file) throws IOException {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(file, MediaType.parse("image/*")))
                .build();

        Request request = new Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=" + IMGBB_API_KEY)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Upload échoué : " + response);
            JSONObject json = new JSONObject(response.body().string());
            return json.getJSONObject("data").getString("url");
        }
    }

    // --- TRADUCTION VIA GOOGLE TRANSLATE ---
    public static String translate(String text, String targetLang) throws IOException {
        if (text == null || text.trim().isEmpty()) return "";

        OkHttpClient client = new OkHttpClient();
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                + targetLang + "&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            JSONArray jsonArray = new JSONArray(response.body().string());
            JSONArray sentences = jsonArray.getJSONArray(0);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < sentences.length(); i++) {
                result.append(sentences.getJSONArray(i).getString(0));
            }
            return result.toString();
        }
    }

    // --- DÉTECTION DE TONALITÉ VIA GEMINI ---
    public static String detectTone(String text) {
        try {
            return ForumGeminiService.detectTone(text);
        } catch (Exception e) {
            return "neutral";
        }
    }

    // --- ASSISTANT IA ---
    public static String askAI(String prompt) {
        try {
            return ForumGeminiService.generateResponse(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "Service IA temporairement indisponible.";
        }
    }
}
