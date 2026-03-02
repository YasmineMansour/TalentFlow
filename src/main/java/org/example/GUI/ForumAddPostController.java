package org.example.GUI;

import org.example.model.Post;
import org.example.model.User;
import org.example.services.ForumCloudService;
import org.example.services.ModerationService;
import org.example.services.PostService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Contrôleur pour l'ajout d'un nouveau post dans le forum.
 */
public class ForumAddPostController {

    @FXML private TextField titleField;
    @FXML private TextArea contentField;
    @FXML private ImageView imagePreview;
    @FXML private Label imageNameLabel;

    private String selectedImagePath = null;
    private final PostService postService = new PostService();

    /** Upload d'image vers le cloud (ImgBB) */
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
                        imageNameLabel.setText("✅ Image uploadée");
                        imagePreview.setImage(new javafx.scene.image.Image(cloudUrl));
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

    /** Sauvegarde le post après validation de modération */
    @FXML
    private void handleSave() {
        User currentUser = UserSession.getInstance();
        if (currentUser == null) return;

        String title = titleField.getText().trim();
        String content = contentField.getText().trim();
        String fullName = currentUser.getFullName();

        if (title.isEmpty() || content.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Le titre et le contenu sont obligatoires !").show();
            return;
        }

        try {
            // Vérification rate limit
            if (ModerationService.isRateLimited(fullName)) {
                new Alert(Alert.AlertType.ERROR, "Veuillez attendre 60 secondes avant de poster à nouveau !").show();
                return;
            }

            // Validation du contenu (mots interdits + API)
            ModerationService.validateContent(title, content, currentUser.getRole());

            Post p = new Post();
            p.setTitle(title);
            p.setContent(content);
            p.setAuthorId(currentUser.getId());
            p.setAuthorName(fullName);
            p.setAuthorRole(currentUser.getRole());
            p.setImagePath(selectedImagePath);

            postService.ajouter(p);
            ModerationService.recordPost(fullName);
            returnToForum();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
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
