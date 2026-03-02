package org.example.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Détecte la langue d'un texte via l'API DetectLanguage.
 */
public class LanguageDetectionService {

    private static final String API_KEY = "db6a8b686bcb4e39fb875d6a0f1b55b2";

    public static String detectLanguage(String text) {
        if (text == null || text.trim().length() < 10) return "Texte trop court";

        try {
            URL url = new URL("https://ws.detectlanguage.com/0.2/detect");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);

            String postData = "q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes());
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) response.append(line);
            in.close();

            // Parse JSON manually
            String jsonStr = response.toString();
            int langIdx = jsonStr.indexOf("\"language\":\"");
            if (langIdx >= 0) {
                int start = langIdx + "\"language\":\"".length();
                int end = jsonStr.indexOf("\"", start);
                if (end > start) {
                    String langCode = jsonStr.substring(start, end).toUpperCase();

                    // Check confidence
                    int confIdx = jsonStr.indexOf("\"confidence\":", langIdx);
                    if (confIdx >= 0) {
                        int confStart = confIdx + "\"confidence\":".length();
                        int confEnd = jsonStr.indexOf(",", confStart);
                        if (confEnd < 0) confEnd = jsonStr.indexOf("}", confStart);
                        if (confEnd > confStart) {
                            try {
                                double confidence = Double.parseDouble(jsonStr.substring(confStart, confEnd).trim());
                                if (confidence < 0.5) {
                                    return langCode + " (Probable)";
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    return langCode;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur API Langue : " + e.getMessage());
        }
        return "Analyse impossible";
    }
}
