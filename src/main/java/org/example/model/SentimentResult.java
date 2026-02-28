package org.example.model;

/**
 * Résultat de l'analyse de sentiment d'un texte (lettre de motivation, description de profil).
 * Utilise l'API OpenAI pour déterminer le ton du candidat.
 */
public class SentimentResult {

    public enum Sentiment {
        MOTIVE("🔥 Motivé", "#00b894", "Le candidat exprime une forte motivation et de l'enthousiasme."),
        CONFIANT("💪 Confiant", "#0984e3", "Le candidat fait preuve d'assurance et de confiance en ses compétences."),
        NEUTRE("😐 Neutre", "#636e72", "Le ton du candidat est professionnel mais sans émotion marquée."),
        INCERTAIN("❓ Incertain", "#fdcb6e", "Le candidat semble hésitant ou manque de conviction."),
        NEGATIF("⚠️ Négatif", "#d63031", "Le ton du candidat exprime de l'insatisfaction ou du découragement."),
        ERREUR("❌ Erreur", "#b2bec3", "Impossible d'analyser le sentiment.");

        private final String label;
        private final String color;
        private final String description;

        Sentiment(String label, String color, String description) {
            this.label = label;
            this.color = color;
            this.description = description;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
        public String getDescription() { return description; }
    }

    private final Sentiment sentiment;
    private final double confidenceScore;   // Confiance de l'IA (0.0 - 1.0)
    private final String summary;           // Résumé généré par l'IA
    private final String rawResponse;       // Réponse brute de l'API

    public SentimentResult(Sentiment sentiment, double confidenceScore, String summary, String rawResponse) {
        this.sentiment = sentiment;
        this.confidenceScore = confidenceScore;
        this.summary = summary;
        this.rawResponse = rawResponse;
    }

    /** Crée un résultat d'erreur */
    public static SentimentResult error(String errorMessage) {
        return new SentimentResult(Sentiment.ERREUR, 0.0, errorMessage, "");
    }

    // GETTERS
    public Sentiment getSentiment() { return sentiment; }
    public double getConfidenceScore() { return confidenceScore; }
    public String getSummary() { return summary; }
    public String getRawResponse() { return rawResponse; }

    /** Confiance en pourcentage */
    public double getConfidencePercentage() {
        return Math.round(confidenceScore * 100.0 * 10.0) / 10.0;
    }

    @Override
    public String toString() {
        return String.format("%s (Confiance: %.1f%%) — %s",
                sentiment.getLabel(), getConfidencePercentage(), summary);
    }
}
