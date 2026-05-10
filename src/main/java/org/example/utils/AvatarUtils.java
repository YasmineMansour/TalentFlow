package org.example.utils;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Utilitaire partagé pour générer des avatars utilisateur.
 * Utilise i.pravatar.cc pour des photos humaines réalistes,
 * avec un fallback initiales + cercle coloré en attendant le chargement.
 */
public class AvatarUtils {

    private static final String[] PALETTE = {
        "#6c5ce7", "#00b894", "#e17055", "#0984e3", "#d63031",
        "#00cec9", "#e84393", "#fdcb6e", "#6ab04c", "#eb4d4b",
        "#7ed6df", "#22a6b3", "#be2edd", "#f9ca24", "#30336b"
    };

    /**
     * Crée un avatar avec une vraie photo humaine (via i.pravatar.cc) avec fallback initiales.
     *
     * @param name   nom complet de l'utilisateur (sert de seed pour un avatar unique et stable)
     * @param radius rayon du cercle avatar
     * @return StackPane contenant l'avatar
     */
    public static StackPane createAvatar(String name, double radius) {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        pane.setMinSize(radius * 2, radius * 2);
        pane.setMaxSize(radius * 2, radius * 2);

        // Cercle de fond avec couleur basée sur le nom (fallback)
        Color color = getAvatarColor(name);
        Circle circle = new Circle(radius, color);

        // Texte des initiales (fallback)
        Text initials = new Text(getInitials(name));
        initials.setFill(Color.WHITE);
        initials.setFont(Font.font("Segoe UI", FontWeight.BOLD, radius * 0.8));

        pane.getChildren().addAll(circle, initials);

        // Générer un ID stable basé sur le nom pour obtenir toujours la même photo
        int seed = name != null ? Math.abs(name.hashCode() % 70) + 1 : 1;
        int size = Math.max((int) (radius * 2), 64);
        // i.pravatar.cc fournit de vraies photos humaines réalistes
        String url = "https://i.pravatar.cc/" + size + "?img=" + seed;

        new Thread(() -> {
            try {
                Image img = new Image(url, size, size, true, true);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(radius * 2);
                    iv.setFitHeight(radius * 2);
                    iv.setPreserveRatio(true);
                    // Clip circulaire
                    Circle clip = new Circle(radius, radius, radius);
                    iv.setClip(clip);

                    javafx.application.Platform.runLater(() -> {
                        pane.getChildren().setAll(iv);
                    });
                }
            } catch (Exception ignored) {
                // Garder le fallback initiales
            }
        }).start();

        return pane;
    }

    /** Extrait les initiales d'un nom (ex: "Jean Dupont" → "JD") */
    public static String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    /** Génère une couleur unique et agréable basée sur le nom */
    public static Color getAvatarColor(String name) {
        if (name == null) return Color.web("#6c5ce7");
        int idx = Math.abs(name.hashCode()) % PALETTE.length;
        return Color.web(PALETTE[idx]);
    }

    /** Convertit un Color JavaFX en hex sans le # (ex: "6c5ce7") */
    public static String colorToHex(Color c) {
        return String.format("%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}
