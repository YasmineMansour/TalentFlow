package org.example.utils;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Gestionnaire de langue (i18n) pour TalentFlow.
 * Supporte français (fr) et anglais (en).
 * Persiste le choix via java.util.prefs.Preferences.
 */
public class LanguageManager {

    private static final Preferences PREFS = Preferences.userNodeForPackage(LanguageManager.class);
    private static final String KEY_LANG = "language";

    private static Locale currentLocale;
    private static ResourceBundle bundle;

    /** Listeners notifiés lors d'un changement de langue */
    private static final List<Runnable> listeners = new ArrayList<>();

    static {
        String lang = PREFS.get(KEY_LANG, "fr");
        currentLocale = new Locale(lang);
        loadBundle();
    }

    private static void loadBundle() {
        bundle = ResourceBundle.getBundle("messages", currentLocale);
    }

    /** Retourne la locale courante */
    public static Locale getLocale() {
        return currentLocale;
    }

    /** Retourne le code langue courant ("fr" ou "en") */
    public static String getLanguage() {
        return currentLocale.getLanguage();
    }

    /** Change la langue et notifie les listeners */
    public static void setLanguage(String lang) {
        currentLocale = new Locale(lang);
        PREFS.put(KEY_LANG, lang);
        loadBundle();
        notifyListeners();
    }

    /** Retourne le texte traduit pour la clé donnée */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "[" + key + "]";
        }
    }

    /** Retourne le texte traduit avec des paramètres formatés */
    public static String get(String key, Object... args) {
        return String.format(get(key), args);
    }

    /** Ajoute un listener appelé lors d'un changement de langue */
    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /** Supprime un listener */
    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable r : new ArrayList<>(listeners)) {
            r.run();
        }
    }
}
