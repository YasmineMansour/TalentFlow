package org.example.utils;

import org.example.model.SentimentResult;
import org.example.model.SentimentResult.Sentiment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 📝 Service d'Analyse de Sentiment (NLP - Traitement du Langage Naturel)
 *
 * Analyse le ton d'une lettre de motivation ou description de profil
 * via l'API OpenAI (GPT) pour déterminer si le candidat est :
 * Motivé, Confiant, Neutre, Incertain ou Négatif.
 *
 * CONFIGURATION REQUISE :
 * 1. Obtenez une clé API sur https://platform.openai.com/api-keys
 * 2. Remplacez OPENAI_API_KEY ci-dessous
 *
 * Utilise java.net.http.HttpClient (pas de dépendances externes).
 */
public class SentimentAnalysisService {

    // ===== CONFIGURATION OpenAI — À MODIFIER =====
    private static final String OPENAI_API_KEY = "VOTRE_CLE_API_OPENAI";  // ← Remplacez par votre clé API
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-3.5-turbo";
    private static final int TIMEOUT_SECONDS = 30;

    /** Vérifie si le service est configuré avec une vraie clé API */
    public static boolean isConfigured() {
        return OPENAI_API_KEY != null
                && !OPENAI_API_KEY.isBlank()
                && !OPENAI_API_KEY.equals("VOTRE_CLE_API_OPENAI");
    }

    // ===========================
    //   ANALYSE DE SENTIMENT
    // ===========================

    /**
     * Analyse le sentiment d'un texte (lettre de motivation, bio, description).
     *
     * @param text le texte à analyser
     * @return SentimentResult contenant le sentiment, la confiance et un résumé
     */
    public static SentimentResult analyzeSentiment(String text) {
        if (text == null || text.isBlank()) {
            return SentimentResult.error("Texte vide — impossible d'analyser.");
        }

        // Si l'API n'est pas configurée, utiliser l'analyse locale
        if (!isConfigured()) {
            System.out.println("⚠️ API OpenAI non configurée. Utilisation de l'analyse locale.");
            return analyzeLocally(text);
        }

        try {
            return analyzeWithOpenAI(text);
        } catch (Exception e) {
            System.err.println("❌ Erreur API OpenAI : " + e.getMessage());
            // Fallback vers l'analyse locale
            return analyzeLocally(text);
        }
    }

    // ===========================
    //   ANALYSE VIA OPENAI API
    // ===========================

    /**
     * Appelle l'API OpenAI pour analyser le sentiment du texte.
     */
    private static SentimentResult analyzeWithOpenAI(String text) throws Exception {
        // Limiter la taille du texte envoyé
        String truncatedText = text.length() > 2000 ? text.substring(0, 2000) + "..." : text;

        String prompt = """
                Analyse le sentiment du texte suivant, qui est une lettre de motivation ou description de profil d'un candidat.
                
                Réponds UNIQUEMENT au format suivant (3 lignes exactement) :
                SENTIMENT: [MOTIVE|CONFIANT|NEUTRE|INCERTAIN|NEGATIF]
                CONFIANCE: [un nombre entre 0.0 et 1.0]
                RESUME: [une phrase de résumé en français]
                
                Texte à analyser :
                \"\"\"%s\"\"\"
                """.formatted(truncatedText);

        // Construire le JSON de la requête (sans bibliothèque externe)
        String requestBody = """
                {
                    "model": "%s",
                    "messages": [
                        {"role": "system", "content": "Tu es un expert en analyse de sentiment spécialisé dans le recrutement. Réponds toujours exactement au format demandé."},
                        {"role": "user", "content": %s}
                    ],
                    "temperature": 0.3,
                    "max_tokens": 150
                }
                """.formatted(MODEL, escapeJsonString(prompt));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("❌ OpenAI API erreur " + response.statusCode() + " : " + response.body());
            return analyzeLocally(text); // Fallback
        }

        return parseOpenAIResponse(response.body());
    }

    /**
     * Parse la réponse JSON de l'API OpenAI (sans dépendance JSON externe).
     */
    private static SentimentResult parseOpenAIResponse(String jsonResponse) {
        try {
            // Extraire le contenu du message de la réponse
            String content = extractJsonValue(jsonResponse, "content");
            if (content == null || content.isBlank()) {
                return SentimentResult.error("Réponse vide de l'API.");
            }

            // Parser les lignes de la réponse
            String sentimentStr = "";
            double confidence = 0.5;
            String summary = "Analyse non disponible.";

            for (String line : content.split("\\n")) {
                line = line.trim();
                if (line.startsWith("SENTIMENT:")) {
                    sentimentStr = line.substring("SENTIMENT:".length()).trim();
                } else if (line.startsWith("CONFIANCE:")) {
                    try {
                        confidence = Double.parseDouble(line.substring("CONFIANCE:".length()).trim());
                    } catch (NumberFormatException ignored) {}
                } else if (line.startsWith("RESUME:")) {
                    summary = line.substring("RESUME:".length()).trim();
                }
            }

            Sentiment sentiment = parseSentiment(sentimentStr);

            System.out.println("📝 Analyse IA — " + sentiment.getLabel() + " (Confiance: "
                    + String.format("%.0f%%", confidence * 100) + ")");

            return new SentimentResult(sentiment, confidence, summary, jsonResponse);

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing réponse OpenAI : " + e.getMessage());
            return SentimentResult.error("Erreur de parsing de la réponse API.");
        }
    }

    // ===========================
    //   ANALYSE LOCALE (FALLBACK)
    // ===========================

    /**
     * Analyse de sentiment basique sans API externe.
     * Utilise un système de scoring par mots-clés pondérés.
     * Sert de fallback si l'API OpenAI n'est pas configurée ou échoue.
     */
    public static SentimentResult analyzeLocally(String text) {
        if (text == null || text.isBlank()) {
            return SentimentResult.error("Texte vide.");
        }

        String lower = text.toLowerCase();

        // Mots-clés positifs (motivation / confiance)
        String[] motiveWords = {
                "passionné", "passionnée", "motivé", "motivée", "enthousiaste",
                "déterminé", "déterminée", "ambition", "ambitieux", "ambitieuse",
                "dynamique", "proactif", "proactive", "engagement", "dédié", "dédiée",
                "hâte", "impatient", "impatiente", "rêve", "aspire", "vocation",
                "épanouir", "challenge", "défi", "excité", "excitée"
        };

        String[] confidentWords = {
                "expérience", "expert", "experte", "maîtrise", "compétent", "compétente",
                "capable", "qualifié", "qualifiée", "solide", "expertise", "performant",
                "performante", "efficace", "rigoureux", "rigoureuse", "professionnel",
                "professionnelle", "accompli", "accomplie", "réussi", "succès"
        };

        // Mots-clés négatifs (incertitude / négativité)
        String[] uncertainWords = {
                "peut-être", "éventuellement", "possible", "hésitant", "hésitante",
                "espère", "essayer", "tenter", "penser que", "croire que",
                "pas sûr", "pas certain", "doute", "incertain", "incertaine"
        };

        String[] negativeWords = {
                "malheureusement", "difficile", "problème", "échec", "frustr",
                "déçu", "déçue", "insatisf", "ennuy", "contraint", "obligé",
                "stress", "anxieux", "anxieuse", "fatigué", "fatiguée",
                "démotivé", "démotivée", "désespoir"
        };

        // Calculer les scores
        int motiveScore = countMatches(lower, motiveWords);
        int confidentScore = countMatches(lower, confidentWords);
        int uncertainScore = countMatches(lower, uncertainWords);
        int negativeScore = countMatches(lower, negativeWords);

        int totalPositive = motiveScore + confidentScore;
        int totalNegative = uncertainScore + negativeScore;
        int totalSignals = totalPositive + totalNegative;

        // Configuration des seuils
        Sentiment sentiment;
        double confidence;
        String summary;

        if (totalSignals == 0) {
            sentiment = Sentiment.NEUTRE;
            confidence = 0.4;
            summary = "Le texte est professionnel mais ne révèle pas d'émotion particulière.";
        } else if (motiveScore > confidentScore && motiveScore > totalNegative) {
            sentiment = Sentiment.MOTIVE;
            confidence = Math.min(0.9, 0.5 + motiveScore * 0.08);
            summary = "Le candidat exprime une forte motivation avec " + motiveScore + " indicateurs positifs détectés.";
        } else if (confidentScore > motiveScore && confidentScore > totalNegative) {
            sentiment = Sentiment.CONFIANT;
            confidence = Math.min(0.9, 0.5 + confidentScore * 0.08);
            summary = "Le candidat fait preuve d'assurance avec " + confidentScore + " marqueurs de confiance.";
        } else if (totalPositive > totalNegative) {
            sentiment = (motiveScore >= confidentScore) ? Sentiment.MOTIVE : Sentiment.CONFIANT;
            confidence = Math.min(0.85, 0.4 + totalPositive * 0.06);
            summary = "Tonalité globalement positive avec " + totalPositive + " signaux positifs vs " + totalNegative + " négatifs.";
        } else if (negativeScore > uncertainScore) {
            sentiment = Sentiment.NEGATIF;
            confidence = Math.min(0.85, 0.4 + negativeScore * 0.08);
            summary = "Le ton exprime de l'insatisfaction avec " + negativeScore + " marqueurs négatifs détectés.";
        } else if (uncertainScore > 0) {
            sentiment = Sentiment.INCERTAIN;
            confidence = Math.min(0.85, 0.4 + uncertainScore * 0.08);
            summary = "Le candidat semble hésitant avec " + uncertainScore + " marqueurs d'incertitude.";
        } else {
            sentiment = Sentiment.NEUTRE;
            confidence = 0.5;
            summary = "Signal mixte : " + totalPositive + " positifs / " + totalNegative + " négatifs.";
        }

        System.out.println("📝 Analyse locale — " + sentiment.getLabel() + " (Confiance: "
                + String.format("%.0f%%", confidence * 100) + ")");

        return new SentimentResult(sentiment, confidence, summary, "analyse_locale");
    }

    // ===========================
    //   UTILITAIRES
    // ===========================

    /** Compte le nombre de correspondances de mots-clés dans le texte */
    private static int countMatches(String text, String[] keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    /** Parse un sentiment depuis une chaîne */
    private static Sentiment parseSentiment(String str) {
        if (str == null) return Sentiment.NEUTRE;
        return switch (str.toUpperCase().trim()) {
            case "MOTIVE", "MOTIVÉ", "MOTIVÉE" -> Sentiment.MOTIVE;
            case "CONFIANT", "CONFIANTE" -> Sentiment.CONFIANT;
            case "INCERTAIN", "INCERTAINE" -> Sentiment.INCERTAIN;
            case "NEGATIF", "NÉGATIF", "NEGATIVE", "NÉGATIVE" -> Sentiment.NEGATIF;
            default -> Sentiment.NEUTRE;
        };
    }

    /**
     * Extrait la valeur d'une clé dans un JSON (parsing simplifié sans bibliothèque).
     * Fonctionne pour les valeurs string simples.
     */
    private static String extractJsonValue(String json, String key) {
        // Chercher "content": "..." dans la réponse
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        // Trouver le début de la valeur après les ":"
        int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
        if (colonIndex == -1) return null;

        // Trouver le début et la fin de la valeur string
        int startQuote = json.indexOf("\"", colonIndex + 1);
        if (startQuote == -1) return null;

        // Trouver la fin de la string en gérant les échappements
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> value.append('\n');
                    case 't' -> value.append('\t');
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    default -> value.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
            }
        }

        return value.toString();
    }

    /** Échappe une string pour un JSON (sans bibliothèque) */
    private static String escapeJsonString(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
