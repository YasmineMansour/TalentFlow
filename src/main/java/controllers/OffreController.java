package controllers;

import entities.Offre;
import services.OffreService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class OffreController implements Initializable {
    @FXML private TextField tfTitre, tfLocalisation;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<String> comboStatut;
    @FXML private TableView<Offre> tableOffres;
    @FXML private TableColumn<Offre, String> colTitre, colDesc, colLoc, colStatut;

    private final OffreService service = new OffreService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialisation du ComboBox
        comboStatut.setItems(FXCollections.observableArrayList("PUBLISHED", "CLOSED", "ARCHIVED"));

        // Liaison des colonnes avec les attributs de l'entité Offre
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colLoc.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        rafraichir();
    }

    @FXML
    private void handleAjouter() {
        if (tfTitre.getText().isEmpty() || comboStatut.getValue() == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Champs obligatoires", "Veuillez remplir au moins le titre et le statut.");
            return;
        }
        try {
            Offre o = new Offre();
            o.setTitre(tfTitre.getText());
            o.setDescription(taDescription.getText());
            o.setLocalisation(tfLocalisation.getText());
            o.setStatut(comboStatut.getValue());

            service.ajouter(o);
            rafraichir();
            nettoyer();
            afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "L'offre a été ajoutée avec succès !");
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de base de données", "Détail : " + e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection != null) {
            if (confirmerAction("Voulez-vous modifier cette offre ?")) {
                try {
                    selection.setTitre(tfTitre.getText());
                    selection.setDescription(taDescription.getText());
                    selection.setLocalisation(tfLocalisation.getText());
                    selection.setStatut(comboStatut.getValue());

                    service.modifier(selection);
                    rafraichir();
                    nettoyer();
                    afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "L'offre a été modifiée !");
                } catch (SQLException e) {
                    e.printStackTrace();
                    afficherAlerte(Alert.AlertType.ERROR, "Erreur de modification", e.getMessage());
                }
            }
        } else {
            afficherAlerte(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une offre dans le tableau.");
        }
    }

    @FXML
    private void handleSupprimer() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection != null) {
            if (confirmerAction("Voulez-vous vraiment supprimer l'offre : " + selection.getTitre() + " ?")) {
                try {
                    service.supprimer(selection.getId());
                    rafraichir();
                    nettoyer();
                    afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "L'offre a été supprimée.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    afficherAlerte(Alert.AlertType.ERROR, "Erreur de suppression", e.getMessage());
                }
            }
        } else {
            afficherAlerte(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une offre à supprimer.");
        }
    }

    @FXML
    private void ouvrirCandidatures() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/candidatures.fxml"));
                Parent root = loader.load();

                CandidatureController controller = loader.getController();
                controller.setOffre(selection);

                Stage stage = (Stage) tableOffres.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Candidatures pour : " + selection.getTitre());
            } catch (IOException e) {
                e.printStackTrace();
                afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page candidatures.");
            }
        } else {
            afficherAlerte(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une offre pour voir ses candidatures.");
        }
    }

    @FXML
    private void chargerSelection() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection != null) {
            tfTitre.setText(selection.getTitre());
            taDescription.setText(selection.getDescription());
            tfLocalisation.setText(selection.getLocalisation());
            comboStatut.setValue(selection.getStatut());
        }
    }

    private boolean confirmerAction(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void rafraichir() {
        try {
            tableOffres.setItems(FXCollections.observableArrayList(service.afficher()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void nettoyer() {
        tfTitre.clear();
        taDescription.clear();
        tfLocalisation.clear();
        comboStatut.setValue(null);
        tableOffres.getSelectionModel().clearSelection();
    }
}