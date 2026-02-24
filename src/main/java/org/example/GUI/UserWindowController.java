package org.example.GUI;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.utils.PdfExportService;
import org.example.utils.ValidationUtils;

import java.io.File;
import java.util.List;

public class UserWindowController {

    @FXML private TextField nomField, prenomField, emailField, telField, searchField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private Button togglePasswordBtn;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label statusLabel, countLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colNom, colPrenom, colEmail, colRole, colTel;

    private UserDAO userDAO = new UserDAO();
    private User selectedUser = null;
    private boolean passwordShown = false;

    @FXML
    public void initialize() {
        // 1. Liaison des colonnes
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("telephone"));

        // 2. Remplir le ComboBox
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "RH", "CANDIDAT"));

        // 3. Synchroniser eye toggle
        if (passwordVisible != null) {
            passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());
        }

        // 4. Sélection dans le tableau
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedUser = newSel;
                remplirFormulaire(selectedUser);
            }
        });

        // 5. Recherche en temps réel
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldText, newText) -> handleSearch());
        }

        refreshTable();
    }

    @FXML
    private void togglePassword() {
        passwordShown = !passwordShown;
        passwordField.setVisible(!passwordShown);
        passwordVisible.setVisible(passwordShown);
        togglePasswordBtn.setText(passwordShown ? "🙈" : "👁");
    }

    private void refreshTable() {
        List<User> list = userDAO.readAll();
        userTable.setItems(FXCollections.observableArrayList(list));
        updateCount(list.size());
    }

    private void updateCount(int count) {
        if (countLabel != null) {
            countLabel.setText(count + " utilisateur(s)");
        }
    }

    private void remplirFormulaire(User user) {
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        telField.setText(user.getTelephone());
        roleCombo.setValue(user.getRole());
        passwordField.clear();
        passwordField.setPromptText("Laisser vide pour ne pas modifier");
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField != null ? searchField.getText().trim() : "";
        List<User> results = keyword.isEmpty() ? userDAO.readAll() : userDAO.search(keyword);
        userTable.setItems(FXCollections.observableArrayList(results));
        updateCount(results.size());
    }

    @FXML
    private void handleInsert() {
        if (!validerSaisie(true)) return;

        if (confirmerAction("Voulez-vous vraiment ajouter cet utilisateur ?")) {
            User newUser = new User(0, nomField.getText().trim(), prenomField.getText().trim(),
                    emailField.getText().trim(), passwordField.getText(),
                    roleCombo.getValue(), telField.getText().trim());
            boolean success = userDAO.create(newUser);
            if (success) {
                statusLabel.setText("✅ Utilisateur ajouté avec succès !");
                statusLabel.setStyle("-fx-text-fill: green;");
                refreshTable();
                clearFields();
            } else {
                statusLabel.setText("❌ Échec : Email déjà utilisé.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedUser == null) {
            statusLabel.setText("⚠️ Sélectionnez un utilisateur à modifier.");
            statusLabel.setStyle("-fx-text-fill: #e67e22;");
            return;
        }

        if (!validerSaisie(false)) return;

        if (confirmerAction("Voulez-vous enregistrer les modifications pour " + selectedUser.getNom() + " ?")) {
            selectedUser.setNom(nomField.getText().trim());
            selectedUser.setPrenom(prenomField.getText().trim());
            selectedUser.setEmail(emailField.getText().trim());
            selectedUser.setTelephone(telField.getText().trim());
            selectedUser.setRole(roleCombo.getValue());

            // Si le champ mot de passe est vide, conserver l'ancien (haché)
            String newPassword = passwordField.getText();
            if (newPassword != null && !newPassword.isEmpty()) {
                selectedUser.setPassword(newPassword);
            }
            // sinon, le mot de passe reste celui déjà haché dans l'objet

            boolean success = userDAO.update(selectedUser);
            if (success) {
                statusLabel.setText("✅ Modification réussie !");
                statusLabel.setStyle("-fx-text-fill: green;");
                refreshTable();
            } else {
                statusLabel.setText("❌ Échec : Email déjà utilisé par un autre utilisateur.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedUser == null) {
            statusLabel.setText("⚠️ Sélectionnez un utilisateur à supprimer.");
            statusLabel.setStyle("-fx-text-fill: #e67e22;");
            return;
        }

        // Empêcher la suppression de son propre compte
        User currentUser = UserSession.getInstance();
        if (currentUser != null && currentUser.getId() == selectedUser.getId()) {
            statusLabel.setText("⚠️ Vous ne pouvez pas supprimer votre propre compte.");
            statusLabel.setStyle("-fx-text-fill: #e67e22;");
            return;
        }

        if (confirmerAction("Êtes-vous sûr de vouloir supprimer l'utilisateur " + selectedUser.getFullName() + " ?")) {
            boolean success = userDAO.delete(selectedUser.getId());
            if (success) {
                statusLabel.setText("✅ Utilisateur supprimé !");
                statusLabel.setStyle("-fx-text-fill: green;");
                refreshTable();
                clearFields();
            } else {
                statusLabel.setText("❌ Erreur lors de la suppression.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }

    @FXML
    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        telField.clear();
        passwordField.clear();
        passwordField.setPromptText("Mot de passe");
        roleCombo.setValue(null);
        selectedUser = null;
        userTable.getSelectionModel().clearSelection();
        statusLabel.setText("");
    }

    @FXML
    private void handleExportPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter la liste en PDF");
        fileChooser.setInitialFileName("utilisateurs_talentflow.pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(userTable.getScene().getWindow());
        if (file != null) {
            List<User> users = userTable.getItems();
            boolean success = PdfExportService.exportUserList(users, file.getAbsolutePath());
            if (success) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("✅ PDF exporté : " + file.getName());
            } else {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("❌ Erreur lors de l'export PDF.");
            }
        }
    }

    /**
     * Valide les champs de saisie.
     * @param isInsert true si c'est un ajout (mot de passe obligatoire), false si c'est une modification
     */
    private boolean validerSaisie(boolean isInsert) {
        statusLabel.setStyle("-fx-text-fill: red;");

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String tel = telField.getText().trim();
        String password = passwordField.getText();

        if (nom.isEmpty() || email.isEmpty() || roleCombo.getValue() == null) {
            statusLabel.setText("⚠️ Nom, Email et Rôle sont obligatoires.");
            return false;
        }
        if (ValidationUtils.isInvalidName(nom)) {
            statusLabel.setText("⚠️ Nom invalide (lettres uniquement, min 2 caractères).");
            return false;
        }
        if (!prenom.isEmpty() && ValidationUtils.isInvalidName(prenom)) {
            statusLabel.setText("⚠️ Prénom invalide (lettres uniquement, min 2 caractères).");
            return false;
        }
        if (ValidationUtils.isInvalidEmail(email)) {
            statusLabel.setText("⚠️ Email invalide.");
            return false;
        }
        if (!tel.isEmpty() && ValidationUtils.isInvalidTel(tel)) {
            statusLabel.setText("⚠️ Téléphone invalide (exactement 8 chiffres).");
            return false;
        }
        if (isInsert && (password == null || password.isEmpty())) {
            statusLabel.setText("⚠️ Le mot de passe est obligatoire pour un nouvel utilisateur.");
            return false;
        }
        if (!password.isEmpty() && ValidationUtils.isInvalidPassword(password)) {
            statusLabel.setText("⚠️ Mot de passe : min 8 caractères, 1 Maj, 1 Chiffre, 1 Symbole.");
            return false;
        }
        return true;
    }

    private boolean confirmerAction(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de l'action");
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType boutonOui = new ButtonType("Oui");
        ButtonType boutonNon = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(boutonOui, boutonNon);

        java.util.Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == boutonOui;
    }
}