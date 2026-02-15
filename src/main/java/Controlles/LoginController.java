package Controlles;

import Services.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private TextField regUserField;
    @FXML private PasswordField regPassField;
    @FXML private ComboBox<String> roleBox;

    @FXML
    private void handleLogin() {
        try {
            if (AuthService.login(userField.getText(), passField.getText())) {
                // Redirect to the MAIN layout with the sidebar
                switchScene("/forum_main.fxml");
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid Username or Password!").show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Login error: " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSignUp() {
        String user = regUserField.getText();
        String pass = regPassField.getText();
        String role = roleBox.getValue();

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty() || role == null) {
            new Alert(Alert.AlertType.WARNING, "Please fill all fields!").show();
            return;
        }

        try {
            AuthService.signUp(user, pass, role);
            new Alert(Alert.AlertType.INFORMATION, "Account created! Redirecting to login...").showAndWait();
            handleGoToLogin();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Username taken or database error!").show();
            e.printStackTrace();
        }
    }

    @FXML private void handleGoToSignUp() throws Exception { switchScene("/signup.fxml"); }

    @FXML private void handleGoToLogin() throws Exception { switchScene("/login.fxml"); }

    private void switchScene(String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Stage stage;
        if (userField != null && userField.getScene() != null) {
            stage = (Stage) userField.getScene().getWindow();
        } else if (regUserField != null && regUserField.getScene() != null) {
            stage = (Stage) regUserField.getScene().getWindow();
        } else {
            return;
        }

        stage.setScene(new Scene(root));
        stage.show();
    }
}