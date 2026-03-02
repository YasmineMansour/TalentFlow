package org.example.GUI;

import org.example.model.Message;
import org.example.model.User;
import org.example.services.MessageService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrôleur de la messagerie privée du forum TalentFlow.
 * Permet d'envoyer, modifier et supprimer des messages entre utilisateurs.
 */
public class ForumMessagingController {

    @FXML private VBox chatBubbleContainer;
    @FXML private ListView<User> contactList;
    @FXML private TextField messageInput;
    @FXML private Label chatPartnerName;

    private final MessageService messageService = new MessageService();
    private User selectedPartner;

    @FXML
    public void initialize() {
        // Afficher le nom complet dans la liste de contacts
        contactList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getFullName());
                }
            }
        });

        loadSidebar();

        // Écouter les sélections dans la liste
        contactList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                this.selectedPartner = newVal;
                chatPartnerName.setText(newVal.getFullName());
                loadMessages();
            }
        });
    }

    /** Charge la liste des partenaires de chat */
    private void loadSidebar() {
        User currentUser = UserSession.getInstance();
        if (currentUser == null) return;
        try {
            List<User> partners = messageService.getChatPartners(currentUser.getId());
            contactList.getItems().setAll(partners);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Initie une conversation avec un utilisateur spécifique */
    public void initiateChatWith(String fullName) {
        User currentUser = UserSession.getInstance();
        if (currentUser == null) return;

        try {
            List<User> partners = messageService.getChatPartners(currentUser.getId());

            // Chercher si l'utilisateur est déjà dans les partenaires
            User target = null;
            for (User u : partners) {
                if (u.getFullName().equalsIgnoreCase(fullName)) {
                    target = u;
                    break;
                }
            }

            // Si pas trouvé, le chercher en BDD
            if (target == null) {
                target = messageService.getUserByFullName(fullName);
                if (target != null) {
                    partners.add(target);
                }
            }

            if (target != null) {
                contactList.getItems().setAll(partners);
                this.selectedPartner = target;
                chatPartnerName.setText(target.getFullName());
                contactList.getSelectionModel().select(target);
                contactList.scrollTo(target);
                loadMessages();
                messageInput.requestFocus();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Charge les messages d'une conversation */
    private void loadMessages() {
        if (selectedPartner == null) return;
        User currentUser = UserSession.getInstance();
        if (currentUser == null) return;

        chatBubbleContainer.getChildren().clear();
        try {
            List<Message> history = messageService.getChatHistory(currentUser.getId(), selectedPartner.getId());
            for (Message m : history) {
                chatBubbleContainer.getChildren().add(createBubble(m));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Crée une bulle de message */
    private HBox createBubble(Message m) {
        User currentUser = UserSession.getInstance();
        HBox row = new HBox();
        VBox bubble = new VBox(5);
        Label content = new Label(m.getContent());
        content.setWrapText(true);
        content.setMaxWidth(400);
        bubble.getChildren().add(content);
        bubble.setPadding(new Insets(10));

        if (m.getSenderId() == currentUser.getId()) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: linear-gradient(to bottom right, #6c5ce7, #a29bfe); -fx-background-radius: 15 15 2 15;");
            content.setStyle("-fx-text-fill: white;");

            // Menu contextuel pour modifier/supprimer
            ContextMenu menu = new ContextMenu();
            MenuItem edit = new MenuItem("Modifier");
            MenuItem delete = new MenuItem("Supprimer");
            edit.setOnAction(e -> handleEditMessage(m));
            delete.setOnAction(e -> handleDeleteMessage(m));
            menu.getItems().addAll(edit, delete);
            bubble.setOnContextMenuRequested(e -> menu.show(bubble, e.getScreenX(), e.getScreenY()));
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 15 15 15 2;");
            content.setStyle("-fx-text-fill: #2d3436;");
        }

        row.getChildren().add(bubble);
        return row;
    }

    /** Envoyer un message */
    @FXML
    private void handleSendMessage() {
        if (selectedPartner == null) return;
        User currentUser = UserSession.getInstance();
        if (currentUser == null) return;

        String text = messageInput.getText().trim();
        if (text.isEmpty()) {
            showAlert("Saisie invalide", "Impossible d'envoyer un message vide !", Alert.AlertType.WARNING);
            return;
        }

        try {
            messageService.sendMessage(currentUser.getId(), selectedPartner.getId(), text);
            messageInput.clear();
            loadMessages();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'envoyer le message.", Alert.AlertType.ERROR);
        }
    }

    /** Modifier un message */
    private void handleEditMessage(Message msg) {
        if (msg == null) return;

        TextInputDialog dialog = new TextInputDialog(msg.getContent());
        dialog.setTitle("Modifier le message");
        dialog.setHeaderText("Modifiez votre message :");
        dialog.setContentText("Message :");

        dialog.showAndWait().ifPresent(newContent -> {
            if (newContent.trim().isEmpty()) {
                showAlert("Saisie invalide", "Le message ne peut pas être vide !", Alert.AlertType.WARNING);
            } else {
                try {
                    messageService.modifierMessage(msg.getId(), newContent);
                    loadMessages();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de modifier le message.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    /** Supprimer un message */
    private void handleDeleteMessage(Message msg) {
        if (msg == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le message");
        confirm.setHeaderText("Êtes-vous sûr de vouloir supprimer ce message ?");
        confirm.setContentText(msg.getContent());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    messageService.supprimerMessage(msg.getId());
                    loadMessages();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de supprimer le message.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
