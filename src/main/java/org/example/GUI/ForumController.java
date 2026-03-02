package org.example.GUI;

import org.example.model.*;
import org.example.services.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrôleur principal du forum TalentFlow.
 * Affiche le fil de posts avec votes, commentaires, recherche et tri.
 */
public class ForumController {

    @FXML private VBox postsContainer;
    @FXML private TextField searchBar;
    @FXML private ComboBox<String> sortFilter;

    private final PostService postService = new PostService();
    private final CommentService commentService = new CommentService();

    @FXML
    public void initialize() {
        if (sortFilter != null) {
            sortFilter.getItems().addAll("Plus récents", "Plus populaires");
            sortFilter.setValue("Plus récents");
            sortFilter.setOnAction(e -> refreshFeed(searchBar.getText()));
        }

        if (searchBar != null) {
            searchBar.textProperty().addListener((obs, oldVal, newVal) -> refreshFeed(newVal));
        }

        refreshFeed("");
    }

    /** Vérifie si l'utilisateur courant est admin ou l'auteur du post */
    private boolean isAdminOrOwner(String authorName) {
        User currentUser = UserSession.getInstance();
        if (currentUser == null) return false;
        return "ADMIN".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getFullName().equals(authorName);
    }

    /** Rafraîchit le fil de posts avec filtre de recherche */
    public void refreshFeed(String filter) {
        if (postsContainer == null) return;
        postsContainer.getChildren().clear();
        try {
            List<Post> posts = postService.afficher();

            // Filtrer par titre ou contenu
            if (filter != null && !filter.isEmpty()) {
                posts.removeIf(p -> !p.getTitle().toLowerCase().contains(filter.toLowerCase()) &&
                        !p.getContent().toLowerCase().contains(filter.toLowerCase()));
            }

            // Tri
            if (sortFilter != null && "Plus populaires".equals(sortFilter.getValue())) {
                posts.sort((p1, p2) -> Integer.compare(p2.getUpvotes(), p1.getUpvotes()));
            } else {
                posts.sort((p1, p2) -> Integer.compare(p2.getId(), p1.getId()));
            }

            for (Post p : posts) {
                postsContainer.getChildren().add(createPostCard(p));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Crée une carte visuelle pour un post */
    private VBox createPostCard(Post p) {
        VBox cardContainer = new VBox();
        cardContainer.getStyleClass().add("forum-post-card");

        // --- VOTE BOX ---
        VBox voteBox = new VBox(5);
        voteBox.getStyleClass().add("forum-vote-box");
        voteBox.setMinWidth(45);
        voteBox.setAlignment(Pos.CENTER);

        Button upBtn = new Button("▲");
        upBtn.getStyleClass().add("forum-vote-button");
        Label voteCount = new Label(String.valueOf(p.getUpvotes()));
        voteCount.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-text-fill: #6c5ce7; -fx-font-weight: bold;");
        Button downBtn = new Button("▼");
        downBtn.getStyleClass().add("forum-vote-button");

        upBtn.setOnAction(e -> handleVote(p, 1));
        downBtn.setOnAction(e -> handleVote(p, -1));
        voteBox.getChildren().addAll(upBtn, voteCount, downBtn);

        // --- CONTENT BOX ---
        VBox contentBox = new VBox(8);
        contentBox.setPadding(new Insets(10, 15, 10, 10));
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label authorInfo = new Label("👤 " + p.getAuthorName());
        authorInfo.getStyleClass().add("forum-post-meta");

        // --- BADGE DE TONALITÉ IA ---
        Label toneBadge = new Label("✨ Analyse...");
        toneBadge.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: #6c757d;");

        new Thread(() -> {
            String tone = ForumCloudService.detectTone(p.getContent());
            javafx.application.Platform.runLater(() -> {
                switch (tone) {
                    case "pos" -> {
                        toneBadge.setText("😊 POSITIF");
                        toneBadge.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: #00b894;");
                    }
                    case "neg" -> {
                        toneBadge.setText("⚠️ HOSTILE");
                        toneBadge.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: #d63031;");
                    }
                    default -> {
                        toneBadge.setText("😐 NEUTRE");
                        toneBadge.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: #0984e3;");
                    }
                }
            });
        }).start();

        Label roleBadge = new Label(p.getAuthorRole() != null ? p.getAuthorRole().toUpperCase() : "");
        roleBadge.getStyleClass().add("forum-role-badge");

        metaRow.getChildren().addAll(authorInfo, roleBadge, toneBadge);

        Label title = new Label(p.getTitle());
        title.getStyleClass().add("forum-post-title");
        Label content = new Label(p.getContent());
        content.getStyleClass().add("forum-post-content");
        content.setWrapText(true);

        HBox actions = new HBox(10);
        actions.getStyleClass().add("forum-action-row");

        Button btnComment = new Button("💬 Commentaires");
        btnComment.getStyleClass().addAll("forum-action-btn", "forum-btn-comment");

        // Bouton Message privé (si pas l'auteur)
        User currentUser = UserSession.getInstance();
        if (currentUser != null && !currentUser.getFullName().equals(p.getAuthorName())) {
            Button btnMessage = new Button("✉️ Message");
            btnMessage.getStyleClass().addAll("forum-action-btn", "forum-btn-message");
            btnMessage.setOnAction(e -> handleDirectMessage(p.getAuthorName()));
            actions.getChildren().add(btnMessage);
        }

        if (isAdminOrOwner(p.getAuthorName())) {
            Button btnEdit = new Button("✏️ Modifier");
            btnEdit.getStyleClass().addAll("forum-action-btn", "forum-btn-edit");
            btnEdit.setOnAction(e -> handleEditPost(p));

            Button btnDel = new Button("🗑 Supprimer");
            btnDel.getStyleClass().addAll("forum-action-btn", "forum-btn-delete");
            btnDel.setOnAction(e -> handleDeletePost(p));
            actions.getChildren().addAll(btnComment, btnEdit, btnDel);
        } else {
            actions.getChildren().add(btnComment);
        }

        contentBox.getChildren().addAll(metaRow, title, content, actions);

        // --- IMAGE ---
        VBox imageSideContainer = new VBox();
        imageSideContainer.setAlignment(Pos.CENTER_RIGHT);
        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try {
                ImageView postImg = new ImageView(new Image(p.getImagePath()));
                postImg.setFitWidth(150);
                postImg.setFitHeight(120);
                postImg.setPreserveRatio(true);
                imageSideContainer.getChildren().add(postImg);
            } catch (Exception e) {
                // Image non chargeable
            }
        }

        HBox bodyLayout = new HBox(contentBox, imageSideContainer);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        HBox mainHBox = new HBox(voteBox, bodyLayout);
        HBox.setHgrow(bodyLayout, Priority.ALWAYS);

        // --- SECTION COMMENTAIRES ---
        VBox commentSection = new VBox(10);
        commentSection.getStyleClass().add("forum-comment-box");
        commentSection.setVisible(false);
        commentSection.setManaged(false);

        btnComment.setOnAction(e -> {
            boolean show = !commentSection.isVisible();
            commentSection.setVisible(show);
            commentSection.setManaged(show);
            if (show) loadComments(p.getId(), commentSection);
        });

        cardContainer.getChildren().addAll(mainHBox, commentSection);
        return cardContainer;
    }

    /** Gère le vote (+1 / -1) */
    private void handleVote(Post p, int delta) {
        User currentUser = UserSession.getInstance();
        if (currentUser == null) return;
        try {
            postService.updateVotes(p.getId(), currentUser.getId(), delta);
            refreshFeed(searchBar.getText());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Supprime un post après confirmation */
    private void handleDeletePost(Post p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Êtes-vous sûr de vouloir supprimer ce post ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    postService.supprimer(p.getId());
                    refreshFeed("");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /** Ouvre l'éditeur de post */
    private void handleEditPost(Post p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/ForumEditPost.fxml"));
            Parent view = loader.load();
            ForumEditPostController controller = loader.getController();
            controller.setPostData(p);

            StackPane contentArea = (StackPane) postsContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de charger la page d'édition.").show();
        }
    }

    /** Ouvre la messagerie avec un utilisateur */
    private void handleDirectMessage(String authorName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/ForumMessaging.fxml"));
            Parent view = loader.load();
            ForumMessagingController controller = loader.getController();

            StackPane contentArea = (StackPane) postsContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
                javafx.application.Platform.runLater(() -> controller.initiateChatWith(authorName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Charge les commentaires d'un post */
    private void loadComments(int postId, VBox commentSection) {
        commentSection.getChildren().clear();
        User currentUser = UserSession.getInstance();

        // Zone de saisie
        HBox inputArea = new HBox(8);
        inputArea.setPadding(new Insets(5));
        TextField commentInput = new TextField();
        commentInput.setPromptText("Ajouter un commentaire...");
        HBox.setHgrow(commentInput, Priority.ALWAYS);
        Button btnAdd = new Button("Publier");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> {
            String text = commentInput.getText().trim();
            if (text.isEmpty()) {
                showAlert("Saisie invalide", "Le commentaire ne peut pas être vide !", Alert.AlertType.WARNING);
                return;
            }
            try {
                commentService.ajouterCommentaire(
                        postId,
                        currentUser.getId(),
                        currentUser.getFullName(),
                        text
                );
                commentInput.clear();
                loadComments(postId, commentSection);
            } catch (Exception ex) {
                showAlert("Erreur", "Impossible de publier le commentaire : " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
        inputArea.getChildren().addAll(commentInput, btnAdd);
        commentSection.getChildren().add(inputArea);

        // Liste des commentaires
        try {
            List<Comment> comments = commentService.getCommentsByPost(postId);
            for (Comment c : comments) {
                VBox commentBox = new VBox(5);
                commentBox.setPadding(new Insets(5, 10, 5, 10));
                commentBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e0e0e0;");

                HBox header = new HBox(10);
                header.setAlignment(Pos.CENTER_LEFT);
                Label cUser = new Label("👤 " + c.getAuthorName());
                cUser.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                header.getChildren().add(cUser);

                // Boutons édition/suppression pour l'auteur ou l'admin
                if (isAdminOrOwner(c.getAuthorName())) {
                    Button editC = new Button("✎");
                    editC.getStyleClass().add("forum-comment-action-btn");
                    editC.setOnAction(e -> {
                        TextInputDialog diag = new TextInputDialog(c.getContent());
                        diag.setTitle("Modifier le commentaire");
                        diag.setHeaderText("Modifiez votre commentaire :");
                        diag.setContentText("Commentaire :");
                        diag.showAndWait().ifPresent(newText -> {
                            if (newText.trim().isEmpty()) {
                                showAlert("Saisie invalide", "Le commentaire ne peut pas être vide !", Alert.AlertType.WARNING);
                            } else {
                                try {
                                    commentService.modifierCommentaire(c.getId(), newText);
                                    loadComments(postId, commentSection);
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                    });

                    Button delC = new Button("×");
                    delC.getStyleClass().add("forum-comment-action-btn");
                    delC.setOnAction(e -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                                "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
                        confirm.showAndWait().ifPresent(resp -> {
                            if (resp == ButtonType.YES) {
                                try {
                                    commentService.supprimerCommentaire(c.getId());
                                    loadComments(postId, commentSection);
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                    });
                    header.getChildren().addAll(editC, delC);
                }

                Label cText = new Label(c.getContent());
                cText.setWrapText(true);
                cText.setStyle("-fx-font-size: 13px; -fx-text-fill: #2d3436;");

                commentBox.getChildren().addAll(header, cText);
                commentSection.getChildren().add(commentBox);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Ouvre la page de création de post */
    @FXML
    public void handleCreatePost() {
        switchView("/org/example/ForumAddPost.fxml");
    }

    /** Ouvre la messagerie */
    @FXML
    public void handleOpenMessages() {
        switchView("/org/example/ForumMessaging.fxml");
    }

    /** Ouvre le chat IA */
    @FXML
    public void handleOpenAIChat() {
        switchView("/org/example/ForumAIChat.fxml");
    }

    private void switchView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            StackPane contentArea = (StackPane) postsContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
