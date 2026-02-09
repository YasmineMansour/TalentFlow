module org.example.talentflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.graphics; // Nécessaire pour lancer l'Application

    // Autorise JavaFX à accéder aux contrôleurs pour charger les fichiers FXML
    opens org.example.GUI to javafx.fxml;

    // Autorise JavaFX (le module graphics) à accéder à ta classe App.java
    exports org.example;

    // Autorise le TableView à lire les données de tes modèles
    opens org.example.model to javafx.base;

    // Exports nécessaires pour les autres packages si besoin
    exports org.example.GUI;
    exports org.example.model;
}