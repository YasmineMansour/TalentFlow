package org.example.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    // Nom/Prénom : Uniquement lettres (accents autorisés), min 2 caractères
    private static final String NAME_REGEX = "^[a-zA-ZÀ-ÿ\\s]{2,20}$";
    // Email standard
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    // Téléphone : Exactement 8 chiffres (selon ton code précédent)
    private static final String TEL_REGEX = "^[0-9]{8}$";
    // Password : Min 8 caractères, 1 Majuscule, 1 Chiffre, 1 Symbole
    private static final String PWD_REGEX = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!*])(?=\\S+$).{8,}$";

    public static boolean isInvalidName(String name) { return !Pattern.matches(NAME_REGEX, name); }
    public static boolean isInvalidEmail(String email) { return !Pattern.matches(EMAIL_REGEX, email); }
    public static boolean isInvalidTel(String tel) { return !Pattern.matches(TEL_REGEX, tel); }
    public static boolean isInvalidPassword(String pwd) { return !Pattern.matches(PWD_REGEX, pwd); }
}