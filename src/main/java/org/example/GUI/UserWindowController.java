package org.example.GUI;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.dao.UserDAO;
import org.example.model.User;
import java.util.List;

public class UserWindowController {

    @FXML private TextField nomField, prenomField, emailField, telField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label statusLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colNom, colEmail, colRole, colTel;

    private UserDAO userDAO = new UserDAO();
    private User selectedUser = null;

    @FXML
    public void initialize() {
        // 1. Liaison des colonnes avec le modèle User
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("telephone"));

        // 2. Remplir le ComboBox
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "RH", "CANDIDAT"));

        // 3. Détecter la sélection dans le tableau
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
        if (!validerSaisie()) return;

        User newUser = new User(0, nomField.getText(), prenomField.getText(),
                emailField.getText(), passwordField.getText(),
                roleCombo.getValue(), telField.getText());
        userDAO.create(newUser);
        statusLabel.setText("✅ Utilisateur ajouté !");
        statusLabel.setStyle("-fx-text-fill: green;");
        refreshTable();
        clearFields();
    }

    @FXML
    private void handleUpdate() {
        if (selectedUser == null) {
            statusLabel.setText("⚠️ Sélectionnez un utilisateur à modifier.");
            return;
        }
        if (!validerSaisie()) return;

        selectedUser.setNom(nomField.getText());
        selectedUser.setPrenom(prenomField.getText());
        selectedUser.setEmail(emailField.getText());
        selectedUser.setTelephone(telField.getText());
        selectedUser.setRole(roleCombo.getValue());
        selectedUser.setPassword(passwordField.getText());

        userDAO.update(selectedUser);
        statusLabel.setText("✅ Modification réussie !");
        refreshTable();
    }

    @FXML
    private void handleDelete() {
        if (selectedUser == null) {
            statusLabel.setText("⚠️ Sélectionnez un utilisateur à supprimer.");
            return;
        }
        userDAO.delete(selectedUser.getId());
        statusLabel.setText("✅ Utilisateur supprimé !");
        refreshTable();
        clearFields();
    }

    @FXML
    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        telField.clear();
        passwordField.clear();
        roleCombo.setValue(null);
        selectedUser = null;
    }

    private boolean validerSaisie() {
        if (nomField.getText().isEmpty() || emailField.getText().isEmpty() || roleCombo.getValue() == null) {
            statusLabel.setText("⚠️ Champs obligatoires manquants.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return false;
        }
        return true;
    }
}