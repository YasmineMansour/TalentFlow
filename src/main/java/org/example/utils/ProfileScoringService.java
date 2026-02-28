package org.example.utils;

import org.example.model.ProfileScore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔍 Service de Scoring Automatique des Profils (IA Prédictive)
 *
 * Compare les compétences d'un candidat avec les exigences d'un poste
 * en utilisant la similarité de Jaccard et la distance de Levenshtein.
 *
 * Fonctionnalités :
 * - Scoring par similarité de Jaccard (intersection / union des compétences)
 * - Correspondance floue via Levenshtein (ex: "Javascript" ≈ "JavaScript")
 * - Pondération des compétences (certaines compétences valent plus que d'autres)
 * - Attribution automatique de badges (Profil Recommandé, Bon Profil, etc.)
 */
public class ProfileScoringService {

    // Seuil de similarité Levenshtein pour considérer deux compétences comme "proches"
    private static final double LEVENSHTEIN_THRESHOLD = 0.75;

    // ===========================
    //   COMPÉTENCES PRÉDÉFINIES
    // ===========================

    /** Catégories de compétences avec leurs synonymes pour le matching intelligent */
    private static final Map<String, List<String>> SKILL_SYNONYMS = new HashMap<>();

    static {
        SKILL_SYNONYMS.put("java", List.of("java", "java se", "java ee", "jdk", "jvm"));
        SKILL_SYNONYMS.put("javascript", List.of("javascript", "js", "ecmascript", "es6", "es2015"));
        SKILL_SYNONYMS.put("python", List.of("python", "python3", "py"));
        SKILL_SYNONYMS.put("sql", List.of("sql", "mysql", "postgresql", "plsql", "tsql", "sqlite"));
        SKILL_SYNONYMS.put("html", List.of("html", "html5", "xhtml"));
        SKILL_SYNONYMS.put("css", List.of("css", "css3", "scss", "sass", "less"));
        SKILL_SYNONYMS.put("react", List.of("react", "reactjs", "react.js", "react native"));
        SKILL_SYNONYMS.put("angular", List.of("angular", "angularjs", "angular.js"));
        SKILL_SYNONYMS.put("spring", List.of("spring", "spring boot", "springboot", "spring mvc"));
        SKILL_SYNONYMS.put("git", List.of("git", "github", "gitlab", "bitbucket", "svn"));
        SKILL_SYNONYMS.put("docker", List.of("docker", "containerisation", "conteneur"));
        SKILL_SYNONYMS.put("communication", List.of("communication", "présentation", "prise de parole"));
        SKILL_SYNONYMS.put("leadership", List.of("leadership", "management", "gestion d'équipe", "chef d'équipe"));
        SKILL_SYNONYMS.put("teamwork", List.of("travail en équipe", "teamwork", "collaboration", "esprit d'équipe"));
        SKILL_SYNONYMS.put("agile", List.of("agile", "scrum", "kanban", "méthode agile", "sprint"));
        SKILL_SYNONYMS.put("machine learning", List.of("machine learning", "ml", "deep learning", "ia", "intelligence artificielle"));
        SKILL_SYNONYMS.put("devops", List.of("devops", "ci/cd", "jenkins", "pipeline", "intégration continue"));
        SKILL_SYNONYMS.put("cloud", List.of("cloud", "aws", "azure", "gcp", "google cloud", "amazon web services"));
        SKILL_SYNONYMS.put("php", List.of("php", "laravel", "symfony"));
        SKILL_SYNONYMS.put("csharp", List.of("c#", "csharp", ".net", "dotnet", "asp.net"));
    }

    // ===================================================================
    //   MÉTHODE PRINCIPALE : Scoring d'un profil candidat vs un poste
    // ===================================================================

    /**
     * Calcule le score de pertinence d'un candidat pour un poste donné.
     *
     * @param candidateSkills liste des compétences du candidat (ex: ["Java", "SQL", "Git"])
     * @param requiredSkills  liste des compétences requises par le poste (ex: ["Java", "Spring", "SQL"])
     * @return ProfileScore contenant le score, les matchs, les manques et le badge
     */
    public static ProfileScore scoreProfile(List<String> candidateSkills, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return new ProfileScore(0.0, List.of(), List.of(), Map.of());
        }
        if (candidateSkills == null || candidateSkills.isEmpty()) {
            return new ProfileScore(0.0, List.of(), new ArrayList<>(requiredSkills), Map.of());
        }

        // Normaliser les compétences
        Set<String> normalizedCandidate = candidateSkills.stream()
                .map(ProfileScoringService::normalize)
                .collect(Collectors.toSet());

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        Map<String, Double> skillScores = new LinkedHashMap<>();
        double totalScore = 0.0;

        for (String required : requiredSkills) {
            String normalizedRequired = normalize(required);
            double bestMatch = findBestMatch(normalizedRequired, normalizedCandidate);
            skillScores.put(required, bestMatch);

            if (bestMatch >= LEVENSHTEIN_THRESHOLD) {
                matchedSkills.add(required);
                totalScore += bestMatch;
            } else {
                missingSkills.add(required);
                totalScore += bestMatch * 0.3; // Crédit partiel pour correspondance faible
            }
        }

        // Score final normalisé
        double finalScore = totalScore / requiredSkills.size();
        finalScore = Math.min(1.0, Math.max(0.0, finalScore));

        System.out.println("🔍 Scoring IA — Score: " + String.format("%.1f%%", finalScore * 100)
                + " | Matchs: " + matchedSkills.size() + "/" + requiredSkills.size());

        return new ProfileScore(finalScore, matchedSkills, missingSkills, skillScores);
    }

    /**
     * Scoring avec pondération des compétences.
     * Les compétences clés ont un poids plus élevé.
     *
     * @param candidateSkills liste des compétences du candidat
     * @param weightedSkills  map (compétence → poids) ex: {"Java": 3.0, "SQL": 2.0, "Git": 1.0}
     * @return ProfileScore
     */
    public static ProfileScore scoreProfileWeighted(List<String> candidateSkills,
                                                     Map<String, Double> weightedSkills) {
        if (weightedSkills == null || weightedSkills.isEmpty()) {
            return new ProfileScore(0.0, List.of(), List.of(), Map.of());
        }
        if (candidateSkills == null || candidateSkills.isEmpty()) {
            return new ProfileScore(0.0, List.of(), new ArrayList<>(weightedSkills.keySet()), Map.of());
        }

        Set<String> normalizedCandidate = candidateSkills.stream()
                .map(ProfileScoringService::normalize)
                .collect(Collectors.toSet());

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        Map<String, Double> skillScores = new LinkedHashMap<>();
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : weightedSkills.entrySet()) {
            String required = entry.getKey();
            double weight = entry.getValue();
            String normalizedRequired = normalize(required);

            double bestMatch = findBestMatch(normalizedRequired, normalizedCandidate);
            skillScores.put(required, bestMatch);
            totalWeight += weight;

            if (bestMatch >= LEVENSHTEIN_THRESHOLD) {
                matchedSkills.add(required);
                totalWeightedScore += bestMatch * weight;
            } else {
                missingSkills.add(required);
                totalWeightedScore += bestMatch * weight * 0.3;
            }
        }

        double finalScore = (totalWeight > 0) ? totalWeightedScore / totalWeight : 0.0;
        finalScore = Math.min(1.0, Math.max(0.0, finalScore));

        System.out.println("🔍 Scoring IA (pondéré) — Score: " + String.format("%.1f%%", finalScore * 100)
                + " | Matchs: " + matchedSkills.size() + "/" + weightedSkills.size());

        return new ProfileScore(finalScore, matchedSkills, missingSkills, skillScores);
    }

    // ===========================
    //   ALGORITHMES DE SIMILARITÉ
    // ===========================

    /**
     * Trouve la meilleure correspondance pour une compétence parmi un ensemble.
     * Combine la recherche par synonymes et la similarité Levenshtein.
     */
    private static double findBestMatch(String required, Set<String> candidateSkills) {
        // 1. Correspondance exacte
        if (candidateSkills.contains(required)) {
            return 1.0;
        }

        double bestScore = 0.0;

        // 2. Correspondance par synonymes
        String requiredGroup = findSynonymGroup(required);
        for (String candidate : candidateSkills) {
            String candidateGroup = findSynonymGroup(candidate);

            if (requiredGroup != null && requiredGroup.equals(candidateGroup)) {
                return 0.95; // Très bonne correspondance via synonymes
            }

            // 3. Similarité de Levenshtein normalisée
            double similarity = levenshteinSimilarity(required, candidate);
            bestScore = Math.max(bestScore, similarity);
        }

        // 4. Similarité de Jaccard sur les mots (pour les compétences multi-mots)
        for (String candidate : candidateSkills) {
            double jaccardSim = jaccardSimilarity(required, candidate);
            bestScore = Math.max(bestScore, jaccardSim);
        }

        return bestScore;
    }

    /**
     * Calcul de la similarité de Jaccard entre deux chaînes.
     * J(A, B) = |A ∩ B| / |A ∪ B|
     */
    public static double jaccardSimilarity(String s1, String s2) {
        Set<String> set1 = tokenize(s1);
        Set<String> set2 = tokenize(s2);

        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Calcul de la similarité basée sur la distance de Levenshtein.
     * Retourne une valeur entre 0.0 (totalement différent) et 1.0 (identique).
     */
    public static double levenshteinSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Calcul de la distance de Levenshtein (nombre minimum d'éditions).
     * Programmation dynamique classique.
     */
    public static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[len1][len2];
    }

    // ===========================
    //   UTILITAIRES
    // ===========================

    /** Normalise une compétence (minuscule, trim) */
    private static String normalize(String skill) {
        return skill == null ? "" : skill.toLowerCase().trim();
    }

    /** Tokenise une chaîne en un ensemble de mots */
    private static Set<String> tokenize(String s) {
        if (s == null || s.isBlank()) return Set.of();
        return Arrays.stream(s.toLowerCase().split("[\\s,;/\\-_]+"))
                .filter(w -> !w.isBlank())
                .collect(Collectors.toSet());
    }

    /** Trouve le groupe de synonymes d'une compétence */
    private static String findSynonymGroup(String skill) {
        String normalized = normalize(skill);
        for (Map.Entry<String, List<String>> entry : SKILL_SYNONYMS.entrySet()) {
            if (entry.getValue().contains(normalized)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ===========================
    //   POSTES PRÉDÉFINIS
    // ===========================

    /** Retourne les compétences requises pour des postes courants */
    public static Map<String, List<String>> getPredefinedJobs() {
        Map<String, List<String>> jobs = new LinkedHashMap<>();
        jobs.put("Développeur Java Full-Stack", List.of("Java", "Spring", "SQL", "HTML", "CSS", "JavaScript", "Git", "Docker"));
        jobs.put("Développeur Front-End", List.of("JavaScript", "React", "HTML", "CSS", "Git", "Agile"));
        jobs.put("Data Scientist", List.of("Python", "Machine Learning", "SQL", "Communication", "Git"));
        jobs.put("DevOps Engineer", List.of("Docker", "Cloud", "Git", "DevOps", "Python", "Agile"));
        jobs.put("Chef de Projet IT", List.of("Agile", "Leadership", "Communication", "Teamwork", "Git"));
        jobs.put("Développeur PHP", List.of("PHP", "SQL", "HTML", "CSS", "JavaScript", "Git"));
        jobs.put("Développeur .NET", List.of("C#", "SQL", "HTML", "CSS", "JavaScript", "Git", "Docker"));
        return jobs;
    }

    /**
     * Score un candidat contre tous les postes prédéfinis et retourne le meilleur match.
     *
     * @param candidateSkills compétences du candidat
     * @return Map.Entry avec le nom du poste et le ProfileScore
     */
    public static Map.Entry<String, ProfileScore> findBestJob(List<String> candidateSkills) {
        Map<String, List<String>> jobs = getPredefinedJobs();
        String bestJob = null;
        ProfileScore bestScore = null;

        for (Map.Entry<String, List<String>> job : jobs.entrySet()) {
            ProfileScore score = scoreProfile(candidateSkills, job.getValue());
            if (bestScore == null || score.getScore() > bestScore.getScore()) {
                bestJob = job.getKey();
                bestScore = score;
            }
        }

        return Map.entry(bestJob != null ? bestJob : "Aucun", bestScore != null ? bestScore :
                new ProfileScore(0.0, List.of(), List.of(), Map.of()));
    }

    /**
     * Score un candidat contre tous les postes prédéfinis.
     *
     * @param candidateSkills compétences du candidat
     * @return Map triée par score décroissant (poste → ProfileScore)
     */
    public static LinkedHashMap<String, ProfileScore> scoreAllJobs(List<String> candidateSkills) {
        Map<String, List<String>> jobs = getPredefinedJobs();
        Map<String, ProfileScore> results = new HashMap<>();

        for (Map.Entry<String, List<String>> job : jobs.entrySet()) {
            results.put(job.getKey(), scoreProfile(candidateSkills, job.getValue()));
        }

        // Trier par score décroissant
        return results.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue().getScore(), a.getValue().getScore()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}
