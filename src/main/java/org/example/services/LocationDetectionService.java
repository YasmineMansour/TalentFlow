package org.example.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Détecte la localisation de l'utilisateur via son IP.
 * Utilise l'API gratuite ip-api.com.
 */
public class LocationDetectionService {

    public static String getCityLocation() {
        try {
            URL url = new URL("http://ip-api.com/json/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            in.close();

            // Parse JSON manually
            String json = content.toString();
            String city = extractJsonString(json, "city");
            String country = extractJsonString(json, "countryCode");

            if (city != null && country != null) {
                return city + ", " + country;
            }
        } catch (Exception e) {
            System.err.println("Erreur API Location : " + e.getMessage());
        }
        return "Localisation indisponible";
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx >= 0) {
            int start = idx + search.length();
            int end = json.indexOf("\"", start);
            if (end > start) return json.substring(start, end);
        }
        return null;
    }
}
