package org.example.utils;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion des codes de vérification (2FA + réinitialisation mot de passe).
 * Les codes sont stockés en mémoire avec une durée de validité de 5 minutes.
 */
public class VerificationService {

    // ===== CONFIGURATION 2FA =====
    // Mettre à false pour désactiver l'authentification à deux facteurs
    public static final boolean TWO_FA_ENABLED = true;

    private static final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();
    private static final long CODE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    /** Entrée de code avec expiration */
    private static class CodeEntry {
        final String code;
        final long expiresAt;

        CodeEntry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Génère un code à 6 chiffres pour un email donné.
     * @param email l'adresse email associée
     * @return le code généré
     */
    public static String generateCode(String email) {
        String code = String.format("%06d", random.nextInt(1000000));
        codes.put(email.toLowerCase().trim(), new CodeEntry(code, System.currentTimeMillis() + CODE_VALIDITY_MS));
        System.out.println("🔑 Code généré pour " + email + " : " + code);
        return code;
    }

    /**
     * Vérifie un code pour un email donné.
     * Le code est supprimé après vérification réussie.
     * @return true si le code est valide et non expiré
     */
    public static boolean verifyCode(String email, String code) {
        if (email == null || code == null) return false;

        CodeEntry entry = codes.get(email.toLowerCase().trim());
        if (entry == null) {
            System.err.println("❌ Aucun code trouvé pour : " + email);
            return false;
        }

        // Vérifier l'expiration
        if (System.currentTimeMillis() > entry.expiresAt) {
            codes.remove(email.toLowerCase().trim());
            System.err.println("❌ Code expiré pour : " + email);
            return false;
        }

        // Vérifier le code
        if (entry.code.equals(code.trim())) {
            codes.remove(email.toLowerCase().trim());
            System.out.println("✅ Code vérifié avec succès pour : " + email);
            return true;
        }

        System.err.println("❌ Code incorrect pour : " + email);
        return false;
    }

    /** Supprime le code associé à un email */
    public static void clearCode(String email) {
        if (email != null) {
            codes.remove(email.toLowerCase().trim());
        }
    }

    /** Vérifie si un code existe encore pour un email */
    public static boolean hasActiveCode(String email) {
        if (email == null) return false;
        CodeEntry entry = codes.get(email.toLowerCase().trim());
        return entry != null && System.currentTimeMillis() <= entry.expiresAt;
    }
}
