package org.example.utils;

import javafx.scene.Scene;
import javafx.scene.Parent;
import java.util.prefs.Preferences;

/**
 * Gestionnaire de thème (Dark/Light) pour TalentFlow.
 * Persiste le choix via java.util.prefs.Preferences.
 */
public class ThemeManager {

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String DARK_CSS = "/org/example/dark-theme.css";
    private static final String DARK_STYLE_CLASS = "dark-theme";

    private static boolean darkMode;

    static {
        darkMode = PREFS.getBoolean(KEY_DARK_MODE, false);
    }

    /** Retourne true si le dark mode est actif */
    public static boolean isDarkMode() {
        return darkMode;
    }

    /** Active ou désactive le dark mode et persiste le choix */
    public static void setDarkMode(boolean enabled) {
        darkMode = enabled;
        PREFS.putBoolean(KEY_DARK_MODE, enabled);
    }

    /** Applique le thème courant à une scène */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        String darkCssUrl = ThemeManager.class.getResource(DARK_CSS).toExternalForm();

        if (darkMode) {
            if (!scene.getStylesheets().contains(darkCssUrl)) {
                scene.getStylesheets().add(darkCssUrl);
            }
            scene.getRoot().getStyleClass().removeAll(DARK_STYLE_CLASS);
            scene.getRoot().getStyleClass().add(DARK_STYLE_CLASS);
        } else {
            scene.getStylesheets().remove(darkCssUrl);
            scene.getRoot().getStyleClass().removeAll(DARK_STYLE_CLASS);
        }
    }

    /** Bascule le thème et l'applique à la scène */
    public static void toggleTheme(Scene scene) {
        setDarkMode(!darkMode);
        applyTheme(scene);
    }
}
