package org.example.GUI; // Utilise GUI en majuscules

import org.example.model.User;

public class UserSession {
    private static User instance;
    private static User pendingUser;   // Utilisateur en attente de vérification 2FA
    private static String pendingEmail; // Email en attente (pour réinitialisation mot de passe)

    // --- Session active ---
    public static void setInstance(User user) { instance = user; }
    public static User getInstance() { return instance; }

    // --- Utilisateur en attente de 2FA ---
    public static void setPendingUser(User user) { pendingUser = user; }
    public static User getPendingUser() { return pendingUser; }
    public static void clearPendingUser() { pendingUser = null; }

    // --- Email en attente de réinitialisation ---
    public static void setPendingEmail(String email) { pendingEmail = email; }
    public static String getPendingEmail() { return pendingEmail; }
    public static void clearPendingEmail() { pendingEmail = null; }

    // --- Nettoyage complet ---
    public static void cleanUserSession() {
        instance = null;
        pendingUser = null;
        pendingEmail = null;
    }
}