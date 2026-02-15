package Controlles;

import Services.AuthService;
import Entites.Post;
import Services.PostService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.SQLException;

public class AddPostController {
    @FXML private TextField titleField;
    @FXML private TextArea contentField;

    private PostService postService = new PostService();

    @FXML
    private void handleSave() {
        String title = titleField.getText().trim();
        String content = contentField.getText().trim();

        // INPUT CONTROL
        if (title.isEmpty() || content.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText(null);
            alert.setContentText("Title and Content are required!");
            alert.showAndWait();
            return;
        }

        try {
            Post p = new Post();
            p.setTitle(title);
            p.setContent(content);
            p.setAuthorName(AuthService.currentUser.getUsername());
            p.setAuthorRole(AuthService.currentUser.getRole());
            postService.ajouter(p);
            returnToForum();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }
    private void returnToForum() {
        try {
            // Load the forum feed
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ForumFeed.fxml"));
            Parent view = loader.load();

            // Find the contentArea from the current scene and swap the view
            StackPane contentArea = (StackPane) titleField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleCancel() {
        returnToForum();
    }
}