package org.example.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Vérifie si une adresse email est réelle ou suspecte.
 * Utilise l'API gratuite MailCheck.ai (aucune clé API requise).
 */
public class EmailValidationService {

    public static String checkEmail(String email) {
        if (email == null || email.isEmpty()) return "Email vide";

        try {
            String urlString = "https://api.mailcheck.ai/email/" + email;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code != 200) {
                System.err.println("API Email: HTTP " + code);
                return "ERREUR";
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) response.append(line);
            in.close();

            // Parse JSON manually to avoid extra dependency
            boolean disposable = response.toString().contains("\"disposable\":true");
            boolean mxExists = response.toString().contains("\"mx\":true");

            if (disposable) {
                return "UNDELIVERABLE";
            } else if (mxExists) {
                return "DELIVERABLE";
            } else {
                return "UNKNOWN";
            }

        } catch (Exception e) {
            System.err.println("Erreur API Email : " + e.getMessage());
            return "ERREUR";
        }
    }
}
