package org.example.model;

import java.util.List;
import java.util.Map;

/**
 * Résultat du scoring automatique d'un profil candidat.
 * Contient le score de pertinence, les compétences matchées, et un badge éventuel.
 */
public class ProfileScore {

    public enum Badge {
        PROFIL_RECOMMANDE("🌟 Profil Recommandé", "#00b894"),
        BON_PROFIL("👍 Bon Profil", "#0984e3"),
        PROFIL_MOYEN("📋 Profil Moyen", "#fdcb6e"),
        PROFIL_FAIBLE("⚠️ Profil Faible", "#d63031");

        private final String label;
        private final String color;

        Badge(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
    }

    private final double score;                    // Score de 0.0 à 1.0
    private final double scorePercentage;           // Score en pourcentage (0-100)
    private final Badge badge;
    private final List<String> matchedSkills;       // Compétences correspondantes
    private final List<String> missingSkills;       // Compétences manquantes
    private final Map<String, Double> skillScores;  // Score détaillé par compétence (similarité)

    public ProfileScore(double score, List<String> matchedSkills,
                        List<String> missingSkills, Map<String, Double> skillScores) {
        this.score = score;
        this.scorePercentage = Math.round(score * 100.0 * 10.0) / 10.0;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.skillScores = skillScores;

        // Attribution du badge en fonction du score
        if (score >= 0.75) {
            this.badge = Badge.PROFIL_RECOMMANDE;
        } else if (score >= 0.50) {
            this.badge = Badge.BON_PROFIL;
        } else if (score >= 0.30) {
            this.badge = Badge.PROFIL_MOYEN;
        } else {
            this.badge = Badge.PROFIL_FAIBLE;
        }
    }

    // GETTERS
    public double getScore() { return score; }
    public double getScorePercentage() { return scorePercentage; }
    public Badge getBadge() { return badge; }
    public List<String> getMatchedSkills() { return matchedSkills; }
    public List<String> getMissingSkills() { return missingSkills; }
    public Map<String, Double> getSkillScores() { return skillScores; }

    /** Vérifie si le profil est recommandé */
    public boolean isRecommended() {
        return badge == Badge.PROFIL_RECOMMANDE;
    }

    @Override
    public String toString() {
        return String.format("%s | Score: %.1f%% | Match: %d/%d compétences",
                badge.getLabel(), scorePercentage,
                matchedSkills.size(), matchedSkills.size() + missingSkills.size());
    }
}
