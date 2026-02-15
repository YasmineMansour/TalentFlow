package org.example.GUI; // Utilise GUI en majuscules

import org.example.model.User;

public class UserSession {
    private static User instance;
    public static void setInstance(User user) { instance = user; }
    public static User getInstance() { return instance; }
    public static void cleanUserSession() { instance = null; }
}