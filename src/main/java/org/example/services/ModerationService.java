package org.example.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service de modération du forum.
 * - Filtre local de mots interdits
 * - Rate limiting (1 post / 60 sec)
 * - Vérification externe via PurgoMalum API
 */
public class ModerationService {

    // Mots interdits (filtre local)
    private static final List<String> BANNED_WORDS = Arrays.asList(
            "fuck", "shit", "piss", "dick", "asshole", "bitch", "bastard", "cunt",
            "faggot", "retard", "slut", "whore", "scam", "crypto", "porn"
    );

    // Rate limiting
    private static final Map<String, LocalDateTime> userLastPostTime = new HashMap<>();
    private static final int COOLDOWN_SECONDS = 60;

    /** Vérifie si l'utilisateur est limité en débit */
    public static boolean isRateLimited(String username) {
        if (userLastPostTime.containsKey(username)) {
            LocalDateTime lastPost = userLastPostTime.get(username);
            return lastPost.plusSeconds(COOLDOWN_SECONDS).isAfter(LocalDateTime.now());
        }
        return false;
    }

    /** Enregistre le timestamp du dernier post */
    public static void recordPost(String username) {
        userLastPostTime.put(username, LocalDateTime.now());
    }

    /** Vérifie via l'API PurgoMalum (gratuite, sans clé) */
    public static boolean checkExternalAPI(String text) {
        try {
            String encodedText = text.replace(" ", "%20");
            String url = "https://www.purgomalum.com/service/containsprofanity?text=" + encodedText;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return Boolean.parseBoolean(response.body().trim());
        } catch (Exception e) {
            System.out.println("⚠️ Erreur API modération : " + e.getMessage());
            return false;
        }
    }

    /**
     * Validation complète du contenu d'un post.
     * @throws Exception si le contenu est inapproprié
     */
    public static void validateContent(String title, String content, String role) throws Exception {
        String fullText = (title + " " + content).toLowerCase();

        // Couche 1 : Mots interdits locaux
        for (String word : BANNED_WORDS) {
            if (fullText.matches(".*\\b" + word + "\\b.*")) {
                throw new Exception("Contenu inapproprié détecté : mot interdit [" + word + "]");
            }
        }

        // Couche 2 : Règles par rôle
        if ("RH".equalsIgnoreCase(role)) {
            if (content.length() < 30) {
                throw new Exception("Les posts RH doivent contenir au moins 30 caractères.");
            }
        }

        // Couche 3 : API externe
        if (checkExternalAPI(fullText)) {
            throw new Exception("Filtre IA externe : Contenu inapproprié détecté.");
        }
    }
}
