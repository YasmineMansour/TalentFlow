package org.example.utils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service d'envoi de SMS via l'API REST Twilio.
 * Utilise java.net.http.HttpClient (pas besoin de dépendance externe).
 *
 * CONFIGURATION REQUISE :
 * 1. Créez un compte Twilio : https://www.twilio.com/
 * 2. Récupérez votre Account SID et Auth Token depuis la console
 * 3. Obtenez un numéro Twilio (FROM_NUMBER)
 * 4. Remplacez les valeurs ci-dessous
 */
public class SmsService {

    // ===== CONFIGURATION TWILIO — À MODIFIER =====
    private static final String ACCOUNT_SID = "VOTRE_ACCOUNT_SID";         // ← Remplacez
    private static final String AUTH_TOKEN = "VOTRE_AUTH_TOKEN";           // ← Remplacez
    private static final String FROM_NUMBER = "+1234567890";               // ← Votre numéro Twilio

    /** Vérifie si le service Twilio est configuré */
    public static boolean isConfigured() {
        return !ACCOUNT_SID.equals("VOTRE_ACCOUNT_SID");
    }

    /**
     * Envoie un SMS via l'API REST de Twilio.
     * @param to numéro du destinataire (format international, ex : +21612345678)
     * @param message contenu du SMS
     * @return true si l'envoi a réussi
     */
    public static boolean sendSms(String to, String message) {
        if (!isConfigured()) {
            System.err.println("⚠️ SmsService non configuré. Modifiez ACCOUNT_SID, AUTH_TOKEN et FROM_NUMBER.");
            return false;
        }

        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";

            String body = "To=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(FROM_NUMBER, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            String auth = Base64.getEncoder().encodeToString(
                    (ACCOUNT_SID + ":" + AUTH_TOKEN).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println("✅ SMS envoyé à : " + to);
                return true;
            } else {
                System.err.println("❌ Erreur SMS (HTTP " + response.statusCode() + ") : " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi SMS : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envoie un code de vérification par SMS.
     * @param to numéro de téléphone (format international)
     * @param code le code à 6 chiffres
     */
    public static boolean sendVerificationCode(String to, String code) {
        return sendSms(to, "🔐 TalentFlow - Votre code de vérification : " + code + "\nValide pendant 5 minutes.");
    }

    /**
     * Formate un numéro de téléphone tunisien en format international.
     * Ex: 12345678 → +21612345678
     */
    public static String formatPhoneNumber(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("[^0-9+]", "");

        if (phone.startsWith("+")) return phone;
        if (phone.length() == 8) return "+216" + phone;  // Tunisie
        return "+" + phone;
    }
}
