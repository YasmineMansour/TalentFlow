package org.example.GUI;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.dao.UserDAO;
import org.example.model.User;

import java.util.List;

public class DashboardHomeController {

    @FXML private Label greetingLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminCountLabel;
    @FXML private Label rhCountLabel;
    @FXML private Label candidatCountLabel;
    @FXML private PieChart rolePieChart;
    @FXML private VBox recentUsersBox;

    private UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // Greeting
        User currentUser = UserSession.getInstance();
        if (currentUser != null && greetingLabel != null) {
            greetingLabel.setText("Bonjour, " + currentUser.getPrenom() + " !");
        }

        loadStatistics();
        loadPieChart();
        loadRecentUsers();
    }

    private void loadStatistics() {
        int total = userDAO.count();
        int admins = userDAO.countByRole("ADMIN");
        int rh = userDAO.countByRole("RH");
        int candidats = userDAO.countByRole("CANDIDAT");

        totalUsersLabel.setText(String.valueOf(total));
        adminCountLabel.setText(String.valueOf(admins));
        rhCountLabel.setText(String.valueOf(rh));
        candidatCountLabel.setText(String.valueOf(candidats));
    }

    private void loadPieChart() {
        int admins = userDAO.countByRole("ADMIN");
        int rh = userDAO.countByRole("RH");
        int candidats = userDAO.countByRole("CANDIDAT");

        rolePieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Admin (" + admins + ")", admins),
                new PieChart.Data("RH (" + rh + ")", rh),
                new PieChart.Data("Candidat (" + candidats + ")", candidats)
        ));
        rolePieChart.setStartAngle(90);
    }

    private void loadRecentUsers() {
        if (recentUsersBox == null) return;
        recentUsersBox.getChildren().clear();

        List<User> recent = userDAO.getRecentUsers(5);

        if (recent.isEmpty()) {
            Label empty = new Label("Aucun utilisateur inscrit.");
            empty.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 13px;");
            recentUsersBox.getChildren().add(empty);
            return;
        }

        for (User user : recent) {
            HBox row = new HBox(12);
            row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 12 16;");
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Avatar circle with initials
            String initials = "";
            if (user.getPrenom() != null && !user.getPrenom().isEmpty())
                initials += user.getPrenom().charAt(0);
            if (user.getNom() != null && !user.getNom().isEmpty())
                initials += user.getNom().charAt(0);

            Label avatar = new Label(initials.toUpperCase());
            avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #6c5ce7, #a29bfe); " +
                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; " +
                    "-fx-min-width: 38; -fx-min-height: 38; -fx-max-width: 38; -fx-max-height: 38; " +
                    "-fx-background-radius: 19; -fx-alignment: center;");

            // Name + email
            VBox info = new VBox(2);
            Label nameLabel = new Label(user.getPrenom() + " " + user.getNom());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436; -fx-font-size: 13px;");
            Label emailLabel = new Label(user.getEmail());
            emailLabel.setStyle("-fx-text-fill: #636e72; -fx-font-size: 11px;");
            info.getChildren().addAll(nameLabel, emailLabel);
            HBox.setHgrow(info, Priority.ALWAYS);

            // Role badge
            Label roleBadge = new Label(user.getRole());
            String badgeColor = switch (user.getRole().toUpperCase()) {
                case "ADMIN" -> "#6c5ce7";
                case "RH" -> "#00b894";
                default -> "#fdcb6e";
            };
            String textColor = user.getRole().equalsIgnoreCase("CANDIDAT") ? "#2d3436" : "white";
            roleBadge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: " + textColor + "; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 12;");

            row.getChildren().addAll(avatar, info, roleBadge);
            recentUsersBox.getChildren().add(row);
        }
    }
}
