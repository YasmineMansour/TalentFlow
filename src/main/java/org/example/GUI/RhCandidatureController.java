package org.example.GUI;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.example.model.Candidature;
import org.example.model.PieceJointe;
import org.example.services.*;
import org.example.utils.EmailService;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur de l'espace RH/Admin — Gestion des candidatures avec dashboard,
 * filtrage, export PDF, détection de langue et validation email.
 */
public class RhCandidatureController {

    @FXML private TableView<Candidature> tvCandidatures;
    @FXML private TableColumn<Candidature, Integer> colId, colUserId, colOffreId;
    @FXML private TableColumn<Candidature, String> colCv, colStatut, colMotivation, colNomCandidat, colTitreOffre;
    @FXML private TableColumn<Candidature, Object> colDate;
    @FXML private TableColumn<Candidature, Void> colActions;

    @FXML private Label statTotal, statAccept, statAttente, statEnCours, statRefuse, lblLocation, lblMsg;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField tfSearch;
    @FXML private VBox chartContainer;

    private final CandidatureService service = new CandidatureService();
    private final PieceJointeService pjService = new PieceJointeService();
    private final ObservableList<Candidature> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cbStatut.setItems(FXCollections.observableArrayList("EN_ATTENTE", "EN_COURS", "ACCEPTE", "REFUSE"));

        // Colonnes principales
        colId.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getId()));
        colStatut.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatut()));
        colDate.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getDatePostulation()));
        colCv.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCvUrl()));
        colMotivation.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getMotivation()));

        // Colonnes avec jointures
        if (colUserId != null) {
            colUserId.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getUserId()));
        }
        if (colOffreId != null) {
            colOffreId.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getOffreId()));
        }
        if (colNomCandidat != null) {
            colNomCandidat.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getNomCandidat() != null ? cell.getValue().getNomCandidat() : "User #" + cell.getValue().getUserId()));
            // Avatar API — affiche un cercle avec initiales + image API en arrière-plan
            colNomCandidat.setCellFactory(col -> new TableCell<>() {
                private final javafx.scene.layout.StackPane avatarPane = new javafx.scene.layout.StackPane();
                private final Circle avatarCircle = new Circle(16);
                private final javafx.scene.text.Text initialsText = new javafx.scene.text.Text();
                private final ImageView avatarImg = new ImageView();
                private final Label nameLabel = new Label();
                private final HBox container = new HBox(10);
                {
                    // Cercle d'initiales (fallback toujours visible)
                    avatarCircle.setFill(javafx.scene.paint.Color.web("#6c5ce7"));
                    initialsText.setFill(javafx.scene.paint.Color.WHITE);
                    initialsText.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

                    // Image API par-dessus le cercle
                    avatarImg.setFitWidth(32);
                    avatarImg.setFitHeight(32);
                    avatarImg.setPreserveRatio(true);
                    avatarImg.setSmooth(true);
                    Circle imgClip = new Circle(16, 16, 16);
                    avatarImg.setClip(imgClip);

                    avatarPane.getChildren().addAll(avatarCircle, initialsText, avatarImg);
                    avatarPane.setMinSize(34, 34);
                    avatarPane.setMaxSize(34, 34);

                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436; -fx-font-size: 13;");
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.getChildren().addAll(avatarPane, nameLabel);
                }

                @Override
                protected void updateItem(String name, boolean empty) {
                    super.updateItem(name, empty);
                    if (empty || name == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        nameLabel.setText(name);

                        // Initiales colorées
                        String initials = getInitials(name);
                        initialsText.setText(initials);
                        avatarCircle.setFill(getAvatarColor(name));

                        // Charger avatar API en arrière-plan
                        String encoded = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
                        String avatarUrl = "https://ui-avatars.com/api/?name=" + encoded
                                + "&size=64&rounded=true&background="
                                + colorToHex(getAvatarColor(name)) + "&color=fff&bold=true&format=png";
                        try {
                            Image img = new Image(avatarUrl, 32, 32, true, true, true);
                            avatarImg.setImage(img);
                            // Si l'image charge avec succès, masquer les initiales
                            img.progressProperty().addListener((obs, oldP, newP) -> {
                                if (newP.doubleValue() >= 1.0 && !img.isError()) {
                                    initialsText.setVisible(false);
                                    avatarCircle.setVisible(false);
                                }
                            });
                            // Si erreur, garder les initiales
                            img.errorProperty().addListener((obs, oldE, newE) -> {
                                if (newE) {
                                    initialsText.setVisible(true);
                                    avatarCircle.setVisible(true);
                                    avatarImg.setImage(null);
                                }
                            });
                        } catch (Exception e) {
                            avatarImg.setImage(null);
                            initialsText.setVisible(true);
                            avatarCircle.setVisible(true);
                        }

                        setGraphic(container);
                        setText(null);
                    }
                }
            });
        }
        if (colTitreOffre != null) {
            colTitreOffre.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getTitreOffre() != null ? cell.getValue().getTitreOffre() : "Offre #" + cell.getValue().getOffreId()));
        }

        // Coloration statut
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "ACCEPTE" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "EN_ATTENTE" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    case "EN_COURS" -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    case "REFUSE" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });

        // Recherche / filtre
        FilteredList<Candidature> filteredData = new FilteredList<>(data, p -> true);
        tfSearch.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(c -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String low = newValue.toLowerCase();
                return String.valueOf(c.getId()).contains(low)
                        || (c.getStatut() != null && c.getStatut().toLowerCase().contains(low))
                        || (c.getEmail() != null && c.getEmail().toLowerCase().contains(low))
                        || (c.getNomCandidat() != null && c.getNomCandidat().toLowerCase().contains(low))
                        || (c.getTitreOffre() != null && c.getTitreOffre().toLowerCase().contains(low));
            });
        });

        SortedList<Candidature> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tvCandidatures.comparatorProperty());
        tvCandidatures.setItems(sortedData);

        // Bouton actions (voir PJ)
        setupActionsColumn();

        // Sélection → analyse API
        tvCandidatures.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                cbStatut.setValue(newSel.getStatut());
                lblMsg.setText("🔍 Analyse en cours...");

                new Thread(() -> {
                    try {
                        String finalLang = newSel.getLangue();
                        if (finalLang == null || finalLang.equals("INCONNU")) {
                            finalLang = LanguageDetectionService.detectLanguage(newSel.getMotivation());
                            service.updateLangue(newSel.getId(), finalLang);
                            newSel.setLangue(finalLang);
                        }

                        String emailStatus = "Email Absent";
                        String emailIcon = "⚠️";
                        if (newSel.getEmail() != null && !newSel.getEmail().isEmpty()) {
                            emailStatus = EmailValidationService.checkEmail(newSel.getEmail());
                            emailIcon = emailStatus.equalsIgnoreCase("DELIVERABLE") ? "✅ Valide" : "❌ Suspect";
                        }

                        String finalResult = String.format("📌 Langue: %s | %s (%s)",
                                finalLang, emailIcon, (newSel.getEmail() == null ? "Non saisi" : newSel.getEmail()));

                        Platform.runLater(() -> lblMsg.setText(finalResult));
                    } catch (Exception e) {
                        Platform.runLater(() -> lblMsg.setText("❌ Erreur API : " + e.getMessage()));
                    }
                }).start();
            }
        });

        // Localisation
        new Thread(() -> {
            String city = LocationDetectionService.getCityLocation();
            Platform.runLater(() -> {
                if (lblLocation != null) lblLocation.setText("📍 " + city);
            });
        }).start();

        refresh();
    }

    private void setupActionsColumn() {
        if (colActions == null) return;
        colActions.setCellFactory(p -> new TableCell<>() {
            private final Button btn = new Button("📄 Voir PJ");
            {
                btn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 10;");
                btn.setOnAction(e -> handleViewPieces(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setAlignment(Pos.CENTER);
            }
        });
    }

    private void handleViewPieces(Candidature c) {
        try {
            List<PieceJointe> pieces = pjService.getByCandidature(c.getId());
            if (pieces.isEmpty()) {
                lblMsg.setText("ℹ️ Aucun document pour la candidature #" + c.getId());
                return;
            }
            StringBuilder sb = new StringBuilder("📂 Documents de la candidature #" + c.getId() + " :\n\n");
            for (PieceJointe pj : pieces) {
                sb.append("• ").append(pj.getTypeDoc()).append(" : ").append(pj.getTitre())
                        .append(" (").append(pj.getUrl()).append(")\n");
            }
            Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString());
            a.setTitle("Dossier Candidature #" + c.getId());
            a.setHeaderText("Pièces jointes (" + pieces.size() + ")");
            a.getDialogPane().setMinWidth(500);
            a.show();
        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur chargement PJ.");
        }
    }

    private void updateDashboard() {
        long total = data.size();
        long accepte = data.stream().filter(c -> "ACCEPTE".equals(c.getStatut())).count();
        long attente = data.stream().filter(c -> "EN_ATTENTE".equals(c.getStatut())).count();
        long enCours = data.stream().filter(c -> "EN_COURS".equals(c.getStatut())).count();
        long refuse = data.stream().filter(c -> "REFUSE".equals(c.getStatut())).count();

        statTotal.setText(String.valueOf(total));
        statAccept.setText(String.valueOf(accepte));
        statAttente.setText(String.valueOf(attente));
        statEnCours.setText(String.valueOf(enCours));
        statRefuse.setText(String.valueOf(refuse));

        // Pie Chart
        if (chartContainer != null) {
            chartContainer.getChildren().clear();
            if (total > 0) {
                PieChart pieChart = new PieChart(FXCollections.observableArrayList(
                        new PieChart.Data("Accepté (" + accepte + ")", accepte),
                        new PieChart.Data("En Attente (" + attente + ")", attente),
                        new PieChart.Data("En Cours (" + enCours + ")", enCours),
                        new PieChart.Data("Refusé (" + refuse + ")", refuse)
                ));
                pieChart.setTitle("Répartition des Statuts");
                pieChart.setLabelsVisible(true);
                pieChart.setLegendVisible(true);
                pieChart.setPrefHeight(250);
                pieChart.setMaxHeight(250);
                chartContainer.getChildren().add(pieChart);

                Platform.runLater(() -> {
                    try {
                        String[] colors = {"#27ae60", "#f39c12", "#3498db", "#e74c3c"};
                        ObservableList<PieChart.Data> chartData = pieChart.getData();
                        for (int i = 0; i < chartData.size(); i++) {
                            Node node = chartData.get(i).getNode();
                            if (node != null) {
                                node.setStyle("-fx-pie-color: " + colors[i] + ";");
                            }
                        }
                    } catch (Exception ignored) {}
                });
            }
        }
    }

    @FXML
    private void handleUpdateStatut() {
        Candidature selected = tvCandidatures.getSelectionModel().getSelectedItem();
        String newStatut = cbStatut.getValue();
        if (selected == null) { lblMsg.setText("⚠️ Sélectionnez une candidature."); return; }
        if (newStatut == null) { lblMsg.setText("⚠️ Sélectionnez un statut."); return; }

        try {
            service.updateStatut(selected.getId(), newStatut);

            // Notification par email en arrière-plan
            if (selected.getEmail() != null && !selected.getEmail().isEmpty()) {
                String email = selected.getEmail();
                String finalStatut = newStatut;
                new Thread(() -> {
                    try {
                        EmailService.sendStatutNotification(email, selected.getId(), finalStatut);
                    } catch (Exception ignored) {}
                }).start();
            }

            lblMsg.setText("✅ Statut mis à jour : " + newStatut);
            refresh();
        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur SQL : " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Candidature selected = tvCandidatures.getSelectionModel().getSelectedItem();
        if (selected == null) { lblMsg.setText("⚠️ Sélectionnez une candidature."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la candidature #" + selected.getId() + " ?\nCela supprimera aussi les pièces jointes associées.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation de suppression");
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    service.delete(selected.getId());
                    lblMsg.setText("🗑 Candidature supprimée.");
                    refresh();
                } catch (SQLException e) {
                    lblMsg.setText("❌ Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleExport() {
        if (data.isEmpty()) { lblMsg.setText("⚠️ Aucune donnée à exporter."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.setInitialFileName("Export_Candidatures_TalentFlow.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File f = fc.showSaveDialog(tvCandidatures.getScene().getWindow());
        if (f != null) {
            try {
                Document document = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(document, new FileOutputStream(f));
                document.open();

                Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(108, 92, 231));
                Paragraph title = new Paragraph("TalentFlow — Export Candidatures", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(8);
                document.add(title);

                Font dateFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
                Paragraph date = new Paragraph("Généré le " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), dateFont);
                date.setAlignment(Element.ALIGN_CENTER);
                date.setSpacingAfter(20);
                document.add(date);

                // Stats
                Font statsFont = new Font(Font.HELVETICA, 11, Font.BOLD);
                long accepte = data.stream().filter(c -> "ACCEPTE".equals(c.getStatut())).count();
                long attente = data.stream().filter(c -> "EN_ATTENTE".equals(c.getStatut())).count();
                long enCours = data.stream().filter(c -> "EN_COURS".equals(c.getStatut())).count();
                long refuse = data.stream().filter(c -> "REFUSE".equals(c.getStatut())).count();
                Paragraph stats = new Paragraph(String.format(
                        "Total: %d  |  Accepté: %d  |  En Attente: %d  |  En Cours: %d  |  Refusé: %d",
                        data.size(), accepte, attente, enCours, refuse), statsFont);
                stats.setAlignment(Element.ALIGN_CENTER);
                stats.setSpacingAfter(15);
                document.add(stats);

                // Table
                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1, 2.5f, 2.5f, 2, 3, 2.5f, 4});

                Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
                Color headerBg = new Color(108, 92, 231);
                String[] headers = {"ID", "Candidat", "Offre", "Statut", "Email", "Date", "Motivation"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                    cell.setBackgroundColor(headerBg);
                    cell.setPadding(8);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }

                Font cellFont = new Font(Font.HELVETICA, 9);
                Color altBg = new Color(245, 248, 255);
                int row = 0;
                for (Candidature c : data) {
                    Color bg = (row % 2 == 0) ? Color.WHITE : altBg;
                    addCell(table, String.valueOf(c.getId()), cellFont, bg);
                    addCell(table, c.getNomCandidat() != null ? c.getNomCandidat() : "User #" + c.getUserId(), cellFont, bg);
                    addCell(table, c.getTitreOffre() != null ? c.getTitreOffre() : "Offre #" + c.getOffreId(), cellFont, bg);
                    addCell(table, c.getStatut(), cellFont, bg);
                    addCell(table, c.getEmail() != null ? c.getEmail() : "-", cellFont, bg);
                    String dateStr = "-";
                    if (c.getDatePostulation() != null) {
                        String raw = c.getDatePostulation().toString();
                        dateStr = raw.length() > 16 ? raw.substring(0, 16) : raw;
                    }
                    addCell(table, dateStr, cellFont, bg);
                    addCell(table, c.getMotivation() != null ? c.getMotivation() : "-", cellFont, bg);
                    row++;
                }

                document.add(table);

                Paragraph footer = new Paragraph("\n© TalentFlow — Rapport généré automatiquement", dateFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                document.add(footer);

                document.close();
                lblMsg.setText("📥 PDF exporté avec succès !");
            } catch (Exception e) {
                lblMsg.setText("❌ Erreur export PDF : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void addCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        table.addCell(cell);
    }

    @FXML
    private void handleRefresh() { refresh(); }

    private void refresh() {
        try {
            data.setAll(service.getAll());
            updateDashboard();
            lblMsg.setText("✅ " + data.size() + " candidature(s) chargée(s).");
        } catch (SQLException e) {
            lblMsg.setText("❌ Erreur BDD : " + e.getMessage());
        }
    }

    @FXML
    private void sortByStrength() {
        data.sort((c1, c2) -> {
            try {
                int size1 = pjService.getByCandidature(c1.getId()).size();
                int size2 = pjService.getByCandidature(c2.getId()).size();
                return Integer.compare(size2, size1);
            } catch (SQLException e) {
                return 0;
            }
        });
        lblMsg.setText("🔥 Liste triée par force du dossier (nombre de PJ) !");
    }

    // ─────── Avatar Helpers ───────

    /** Extrait les initiales d'un nom (ex: "Jean Dupont" → "JD") */
    private static String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    /** Génère une couleur unique et agréable basée sur le nom */
    private static javafx.scene.paint.Color getAvatarColor(String name) {
        if (name == null) return javafx.scene.paint.Color.web("#6c5ce7");
        String[] palette = {
            "#6c5ce7", "#00b894", "#e17055", "#0984e3", "#d63031",
            "#00cec9", "#e84393", "#fdcb6e", "#6ab04c", "#eb4d4b",
            "#7ed6df", "#22a6b3", "#be2edd", "#f9ca24", "#30336b"
        };
        int idx = Math.abs(name.hashCode()) % palette.length;
        return javafx.scene.paint.Color.web(palette[idx]);
    }

    /** Convertit un Color JavaFX en hex sans le # (ex: "6c5ce7") */
    private static String colorToHex(javafx.scene.paint.Color c) {
        return String.format("%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}
