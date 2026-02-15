package controllers;

import entities.Candidature;
import entities.Offre;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.CandidatureService;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class CandidatureController {
    @FXML private TextField tfNomCandidat, tfEmail;
    @FXML private Label lblPathCV;
    @FXML private TableView<Candidature> tableCandidatures;
    @FXML private TableColumn<Candidature, String> colNom, colEmail, colStatut;

    private final CandidatureService service = new CandidatureService();
    private Offre offreSelectionnee;
    private String pathCV = "";

    // Cette méthode sera appelée depuis OffreController pour passer l'ID de l'offre
    public void setOffre(Offre offre) {
        this.offreSelectionnee = offre;
        rafraichir();
    }

    @FXML
    private void handleChoisirCV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            pathCV = selectedFile.getAbsolutePath();
            lblPathCV.setText(selectedFile.getName());
        }
    }

    @FXML
    private void handleAjouterCandidature() {
        if (tfNomCandidat.getText().isEmpty() || pathCV.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Veuillez remplir tous les champs !").show();
            return;
        }
        try {
            Candidature c = new Candidature();
            c.setNomCandidat(tfNomCandidat.getText());
            c.setEmail(tfEmail.getText());
            c.setCvPath(pathCV);
            c.setOffre(offreSelectionnee);

            service.postuler(c);
            rafraichir();
            new Alert(Alert.AlertType.INFORMATION, "Candidature envoyée !").show();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void rafraichir() {
        try {
            colNom.setCellValueFactory(new PropertyValueFactory<>("nomCandidat"));
            colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
            colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
            tableCandidatures.setItems(FXCollections.observableArrayList(
                    service.getCandidaturesParOffre(offreSelectionnee.getId())
            ));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/offres.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableCandidatures.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Gestion des Offres d'Emploi");
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de revenir aux offres.").show();
        }
    }
}