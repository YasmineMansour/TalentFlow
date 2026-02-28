package org.example.model;

import java.util.List;

/**
 * Résultat de la détection de fraude pour un utilisateur.
 * Analyse l'email, le téléphone et les comportements suspects.
 */
public class FraudCheckResult {

    public enum RiskLevel {
        FAIBLE("✅ Risque Faible", "#00b894", false),
        MOYEN("⚠️ Risque Moyen", "#fdcb6e", false),
        ELEVE("🚨 Risque Élevé", "#d63031", true),
        CRITIQUE("🔴 Risque Critique", "#c0392b", true);

        private final String label;
        private final String color;
        private final boolean requiresReview;

        RiskLevel(String label, String color, boolean requiresReview) {
            this.label = label;
            this.color = color;
            this.requiresReview = requiresReview;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
        public boolean isRequiresReview() { return requiresReview; }
    }

    private final double riskScore;          // Score de risque (0.0 - 1.0)
    private final RiskLevel riskLevel;
    private final List<String> flags;        // Liste des signaux d'alerte détectés
    private final boolean flaggedForReview;  // Nécessite une revue manuelle par admin

    public FraudCheckResult(double riskScore, List<String> flags) {
        this.riskScore = riskScore;
        this.flags = flags;

        // Détermination du niveau de risque
        if (riskScore >= 0.80) {
            this.riskLevel = RiskLevel.CRITIQUE;
        } else if (riskScore >= 0.55) {
            this.riskLevel = RiskLevel.ELEVE;
        } else if (riskScore >= 0.30) {
            this.riskLevel = RiskLevel.MOYEN;
        } else {
            this.riskLevel = RiskLevel.FAIBLE;
        }

        this.flaggedForReview = riskLevel.isRequiresReview();
    }

    // GETTERS
    public double getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public List<String> getFlags() { return flags; }
    public boolean isFlaggedForReview() { return flaggedForReview; }

    /** Score de risque en pourcentage */
    public double getRiskPercentage() {
        return Math.round(riskScore * 100.0 * 10.0) / 10.0;
    }

    @Override
    public String toString() {
        return String.format("%s | Score: %.1f%% | Alertes: %d | Revue: %s",
                riskLevel.getLabel(), getRiskPercentage(), flags.size(),
                flaggedForReview ? "OUI" : "Non");
    }
}
