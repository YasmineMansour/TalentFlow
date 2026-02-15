package Controlles;

import Services.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene; // MISSING IMPORT
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage; // MISSING IMPORT

public class MainController {
    @FXML private BorderPane mainRoot;
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private Label logoLabel;
    @FXML private Button darkModeBtn;

    private boolean isDark = false;

    @FXML
    public void initialize() {
        showForum(); // This loads the posts on start
    }

    @FXML
    public void showForum() {
        loadView("/ForumFeed.fxml"); // Point to the new content file
    }

    @FXML
    public void showMessaging() {
        loadView("/MessagingView.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            AuthService.currentUser = null;
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) mainRoot.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDarkMode() {
        isDark = !isDark;
        if (isDark) {
            mainRoot.getStyleClass().add("dark-theme");
            darkModeBtn.setText("☀️ Light Mode");
        } else {
            mainRoot.getStyleClass().remove("dark-theme");
            darkModeBtn.setText("🌙 Dark Mode");
        }
    }
}