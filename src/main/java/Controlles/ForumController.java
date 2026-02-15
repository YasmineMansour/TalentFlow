package Controlles;

import Entites.*;
import Services.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.SQLException;
import java.util.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Insets;

public class ForumController {
    @FXML private VBox postsContainer;
    @FXML private TextField searchBar;
    @FXML private ComboBox<String> sortFilter;
    @FXML private Button darkModeBtn;

    private PostService postService = new PostService();
    private CommentService commentService = new CommentService();
    private boolean isDark = false;

    @FXML
    public void initialize() {
        if (sortFilter != null) {
            sortFilter.getItems().addAll("Newest", "Most Popular");
            sortFilter.setValue("Newest");
            sortFilter.setOnAction(e -> refreshFeed(searchBar.getText()));
        }

        if (searchBar != null) {
            searchBar.textProperty().addListener((obs, oldVal, newVal) -> refreshFeed(newVal));
        }

        refreshFeed("");
    }

    // Helper to check permissions
    private boolean isAdminOrOwner(String authorName) {
        if (AuthService.currentUser == null) return false;
        return AuthService.currentUser.getRole().equalsIgnoreCase("ADMIN") ||
                AuthService.currentUser.getUsername().equals(authorName);
    }

    @FXML
    public void handleOpenMessages() {
        switchView("/MessagingView.fxml");
    }

    @FXML
    public void handleOpenForum() {
        switchView("/ForumView.fxml");
    }

    public void refreshFeed(String filter) {
        if (postsContainer == null) return;
        postsContainer.getChildren().clear();
        try {
            List<Post> posts = postService.afficher();

            posts.removeIf(p -> !p.getTitle().toLowerCase().contains(filter.toLowerCase()) &&
                    !p.getContent().toLowerCase().contains(filter.toLowerCase()));

            if (sortFilter != null && "Most Popular".equals(sortFilter.getValue())) {
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

    private VBox createPostCard(Post p) {
        VBox cardContainer = new VBox();
        cardContainer.getStyleClass().add("post-card");

        VBox voteBox = new VBox(5);
        voteBox.getStyleClass().add("vote-box");
        voteBox.setMinWidth(45);
        voteBox.setAlignment(Pos.CENTER);

        Button upBtn = new Button("▲");
        upBtn.getStyleClass().add("vote-button");
        Label voteCount = new Label(String.valueOf(p.getUpvotes()));
        voteCount.setStyle("-fx-font-weight: bold;");
        Button downBtn = new Button("▼");
        downBtn.getStyleClass().add("vote-button");

        upBtn.setOnAction(e -> handleVote(p, 1));
        downBtn.setOnAction(e -> handleVote(p, -1));

        voteBox.getChildren().addAll(upBtn, voteCount, downBtn);

        VBox contentBox = new VBox(8);
        contentBox.setPadding(new Insets(10, 15, 10, 10));
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label authorInfo = new Label("u/" + p.getAuthorName());
        authorInfo.getStyleClass().add("post-meta");

        Label roleBadge = new Label();
        if (p.getAuthorRole() != null) {
            String role = p.getAuthorRole().toLowerCase();
            roleBadge.setText(role.toUpperCase());
            roleBadge.getStyleClass().addAll("role-badge", role);
        }

        metaRow.getChildren().addAll(authorInfo, roleBadge);

        Label title = new Label(p.getTitle());
        title.getStyleClass().add("post-title");
        Label content = new Label(p.getContent());
        content.getStyleClass().add("post-content");
        content.setWrapText(true);

        HBox actions = new HBox(10);
        actions.getStyleClass().add("action-row");

        Button btnComment = new Button("💬 Comments");
        btnComment.getStyleClass().add("action-button");

        Button btnMessage = new Button("✉️ Message");
        btnMessage.getStyleClass().add("action-button");

        if (AuthService.currentUser != null && !AuthService.currentUser.getUsername().equals(p.getAuthorName())) {
            btnMessage.setOnAction(e -> handleDirectMessage(p.getAuthorName()));
            actions.getChildren().add(btnMessage);
        }

        if (isAdminOrOwner(p.getAuthorName())) {
            Button btnEdit = new Button("✏️ Edit");
            btnEdit.getStyleClass().add("action-button");
            btnEdit.setOnAction(e -> handleEditPost(p));

            Button btnDel = new Button("🗑 Delete");
            btnDel.getStyleClass().addAll("action-button", "delete-button");
            btnDel.setOnAction(e -> handleDeletePost(p));

            actions.getChildren().addAll(btnComment, btnEdit, btnDel);
        } else {
            actions.getChildren().add(btnComment);
        }

        contentBox.getChildren().addAll(metaRow, title, content, actions);
        HBox mainHBox = new HBox(voteBox, contentBox);

        VBox commentSection = new VBox(10);
        commentSection.getStyleClass().add("comment-box");
        commentSection.setVisible(false);
        commentSection.setManaged(false);

        btnComment.setOnAction(e -> {
            boolean show = !commentSection.isVisible();
            commentSection.setVisible(show);
            commentSection.setManaged(show);
            if(show) loadComments(p.getId(), commentSection);
        });

        cardContainer.getChildren().addAll(mainHBox, commentSection);
        return cardContainer;
    }

    @FXML
    public void handleDirectMessage(String authorName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MessagingView.fxml"));
            Parent view = loader.load();
            MessagingController controller = loader.getController();
            controller.initiateChatWith(authorName);

            StackPane contentArea = (StackPane) postsContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void switchView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            StackPane contentArea = (StackPane) postsContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleVote(Post p, int delta) {
        try {
            postService.updateVotes(p.getId(), AuthService.currentUser.getId(), delta);
            refreshFeed(searchBar.getText());
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleDeletePost(Post p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this post?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    postService.supprimer(p.getId());
                    refreshFeed("");
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });
    }
    private void handleEditPost(Post p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Post");
        TextField editTitle = new TextField(p.getTitle());
        TextArea editContent = new TextArea(p.getContent());

        VBox layout = new VBox(10, new Label("Title:"), editTitle, new Label("Content:"), editContent);
        dialog.getDialogPane().setContent(layout);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // 1. INPUT CONTROL: Check empty fields
                if (editTitle.getText().trim().isEmpty() || editContent.getText().trim().isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Fields cannot be empty!").show();
                    return;
                }

                // 2. CONFIRMATION CONTROL: Are you sure?
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to save these changes to the post?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(confResponse -> {
                    if (confResponse == ButtonType.YES) {
                        try {
                            p.setTitle(editTitle.getText());
                            p.setContent(editContent.getText());
                            postService.modifier(p);
                            refreshFeed("");
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void loadComments(int postId, VBox commentSection) {
        commentSection.getChildren().clear();
        HBox inputArea = new HBox(8);
        TextField commentInput = new TextField();
        commentInput.setPromptText("Add a comment...");
        HBox.setHgrow(commentInput, Priority.ALWAYS);
        Button btnAdd = new Button("Post");
        btnAdd.setOnAction(e -> {
            if(commentInput.getText().trim().isEmpty()) return;
            try {
                commentService.ajouterCommentaire(postId, AuthService.currentUser.getUsername(), commentInput.getText());
                commentInput.clear();
                loadComments(postId, commentSection);
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        inputArea.getChildren().addAll(commentInput, btnAdd);
        commentSection.getChildren().add(inputArea);

        try {
            List<Comment> comments = commentService.getCommentsByPost(postId);
            for (Comment c : comments) {
                VBox commentBox = new VBox(2);
                HBox header = new HBox(10);
                Label cUser = new Label("u/" + c.getAuthorName());
                cUser.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                header.getChildren().add(cUser);

                Label cText = new Label(c.getContent());
                cText.setWrapText(true);

                if (isAdminOrOwner(c.getAuthorName())) {
                    Button editC = new Button("✎");
                    editC.setStyle("-fx-background-color: transparent; -fx-font-size: 10px;");
                    editC.setOnAction(e -> {
                        TextInputDialog diag = new TextInputDialog(c.getContent());
                        diag.showAndWait().ifPresent(newText -> {
                            try {
                                commentService.modifierCommentaire(c.getId(), newText);
                                loadComments(postId, commentSection);
                            } catch (SQLException ex) { ex.printStackTrace(); }
                        });
                    });

                    Button delC = new Button("×");
                    delC.setStyle("-fx-background-color: transparent; -fx-text-fill: red;");
                    delC.setOnAction(e -> {
                        try {
                            commentService.supprimerCommentaire(c.getId());
                            loadComments(postId, commentSection);
                        } catch (SQLException ex) { ex.printStackTrace(); }
                    });
                    header.getChildren().addAll(editC, delC);
                }
                commentBox.getChildren().addAll(header, cText);
                commentSection.getChildren().add(commentBox);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleCreatePost() {
        switchView("/add_post.fxml");
    }

    @FXML
    private void handleDarkMode() {
        Scene scene = postsContainer.getScene();
        if (scene != null) {
            Pane root = (Pane) scene.getRoot();
            isDark = !isDark;
            if (isDark) {
                root.getStyleClass().add("dark-theme");
                if (darkModeBtn != null) darkModeBtn.setText("☀️ Light Mode");
            } else {
                root.getStyleClass().remove("dark-theme");
                if (darkModeBtn != null) darkModeBtn.setText("🌙 Dark Mode");
            }
        }
    }
}