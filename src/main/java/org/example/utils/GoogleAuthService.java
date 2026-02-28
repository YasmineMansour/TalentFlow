package org.example.utils;

import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service d'authentification Google OAuth 2.0 pour application desktop.
 *
 * CONFIGURATION REQUISE :
 * 1. Créez un projet sur Google Cloud Console : https://console.cloud.google.com/
 * 2. Activez l'API "Google People API" ou "Google+ API"
 * 3. Créez des identifiants OAuth 2.0 (type : Application de bureau)
 * 4. Ajoutez http://localhost:8888/callback comme URI de redirection autorisée
 * 5. Créez un fichier google-oauth.properties à la racine du projet avec CLIENT_ID et CLIENT_SECRET
 */
public class GoogleAuthService {

    // ===== CONFIGURATION GOOGLE OAUTH (chargée depuis google-oauth.properties ou variables d'env) =====
    private static final String CLIENT_ID;
    private static final String CLIENT_SECRET;
    private static final int    CALLBACK_PORT = 8888;
    private static final String REDIRECT_URI  = "http://localhost:" + CALLBACK_PORT + "/callback";

    // Endpoints Google OAuth 2.0
    private static final String AUTH_URL     = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL    = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    static {
        // Charger depuis le fichier google-oauth.properties à la racine du projet
        String id = null;
        String secret = null;
        try {
            java.io.File propsFile = new java.io.File("google-oauth.properties");
            if (propsFile.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(propsFile)) {
                    props.load(fis);
                }
                id = props.getProperty("CLIENT_ID", "").trim();
                secret = props.getProperty("CLIENT_SECRET", "").trim();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lecture google-oauth.properties : " + e.getMessage());
        }
        // Fallback : variables d'environnement
        if (id == null || id.isEmpty()) id = System.getenv("GOOGLE_CLIENT_ID");
        if (secret == null || secret.isEmpty()) secret = System.getenv("GOOGLE_CLIENT_SECRET");
        CLIENT_ID = (id != null) ? id : "";
        CLIENT_SECRET = (secret != null) ? secret : "";
    }

    /** Vérifie si les identifiants Google sont configurés */
    public static boolean isConfigured() {
        return !CLIENT_ID.isEmpty() && !CLIENT_SECRET.isEmpty();
    }

    // ============================
    //   Modèle GoogleUserInfo
    // ============================
    public static class GoogleUserInfo {
        private String id;
        private String email;
        private String givenName;   // Prénom
        private String familyName;  // Nom
        private String name;        // Nom complet

        public String getId()         { return id; }
        public String getEmail()      { return email; }
        public String getGivenName()  { return givenName; }
        public String getFamilyName() { return familyName; }
        public String getName()       { return name; }
    }

    // ============================
    //   Flux OAuth principal
    // ============================

    /**
     * Lance le flux OAuth Google :
     * 1. Démarre un serveur HTTP local pour recevoir le callback
     * 2. Ouvre le navigateur sur la page de connexion Google
     * 3. Reçoit le code d'autorisation
     * 4. Échange le code contre un token d'accès
     * 5. Récupère les informations utilisateur
     *
     * @return CompletableFuture contenant les infos de l'utilisateur Google
     */
    public static CompletableFuture<GoogleUserInfo> authenticate() {
        CompletableFuture<GoogleUserInfo> future = new CompletableFuture<>();

        new Thread(() -> {
            HttpServer server = null;
            try {
                server = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);
                final HttpServer finalServer = server;

                server.createContext("/callback", exchange -> {
                    try {
                        String query = exchange.getRequestURI().getQuery();
                        String code = extractParam(query, "code");

                        String html;
                        if (code != null) {
                            // Page de succès affichée dans le navigateur
                            html = buildSuccessPage();

                            // Échanger le code contre les infos utilisateur
                            try {
                                String accessToken = exchangeCodeForToken(code);
                                GoogleUserInfo userInfo = getUserInfo(accessToken);
                                future.complete(userInfo);
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        } else {
                            // Erreur ou annulation
                            String error = extractParam(query, "error");
                            html = buildErrorPage(error);
                            future.completeExceptionally(
                                    new Exception("Authentification Google annulée : " + (error != null ? error : "inconnue"))
                            );
                        }

                        // Envoyer la réponse HTML au navigateur
                        byte[] response = html.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response);
                        }
                    } finally {
                        // Arrêter le serveur après le callback
                        new Thread(() -> {
                            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                            finalServer.stop(0);
                        }).start();
                    }
                });

                server.start();
                System.out.println("🌐 Serveur OAuth local démarré sur le port " + CALLBACK_PORT);

                // Construire l'URL d'autorisation et ouvrir le navigateur
                String authUrl = AUTH_URL
                        + "?client_id="    + encode(CLIENT_ID)
                        + "&redirect_uri=" + encode(REDIRECT_URI)
                        + "&response_type=code"
                        + "&scope="        + encode("email profile")
                        + "&access_type=offline"
                        + "&prompt=select_account";

                Desktop.getDesktop().browse(URI.create(authUrl));

            } catch (IOException e) {
                System.err.println("❌ Erreur lors du lancement de l'authentification Google : " + e.getMessage());
                future.completeExceptionally(e);
                if (server != null) server.stop(0);
            }
        }).start();

        return future;
    }

    // ============================
    //   Échange code → token
    // ============================
    private static String exchangeCodeForToken(String code) throws Exception {
        String body = "code="          + encode(code)
                + "&client_id="        + encode(CLIENT_ID)
                + "&client_secret="    + encode(CLIENT_SECRET)
                + "&redirect_uri="     + encode(REDIRECT_URI)
                + "&grant_type=authorization_code";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String accessToken = extractJsonValue(response.body(), "access_token");

        if (accessToken == null) {
            throw new Exception("Impossible d'obtenir le token Google. Réponse : " + response.body());
        }

        System.out.println("✅ Token d'accès Google obtenu avec succès.");
        return accessToken;
    }

    // ============================
    //   Récupération profil Google
    // ============================
    private static GoogleUserInfo getUserInfo(String accessToken) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body();

        GoogleUserInfo info = new GoogleUserInfo();
        info.id         = extractJsonValue(json, "id");
        info.email      = extractJsonValue(json, "email");
        info.givenName  = extractJsonValue(json, "given_name");
        info.familyName = extractJsonValue(json, "family_name");
        info.name       = extractJsonValue(json, "name");

        if (info.email == null) {
            throw new Exception("Impossible de récupérer l'email depuis Google. Réponse : " + json);
        }

        System.out.println("✅ Profil Google récupéré : " + info.email);
        return info;
    }

    // ============================
    //   Utilitaires
    // ============================

    /** Encode un paramètre URL */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Extrait la valeur d'un paramètre dans une query string */
    private static String extractParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /** Extrait une valeur string d'un JSON simple (sans bibliothèque externe) */
    private static String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    // ============================
    //   Pages HTML de réponse
    // ============================

    private static String buildSuccessPage() {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><title>TalentFlow - Connexion Google</title></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; text-align: center; padding: 60px;
                         background: linear-gradient(135deg, #f0f2f5, #e8eaf6);">
                <div style="max-width: 450px; margin: 0 auto; background: white; border-radius: 16px;
                            padding: 40px; box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                    <h1 style="color: #00b894; font-size: 28px;">✅ Connexion réussie !</h1>
                    <p style="color: #636e72; font-size: 15px; margin-top: 15px;">
                        Votre compte Google a été connecté avec succès à TalentFlow.
                    </p>
                    <p style="color: #b2bec3; font-size: 13px; margin-top: 20px;">
                        Vous pouvez fermer cette fenêtre et retourner à l'application.
                    </p>
                </div>
            </body></html>
            """;
    }

    private static String buildErrorPage(String error) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><title>TalentFlow - Erreur</title></head>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; text-align: center; padding: 60px;
                         background: linear-gradient(135deg, #f0f2f5, #ffeaea);">
                <div style="max-width: 450px; margin: 0 auto; background: white; border-radius: 16px;
                            padding: 40px; box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                    <h1 style="color: #d63031; font-size: 28px;">❌ Connexion annulée</h1>
                    <p style="color: #636e72; font-size: 15px; margin-top: 15px;">
                        L'authentification Google a été annulée ou a échoué.
                    </p>
                    <p style="color: #b2bec3; font-size: 13px; margin-top: 10px;">
                        Erreur : %s
                    </p>
                    <p style="color: #b2bec3; font-size: 13px; margin-top: 20px;">
                        Vous pouvez fermer cette fenêtre et réessayer dans l'application.
                    </p>
                </div>
            </body></html>
            """.formatted(error != null ? error : "inconnue");
    }
}
