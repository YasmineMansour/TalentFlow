package org.example.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    // Nom/Prénom : Uniquement lettres (accents autorisés), espaces, tirets, min 2 caractères
    private static final String NAME_REGEX = "^[a-zA-ZÀ-ÿ\\s\\-]{2,30}$";
    // Email : format standard plus complet
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";
    // Téléphone : Exactement 8 chiffres
    private static final String TEL_REGEX = "^[0-9]{8}$";
    // Password : Min 8 caractères, 1 Majuscule, 1 Chiffre, 1 Symbole
    private static final String PWD_REGEX = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!*])(?=\\S+$).{8,}$";

    public static boolean isInvalidName(String name) {
        return name == null || !Pattern.matches(NAME_REGEX, name.trim());
    }

    public static boolean isInvalidEmail(String email) {
        return email == null || !Pattern.matches(EMAIL_REGEX, email.trim());
    }

    public static boolean isInvalidTel(String tel) {
        return tel == null || !Pattern.matches(TEL_REGEX, tel.trim());
    }

    public static boolean isInvalidPassword(String pwd) {
        return pwd == null || !Pattern.matches(PWD_REGEX, pwd);
    }

    /** Retourne un message décrivant la force du mot de passe, ou null si valide */
    public static String getPasswordWeakness(String pwd) {
        if (pwd == null || pwd.isEmpty()) return "Le mot de passe est vide.";
        if (pwd.length() < 8) return "Min 8 caractères requis.";
        if (!pwd.matches(".*[A-Z].*")) return "Au moins 1 lettre majuscule requise.";
        if (!pwd.matches(".*[0-9].*")) return "Au moins 1 chiffre requis.";
        if (!pwd.matches(".*[@#$%^&+=!*].*")) return "Au moins 1 symbole (@#$%^&+=!*) requis.";
        if (pwd.contains(" ")) return "Les espaces ne sont pas autorisés.";
        return null; // Mot de passe valide
    }
}