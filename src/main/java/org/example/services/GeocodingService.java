package org.example.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service de geocodage base sur Nominatim (OpenStreetMap).
 * Convertit une adresse textuelle en coordonnees GPS (latitude, longitude).
 */
public class GeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private final Map<String, double[]> cache = new LinkedHashMap<>();
    private long lastRequestTime = 0;

    public double[] geocode(String location) {
        if (location == null || location.isBlank()) return null;

        String key = location.trim().toLowerCase();
        if (cache.containsKey(key)) return cache.get(key);

        try {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTime;
            if (elapsed < 1100) Thread.sleep(1100 - elapsed);
            lastRequestTime = System.currentTimeMillis();

            String query = location.trim();
            String lower = query.toLowerCase();
            if (!lower.contains(",") && !lower.contains("tunisie") && !lower.contains("tunisia")
                    && !lower.contains("france") && !lower.contains("maroc")) {
                query = query + ", Tunisie";
            }

            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlStr = NOMINATIM_URL + "?q=" + encoded + "&format=json&limit=1&accept-language=fr";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "TalentFlow/1.0 (student-project; contact@talentflow.tn)");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            String json = sb.toString();
            double lat = extractJsonDouble(json, "\"lat\"");
            double lon = extractJsonDouble(json, "\"lon\"");

            if (lat != 0 || lon != 0) {
                double[] coords = {lat, lon};
                cache.put(key, coords);
                return coords;
            }
        } catch (Exception e) {
            System.err.println("Erreur geocodage pour '" + location + "' : " + e.getMessage());
        }
        return null;
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double extractJsonDouble(String json, String key) {
        if (json == null || json.trim().equals("[]") || json.trim().isEmpty()) return 0;
        int idx = json.indexOf(key);
        if (idx < 0) return 0;
        int start = json.indexOf(":", idx) + 1;
        if (start <= 0 || start >= json.length()) return 0;
        while (start < json.length() && (json.charAt(start) == '"' || json.charAt(start) == ' ')) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        if (start == end) return 0;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }
}
