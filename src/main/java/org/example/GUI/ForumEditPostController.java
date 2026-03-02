package org.example.GUI;

import org.example.model.Post;
import org.example.services.ForumCloudService;
import org.example.services.PostService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;

/**
 * Contrôleur pour l'édition d'un post existant dans le forum.
 */
public class ForumEditPostController {

    @FXML private TextField titleField;
    @FXML private TextArea contentField;
    @FXML private ImageView imagePreview;
    @FXML private Label imageNameLabel;

    private Post currentPost;
    private String selectedImagePath;
    private final PostService postService = new PostService();

    /** Initialise les champs avec les données du post existant */
    public void setPostData(Post post) {
        this.currentPost = post;
        this.selectedImagePath = post.getImagePath();

        titleField.setText(post.getTitle());
        contentField.setText(post.getContent());

        if (post.getImagePath() != null && !post.getImagePath().isEmpty()) {
            try {
                imagePreview.setImage(new Image(post.getImagePath()));
            } catch (Exception e) {
                System.out.println("Impossible de charger l'aperçu : " + e.getMessage());
            }
        }
    }

    /** Upload d'une nouvelle image */
    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(titleField.getScene().getWindow());

        if (selectedFile != null) {
            imageNameLabel.setText("Upload en cours...");

            new Thread(() -> {
                try {
                    String cloudUrl = ForumCloudService.uploadImage(selectedFile);
                    javafx.application.Platform.runLater(() -> {
                        this.selectedImagePath = cloudUrl;
                        imageNameLabel.setText("✅ Image mise à jour");
                        imagePreview.setImage(new Image(cloudUrl));
                        imagePreview.setVisible(true);
                        imagePreview.setManaged(true);
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() ->
                            new Alert(Alert.AlertType.ERROR, "Échec de l'upload : " + e.getMessage()).show());
                }
            }).start();
        }
    }

    /** Sauvegarde les modifications */
    @FXML
    private void handleSave() {
        if (titleField.getText().trim().isEmpty() || contentField.getText().trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Les champs ne peuvent pas être vides !").show();
            return;
        }

        try {
            currentPost.setTitle(titleField.getText().trim());
            currentPost.setContent(contentField.getText().trim());
            currentPost.setImagePath(selectedImagePath);

            postService.modifier(currentPost);
            returnToForum();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Échec de la mise à jour : " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleCancel() {
        returnToForum();
    }

    private void returnToForum() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/ForumFeed.fxml"));
            Parent view = loader.load();
            StackPane contentArea = (StackPane) titleField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
