package Controlles;

import Entites.Message;
import Entites.User;
import Services.AuthService;
import Services.MessageService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.SQLException;
import java.util.List;

public class MessagingController {
    @FXML private VBox chatBubbleContainer;
    @FXML private ListView<User> contactList;
    @FXML private TextField messageInput;
    @FXML private Label chatPartnerName;

    private MessageService messageService = new MessageService();
    private User selectedPartner;

    @FXML
    public void initialize() {
        contactList.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername());
                }
            }
        });

        loadSidebar();

        contactList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedPartner = newVal;
            if (newVal != null) {
                chatPartnerName.setText(newVal.getUsername());
            }
            loadMessages();
        });
    }

    private void loadSidebar() {
        try {
            List<User> partners = messageService.getChatPartners(AuthService.currentUser.getId());
            contactList.getItems().setAll(partners);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadMessages() {
        if (selectedPartner == null) return;
        chatBubbleContainer.getChildren().clear();
        try {
            List<Message> history = messageService.getChatHistory(AuthService.currentUser.getId(), selectedPartner.getId());
            for (Message m : history) {
                chatBubbleContainer.getChildren().add(createBubble(m));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private HBox createBubble(Message m) {
        HBox row = new HBox();
        VBox bubble = new VBox(5);
        Label content = new Label(m.getContent());
        content.setWrapText(true);
        content.setMaxWidth(400);

        bubble.getChildren().add(content);
        bubble.setPadding(new Insets(10));

        if (m.getSenderId() == AuthService.currentUser.getId()) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: #0084ff; -fx-background-radius: 15 15 2 15;");
            content.setStyle("-fx-text-fill: white;");

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            Button btnEdit = new Button("✎");
            Button btnDel = new Button("🗑");
            String btnStyle = "-fx-background-color: transparent; -fx-text-fill: #D1EAFF; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 0;";
            btnEdit.setStyle(btnStyle);
            btnDel.setStyle(btnStyle + "-fx-text-fill: #FFDADA;");

            btnEdit.setOnAction(e -> handleEditMessage(m));
            btnDel.setOnAction(e -> handleDeleteMessage(m));
            actions.getChildren().addAll(btnEdit, btnDel);
            bubble.getChildren().add(actions);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 15 15 15 2;");
            content.setStyle("-fx-text-fill: #1E293B;");
        }

        row.getChildren().add(bubble);
        return row;
    }

    @FXML
    private void handleSendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || selectedPartner == null) return;

        try {
            messageService.sendMessage(AuthService.currentUser.getId(), selectedPartner.getId(), text);
            messageInput.clear();
            loadMessages();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleEditMessage(Message m) {
        TextInputDialog dialog = new TextInputDialog(m.getContent());
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Modify your message:");

        dialog.showAndWait().ifPresent(newText -> {
            if (newText.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Message cannot be empty!").show();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to edit this message?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    try {
                        messageService.modifierMessage(m.getId(), newText);
                        loadMessages();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    private void handleDeleteMessage(Message m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete message permanently?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    messageService.supprimerMessage(m.getId());
                    loadMessages();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }

    public void initiateChatWith(String username) {
        try {
            List<User> partners = messageService.getChatPartners(AuthService.currentUser.getId());
            for (User u : partners) {
                if (u.getUsername().equals(username)) {
                    contactList.getSelectionModel().select(u);
                    this.selectedPartner = u;
                    loadMessages();
                    break;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}