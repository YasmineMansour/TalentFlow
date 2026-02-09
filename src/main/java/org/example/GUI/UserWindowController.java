package org.example.GUI;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.dao.UserDAO;
import org.example.model.User;
import java.util.List;
import java.util.Optional;

public class UserWindowController {

    @FXML private TextField nomField, prenomField, emailField, telField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label statusLabel;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colNom, colEmail, colRole, colTel;

    private UserDAO userDAO = new UserDAO();
    private User selectedUser = null; // Stocke l'utilisateur sélectionné pour modification/suppression

    @FXML
    public void initialize() {
        // Configuration des colonnes du tableau
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("telephone"));

        // Remplir le ComboBox des rôles
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "RH", "CANDIDAT"));

        // Écouter la sélection dans le tableau pour remplir le formulaire
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedUser = newSelection;
                remplirFormulaire(selectedUser);
            }
        });

        refreshTable();
    }

    private void refreshTable() {
        List<User> list = userDAO.readAll();
        userTable.setItems(FXCollections.observableArrayList(list));
    }

    private void remplirFormulaire(User user) {
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        telField.setText(user.getTelephone());
        roleCombo.setValue(user.getRole());
        passwordField.setText(user.getPassword());
    }

    @FXML
    private void handleInsert() {
        if (validerSaisie()) {
            User user = new User(
                    nomField.getText(), prenomField.getText(), emailField.getText(),
                    passwordField.getText(), roleCombo.getValue(), telField.getText()
            );
            userDAO.create(user);
            statusLabel.setText("✅ Utilisateur ajouté !");
            statusLabel.setStyle("-fx-text-fill: green;");
            refreshTable();
            clearFields();
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedUser == null) {
            statusLabel.setText("⚠️ Sélectionnez un utilisateur dans le tableau.");
            return;
        }
        if (validerSaisie()) {
            User updatedUser = new User(
                    selectedUser.getId(), nomField.getText(), prenomField.getText(),
                    emailField.getText(), passwordField.getText(), roleCombo.getValue(), telField.getText()
            );
            userDAO.update(updatedUser);
            statusLabel.setText("✅ Utilisateur modifié !");
            refreshTable();
            clearFields();
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedUser == null) {
            statusLabel.setText("⚠️ Sélectionnez un utilisateur à supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer " + selectedUser.getNom() + " ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userDAO.delete(selectedUser.getId());
            statusLabel.setText("🗑️ Utilisateur supprimé.");
            refreshTable();
            clearFields();
        }
    }

    @FXML
    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwordField.clear();
        telField.clear();
        roleCombo.setValue(null);
        selectedUser = null;
        userTable.getSelectionModel().clearSelection();
    }

    private boolean validerSaisie() {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        if (nomField.getText().isEmpty() || prenomField.getText().isEmpty()) {
            afficherErreur("Le nom et le prénom sont obligatoires.");
            return false;
        }
        if (!emailField.getText().matches(emailRegex)) {
            afficherErreur("L'adresse email n'est pas valide.");
            return false;
        }
        if (passwordField.getText().length() < 6) {
            afficherErreur("Le mot de passe doit faire au moins 6 caractères.");
            return false;
        }
        if (!telField.getText().matches("\\d{8}")) {
            afficherErreur("Le téléphone doit contenir exactement 8 chiffres.");
            return false;
        }
        if (roleCombo.getValue() == null) {
            afficherErreur("Veuillez choisir un rôle.");
            return false;
        }
        return true;
    }

    private void afficherErreur(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
    }
}