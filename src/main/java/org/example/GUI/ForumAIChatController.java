package org.example.GUI;

import org.example.services.ForumAIService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Contrôleur du chat IA du forum TalentFlow.
 * Permet de poser des questions à l'IA en utilisant les posts du forum comme contexte.
 */
public class ForumAIChatController {

    @FXML private VBox chatContainer;
    @FXML private TextField questionField;

    private final ForumAIService aiService = new ForumAIService();

    /** Envoie une question à l'IA */
    @FXML
    private void handleAsk() {
        String question = questionField.getText().trim();
        if (question.isEmpty()) return;

        addUserMessage(question);
        questionField.clear();

        new Thread(() -> {
            try {
                String response = aiService.askForumAI(question);
                Platform.runLater(() -> addAIMessage(response));
            } catch (Exception e) {
                Platform.runLater(() -> addAIMessage("Erreur lors du traitement de la requête."));
            }
        }).start();
    }

    /** Ajoute un message utilisateur dans le chat */
    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        Label bubble = new Label(text);
        bubble.getStyleClass().add("forum-user-bubble");
        bubble.setWrapText(true);

        row.getChildren().add(bubble);
        chatContainer.getChildren().add(row);
    }

    /** Ajoute une réponse IA dans le chat */
    private void addAIMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label bubble = new Label(text);
        bubble.getStyleClass().add("forum-ai-bubble");
        bubble.setWrapText(true);

        row.getChildren().add(bubble);
        chatContainer.getChildren().add(row);
    }
}
