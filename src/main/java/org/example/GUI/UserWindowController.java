package org.example.GUI;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.dao.UserDAO;
import org.example.model.User;

public class UserWindowController {

    // Ces noms doivent correspondre EXACTEMENT aux fx:id du fichier FXML
    @FXML private TextField nomField, prenomField, emailField, telField;
    @FXML private PasswordField passField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colNom, colEmail, colRole, colTel;

    private UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // 1. Remplir la liste déroulante des rôles
        roleCombo.setItems(FXCollections.observableArrayList("admin", "rh", "candidat"));

        // 2. Lier les colonnes du tableau aux données de l'objet User
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("telephone"));

        // 3. Charger les données depuis la base de données
        refreshTable();

        // 4. LE CODE À AJOUTER : Écouter le clic sur une ligne du tableau
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                handleTableSelection(); // Cette méthode va remplir tes champs de texte
            }
        });
    }

    @FXML
    private void handleInsert() {
        if (validateInput()) {
            User newUser = new User(
                    0,
                    nomField.getText(),
                    prenomField.getText(),
                    emailField.getText(),
                    passField.getText(),
                    roleCombo.getValue(),
                    telField.getText()
            );

            userDAO.create(newUser);
            refreshTable();
            clearFields();
        }
    }

    private boolean validateInput() {
        if (nomField.getText().isEmpty() || emailField.getText().isEmpty() ||
                telField.getText().isEmpty() || roleCombo.getValue() == null) {
            showAlert("Champs manquants", "Veuillez remplir tous les champs obligatoires.");
            return false;
        }

        if (!emailField.getText().contains("@")) {
            showAlert("Email invalide", "Veuillez saisir un email correct.");
            return false;
        }

        if (telField.getText().length() != 8 || !telField.getText().matches("\\d+")) {
            showAlert("Téléphone invalide", "Le numéro doit contenir exactement 8 chiffres.");
            return false;
        }

        return true;
    }

    @FXML
    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        telField.clear();
        passField.clear();
        roleCombo.setValue(null);
    }

    private void refreshTable() {
        userTable.setItems(FXCollections.observableArrayList(userDAO.readAll()));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    // 1. Pour remplir le formulaire quand on clique sur une ligne du tableau
    @FXML
    private void handleTableSelection() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            nomField.setText(selected.getNom());
            prenomField.setText(selected.getPrenom());
            emailField.setText(selected.getEmail());
            telField.setText(selected.getTelephone());
            roleCombo.setValue(selected.getRole().toLowerCase());
        }
    }

    // 2. Pour SUPPRIMER
    @FXML
    private void handleDelete() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            userDAO.delete(selected.getId());
            refreshTable();
            clearFields();
        } else {
            showAlert("Sélection requise", "Veuillez sélectionner un utilisateur dans le tableau.");
        }
    }

    // 3. Pour MODIFIER
    @FXML
    private void handleUpdate() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null && validateInput()) {
            selected.setNom(nomField.getText());
            selected.setPrenom(prenomField.getText());
            selected.setEmail(emailField.getText());
            selected.setRole(roleCombo.getValue());
            selected.setTelephone(telField.getText());

            userDAO.update(selected);
            refreshTable();
            clearFields();
        } else {
            showAlert("Sélection requise", "Sélectionnez quelqu'un à modifier.");
        }
    }
}