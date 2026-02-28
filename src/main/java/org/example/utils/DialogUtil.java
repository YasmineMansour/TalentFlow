package org.example.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

public class DialogUtil {

    private static Alert create(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert;
    }

    public static boolean confirmYesNo(String title, String header, String content) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, title, header, content);

        ButtonType yes = new ButtonType("Oui", ButtonBar.ButtonData.YES);
        ButtonType no  = new ButtonType("Non", ButtonBar.ButtonData.NO);

        alert.getButtonTypes().setAll(yes, no);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yes;
    }

    public static void info(String title, String msg) {
        create(Alert.AlertType.INFORMATION, title, null, msg).showAndWait();
    }

    public static void warning(String title, String msg) {
        create(Alert.AlertType.WARNING, title, null, msg).showAndWait();
    }

    public static void error(String title, String msg) {
        create(Alert.AlertType.ERROR, title, null, msg).showAndWait();
    }

    public static void error(String title, String msg, Exception ex) {
        Alert alert = create(Alert.AlertType.ERROR, title, null, msg);

        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        alert.setContentText(msg + "\n\nDétails:\n" + sw.toString());

        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        info(title, message);
    }

    public static void showWarning(String title, String message) {
        warning(title, message);
    }

    public static void showError(String title, String message) {
        error(title, message);
    }
}
