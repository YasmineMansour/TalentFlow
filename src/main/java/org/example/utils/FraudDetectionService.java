package org.example.utils;

import org.example.model.FraudCheckResult;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 🛡️ Service de Détection de Fraude et Profilage (IA de Classification)
 *
 * Analyse automatiquement les données d'inscription pour détecter :
 * - Emails suspects (jetables, aléatoires, patterns frauduleux)
 * - Numéros de téléphone suspects
 * - Noms suspects (caractères aléatoires)
 * - Comportements d'inscription anormaux
 *
 * Utilise un système de classification pondérée avec scoring de risque.
 * Les utilisateurs à risque élevé sont flaggés pour revue manuelle par un admin.
 */
public class FraudDetectionService {

    // ===========================
    //   DOMAINES EMAIL JETABLES
    // ===========================

    /** Liste des domaines d'emails temporaires / jetables connus */
    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            // Services d'email jetable populaires
            "tempmail.com", "temp-mail.org", "guerrillamail.com", "guerrillamail.net",
            "throwaway.email", "mailinator.com", "yopmail.com", "yopmail.fr",
            "sharklasers.com", "guerrillamailblock.com", "grr.la", "dispostable.com",
            "trashmail.com", "trashmail.net", "trashmail.me", "mailnesia.com",
            "maildrop.cc", "discard.email", "discardmail.com", "getairmail.com",
            "fakeinbox.com", "tempail.com", "tempr.email", "temp-mail.de",
            "10minutemail.com", "20minutemail.com", "minutemail.com",
            "emailondeck.com", "emailfake.com", "emkei.cz", "crazymailing.com",
            "mailcatch.com", "mailexpire.com", "mailmoat.com", "mailnator.com",
            "mohmal.com", "mytemp.email", "nada.email", "throwam.com",
            "trashmail.org", "trashmail.io", "wegwerfmail.de", "wegwerfmail.net",
            "getnada.com", "burnermail.io", "inboxbear.com", "spamgourmet.com",

            // Domaines suspects courants
            "example.com", "test.com", "fake.com", "nomail.com", "noemail.com",
            "nobody.com", "notreal.com", "spam.com"
    );

    // ===========================
    //   PATTERNS SUSPECTS
    // ===========================

    /** Pattern : email avec beaucoup de chiffres aléatoires (ex: abc123456@...) */
    private static final Pattern RANDOM_DIGITS_PATTERN = Pattern.compile(".*\\d{5,}.*");

    /** Pattern : chaîne de consonnes sans voyelle (indicateur d'aléatoire) */
    private static final Pattern RANDOM_CONSONANTS_PATTERN = Pattern.compile("[^aeiouAEIOUàâéèêëîïôùû]{5,}");

    /** Pattern : alternance chaotique majuscule/minuscule (ex: aBcDeFgH) */
    private static final Pattern MIXED_CASE_PATTERN = Pattern.compile("(?:[A-Z][a-z]){4,}|(?:[a-z][A-Z]){4,}");

    /** Pattern : nom très court (1-2 caractères seulement) */
    private static final Pattern TOO_SHORT_NAME = Pattern.compile("^.{1,2}$");

    /** Pattern : nom contenant des chiffres */
    private static final Pattern NAME_WITH_DIGITS = Pattern.compile(".*\\d+.*");

    // ===========================
    //   POIDS DES SIGNAUX
    // ===========================

    private static final double WEIGHT_DISPOSABLE_EMAIL = 0.40;     // Email jetable
    private static final double WEIGHT_RANDOM_EMAIL = 0.25;          // Email aléatoire
    private static final double WEIGHT_SUSPICIOUS_DOMAIN = 0.15;     // Domaine suspect
    private static final double WEIGHT_SHORT_NAME = 0.10;            // Nom trop court
    private static final double WEIGHT_DIGIT_NAME = 0.10;            // Chiffres dans le nom
    private static final double WEIGHT_RANDOM_NAME = 0.20;           // Nom aléatoire
    private static final double WEIGHT_PHONE_SUSPECT = 0.15;         // Téléphone suspect
    private static final double WEIGHT_REPEATED_CHARS = 0.10;        // Caractères répétés

    // ===================================================================
    //   MÉTHODE PRINCIPALE : Analyse complète d'un profil utilisateur
    // ===================================================================

    /**
     * Analyse les données d'inscription pour détecter les fraudes potentielles.
     *
     * @param email     adresse email de l'utilisateur
     * @param nom       nom de famille
     * @param prenom    prénom
     * @param telephone numéro de téléphone
     * @return FraudCheckResult avec le score de risque, le niveau et les alertes
     */
    public static FraudCheckResult analyzeUser(String email, String nom, String prenom, String telephone) {
        List<String> flags = new ArrayList<>();
        double riskScore = 0.0;

        // ===== 1. ANALYSE DE L'EMAIL =====
        riskScore += analyzeEmail(email, flags);

        // ===== 2. ANALYSE DU NOM / PRÉNOM =====
        riskScore += analyzeName(nom, "Nom", flags);
        riskScore += analyzeName(prenom, "Prénom", flags);

        // ===== 3. ANALYSE DU TÉLÉPHONE =====
        riskScore += analyzePhone(telephone, flags);

        // ===== 4. ANALYSE CROISÉE =====
        riskScore += crossAnalysis(email, nom, prenom, flags);

        // Normaliser le score entre 0 et 1
        riskScore = Math.min(1.0, Math.max(0.0, riskScore));

        FraudCheckResult result = new FraudCheckResult(riskScore, flags);

        System.out.println("🛡️ Analyse fraude — " + result.getRiskLevel().getLabel()
                + " | Score: " + String.format("%.1f%%", result.getRiskPercentage())
                + " | Alertes: " + flags.size()
                + (result.isFlaggedForReview() ? " | ⚠️ REVUE REQUISE" : ""));

        return result;
    }

    /**
     * Analyse rapide d'un email uniquement (utile lors de la saisie en temps réel).
     *
     * @param email adresse email à vérifier
     * @return FraudCheckResult
     */
    public static FraudCheckResult analyzeEmailOnly(String email) {
        List<String> flags = new ArrayList<>();
        double riskScore = analyzeEmail(email, flags);
        riskScore = Math.min(1.0, Math.max(0.0, riskScore));
        return new FraudCheckResult(riskScore, flags);
    }

    // ===========================
    //   ANALYSES DÉTAILLÉES
    // ===========================

    /**
     * Analyse une adresse email pour détecter les signaux suspects.
     */
    private static double analyzeEmail(String email, List<String> flags) {
        if (email == null || email.isBlank()) {
            flags.add("📧 Email manquant ou vide");
            return WEIGHT_DISPOSABLE_EMAIL;
        }

        double score = 0.0;
        String lower = email.toLowerCase().trim();
        String[] parts = lower.split("@");

        if (parts.length != 2) {
            flags.add("📧 Format email invalide");
            return WEIGHT_DISPOSABLE_EMAIL;
        }

        String localPart = parts[0];
        String domain = parts[1];

        // 1. Vérifier si le domaine est jetable
        if (DISPOSABLE_DOMAINS.contains(domain)) {
            flags.add("📧 Email jetable détecté (domaine: " + domain + ")");
            score += WEIGHT_DISPOSABLE_EMAIL;
        }

        // 2. Vérifier si le local part contient trop de chiffres aléatoires
        if (RANDOM_DIGITS_PATTERN.matcher(localPart).matches()) {
            flags.add("📧 Email avec séquence de chiffres suspecte");
            score += WEIGHT_RANDOM_EMAIL;
        }

        // 3. Vérifier si le local part semble aléatoire (consonnes sans voyelles)
        String localWithoutDots = localPart.replace(".", "").replace("_", "").replace("-", "");
        if (localWithoutDots.length() > 4 && RANDOM_CONSONANTS_PATTERN.matcher(localWithoutDots).find()) {
            flags.add("📧 Email semble généré aléatoirement");
            score += WEIGHT_RANDOM_EMAIL;
        }

        // 4. Vérifier le ratio voyelles/consonnes (indicateur d'aléatoire)
        double vowelRatio = getVowelRatio(localWithoutDots);
        if (localWithoutDots.length() > 5 && (vowelRatio < 0.15 || vowelRatio > 0.80)) {
            flags.add("📧 Distribution de lettres anormale dans l'email (ratio voyelles: "
                    + String.format("%.0f%%", vowelRatio * 100) + ")");
            score += WEIGHT_RANDOM_EMAIL * 0.5;
        }

        // 5. Vérifier la longueur du local part
        if (localWithoutDots.length() <= 2) {
            flags.add("📧 Partie locale de l'email trop courte");
            score += WEIGHT_SUSPICIOUS_DOMAIN * 0.5;
        }

        // 6. Vérifier les TLD suspects
        if (domain.endsWith(".xyz") || domain.endsWith(".top") || domain.endsWith(".tk")
                || domain.endsWith(".ml") || domain.endsWith(".ga") || domain.endsWith(".cf")
                || domain.endsWith(".gq") || domain.endsWith(".buzz") || domain.endsWith(".click")) {
            flags.add("📧 Extension de domaine suspecte (" + domain.substring(domain.lastIndexOf('.')) + ")");
            score += WEIGHT_SUSPICIOUS_DOMAIN;
        }

        // 7. Vérifier les motifs d'email de test
        if (localPart.contains("test") || localPart.contains("admin") || localPart.contains("user")
                || localPart.contains("demo") || localPart.contains("sample")
                || localPart.startsWith("aaa") || localPart.startsWith("xxx")) {
            flags.add("📧 Email semble être un compte de test");
            score += WEIGHT_SUSPICIOUS_DOMAIN * 0.7;
        }

        // 8. Entropie de Shannon (mesure du désordre/aléatoire)
        double entropy = calculateEntropy(localWithoutDots);
        if (localWithoutDots.length() > 6 && entropy > 3.5) {
            flags.add("📧 Entropie élevée dans l'email (valeur: " + String.format("%.2f", entropy)
                    + ") — possible génération automatique");
            score += WEIGHT_RANDOM_EMAIL * 0.6;
        }

        return score;
    }

    /**
     * Analyse un nom ou prénom pour détecter les anomalies.
     */
    private static double analyzeName(String name, String fieldLabel, List<String> flags) {
        if (name == null || name.isBlank()) {
            flags.add("👤 " + fieldLabel + " manquant ou vide");
            return WEIGHT_SHORT_NAME;
        }

        double score = 0.0;
        String trimmed = name.trim();

        // 1. Nom trop court
        if (TOO_SHORT_NAME.matcher(trimmed).matches()) {
            flags.add("👤 " + fieldLabel + " trop court (" + trimmed.length() + " caractères)");
            score += WEIGHT_SHORT_NAME;
        }

        // 2. Chiffres dans le nom
        if (NAME_WITH_DIGITS.matcher(trimmed).matches()) {
            flags.add("👤 " + fieldLabel + " contient des chiffres");
            score += WEIGHT_DIGIT_NAME;
        }

        // 3. Noms aléatoires (consonnes sans voyelles)
        String cleaned = trimmed.replaceAll("[\\s\\-']", "");
        if (cleaned.length() > 3 && RANDOM_CONSONANTS_PATTERN.matcher(cleaned.toLowerCase()).find()) {
            flags.add("👤 " + fieldLabel + " semble aléatoire (suite de consonnes)");
            score += WEIGHT_RANDOM_NAME;
        }

        // 4. Caractères répétés (ex: "aaaa", "bbbb")
        if (hasRepeatedChars(cleaned, 3)) {
            flags.add("👤 " + fieldLabel + " contient des caractères répétés de façon suspecte");
            score += WEIGHT_REPEATED_CHARS;
        }

        // 5. Ratio voyelles/consonnes anormal
        double vowelRatio = getVowelRatio(cleaned.toLowerCase());
        if (cleaned.length() > 3 && (vowelRatio < 0.15 || vowelRatio > 0.85)) {
            flags.add("👤 " + fieldLabel + " a une distribution de lettres inhabituelle");
            score += WEIGHT_RANDOM_NAME * 0.4;
        }

        return score;
    }

    /**
     * Analyse un numéro de téléphone pour détecter les anomalies.
     */
    private static double analyzePhone(String phone, List<String> flags) {
        if (phone == null || phone.isBlank()) {
            return 0.0; // Pas de pénalité si pas de téléphone
        }

        double score = 0.0;
        String cleaned = phone.replaceAll("[\\s\\-+()]", "");

        // 1. Numéro avec tous les mêmes chiffres (ex: 11111111)
        if (cleaned.length() >= 8 && cleaned.chars().distinct().count() <= 2) {
            flags.add("📱 Numéro de téléphone suspect (trop peu de chiffres distincts)");
            score += WEIGHT_PHONE_SUSPECT;
        }

        // 2. Numéro qui est une suite (ex: 12345678)
        if (isSequentialNumber(cleaned)) {
            flags.add("📱 Numéro de téléphone séquentiel suspect");
            score += WEIGHT_PHONE_SUSPECT;
        }

        // 3. Chiffres répétés
        if (hasRepeatedChars(cleaned, 4)) {
            flags.add("📱 Numéro avec trop de chiffres répétés");
            score += WEIGHT_PHONE_SUSPECT * 0.5;
        }

        return score;
    }

    /**
     * Analyse croisée entre les différents champs pour détecter les incohérences.
     */
    private static double crossAnalysis(String email, String nom, String prenom, List<String> flags) {
        if (email == null || nom == null || prenom == null) return 0.0;

        double score = 0.0;
        String localPart = email.split("@")[0].toLowerCase();
        String nomLower = nom.toLowerCase().trim();
        String prenomLower = prenom.toLowerCase().trim();

        // Vérifier si l'email ne contient ni le nom ni le prénom
        // (les emails professionnels contiennent souvent prenom.nom ou une variante)
        boolean containsName = localPart.contains(nomLower) || localPart.contains(prenomLower);
        boolean containsInitials = localPart.contains(String.valueOf(prenomLower.charAt(0)))
                && localPart.contains(String.valueOf(nomLower.charAt(0)));

        // Ce n'est pas un signal fort : beaucoup de gens ont des emails sans leur nom
        // Mais combiné avec d'autres signaux, cela renforce le score
        if (!containsName && !containsInitials && localPart.length() > 10) {
            // Signal faible uniquement
            flags.add("🔗 L'email ne semble pas correspondre au nom/prénom");
            score += 0.05;
        }

        // Vérifier si nom == prénom (copier-coller suspect)
        if (nomLower.equals(prenomLower)) {
            flags.add("🔗 Le nom et le prénom sont identiques — possible saisie erronée");
            score += 0.10;
        }

        return score;
    }

    // ===========================
    //   UTILITAIRES MATHÉMATIQUES
    // ===========================

    /**
     * Calcule l'entropie de Shannon d'une chaîne.
     * Plus l'entropie est élevée, plus la chaîne est "aléatoire".
     */
    private static double calculateEntropy(String str) {
        if (str == null || str.isEmpty()) return 0.0;

        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : str.toCharArray()) {
            freqMap.merge(c, 1, Integer::sum);
        }

        double entropy = 0.0;
        int length = str.length();
        for (int freq : freqMap.values()) {
            double probability = (double) freq / length;
            if (probability > 0) {
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        return entropy;
    }

    /** Calcule le ratio de voyelles dans une chaîne */
    private static double getVowelRatio(String str) {
        if (str == null || str.isEmpty()) return 0.5;
        long vowels = str.chars()
                .filter(c -> "aeiouàâéèêëîïôùû".indexOf(c) >= 0)
                .count();
        return (double) vowels / str.length();
    }

    /** Vérifie si la chaîne contient des caractères répétés consécutivement */
    private static boolean hasRepeatedChars(String str, int threshold) {
        if (str == null || str.length() < threshold) return false;
        int count = 1;
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) == str.charAt(i - 1)) {
                count++;
                if (count >= threshold) return true;
            } else {
                count = 1;
            }
        }
        return false;
    }

    /** Vérifie si un numéro est une séquence croissante ou décroissante */
    private static boolean isSequentialNumber(String digits) {
        if (digits.length() < 6) return false;
        boolean ascending = true, descending = true;
        for (int i = 1; i < digits.length(); i++) {
            if (digits.charAt(i) - digits.charAt(i - 1) != 1) ascending = false;
            if (digits.charAt(i) - digits.charAt(i - 1) != -1) descending = false;
        }
        return ascending || descending;
    }

    // ===========================
    //   VÉRIFICATION EN TEMPS RÉEL
    // ===========================

    /**
     * Vérification rapide d'email en temps réel (pendant la saisie).
     * Retourne un message d'avertissement ou null si OK.
     *
     * @param email email à vérifier
     * @return message d'avertissement ou null
     */
    public static String quickEmailCheck(String email) {
        if (email == null || !email.contains("@")) return null;

        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        if (DISPOSABLE_DOMAINS.contains(domain)) {
            return "⚠️ Les adresses email temporaires ne sont pas acceptées.";
        }

        String localPart = email.substring(0, email.indexOf("@"));
        if (localPart.length() > 4) {
            String cleaned = localPart.replaceAll("[._\\-]", "");
            if (RANDOM_CONSONANTS_PATTERN.matcher(cleaned.toLowerCase()).find()) {
                return "⚠️ Cette adresse email semble suspecte.";
            }
        }

        return null; // Pas de problème détecté
    }

    /**
     * Vérifie si un domaine est dans la liste des emails jetables.
     *
     * @param email adresse email
     * @return true si le domaine est jetable
     */
    public static boolean isDisposableEmail(String email) {
        if (email == null || !email.contains("@")) return false;
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return DISPOSABLE_DOMAINS.contains(domain);
    }
}
