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

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "RH", "CANDIDAT"));

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("telephone"));

        refreshTable();
    }

    // --- LOGIQUE DE VALIDATION ---
    private boolean validerSaisie() {
        String nameRegex = "^[a-zA-Z\\s]+$"; // Lettres uniquement
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$";
        // 8 chars, 1 Maj, 1 Min, 1 Chiffre, 1 Symbole
        String passRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

        if (!nomField.getText().matches(nameRegex) || !prenomField.getText().matches(nameRegex)) {
            statusLabel.setText("Erreur : Nom/Prénom ne doivent contenir que des lettres.");
            return false;
        }
        if (!emailField.getText().matches(emailRegex)) {
            statusLabel.setText("Erreur : Format d'email invalide.");
            return false;
        }
        if (!passwordField.getText().matches(passRegex)) {
            statusLabel.setText("Erreur : Mot de passe trop faible.");
            return false;
        }
        if (telField.getText().length() != 8 || !telField.getText().matches("\\d+")) {
            statusLabel.setText("Erreur : Le téléphone doit contenir 8 chiffres.");
            return false;
        }
        if (roleCombo.getValue() == null) {
            statusLabel.setText("Erreur : Veuillez choisir un rôle.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleInsert() {
        if (validerSaisie()) {
            try {
                User user = new User(
                        nomField.getText(),
                        prenomField.getText(),
                        emailField.getText(),
                        passwordField.getText(),
                        roleCombo.getValue(),
                        telField.getText()
                );
                userDAO.create(user);
                statusLabel.setText("Utilisateur ajouté !");
                refreshTable();
                clearFields();
            } catch (Exception e) {
                statusLabel.setText("Erreur lors de l'insertion.");
                e.printStackTrace();
            }
        }
    }

    private void refreshTable() {
        List<User> list = userDAO.readAll();
        userTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwordField.clear();
        telField.clear();
        roleCombo.setValue(null);
        statusLabel.setText("");
    }

    // Ajoute ici tes méthodes handleUpdate et handleDelete sur le même modèle de try-catch
}