module org.example.talentflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;
    requires jdk.httpserver;
    requires javafx.graphics;
    requires mysql.connector.j;

    // Ouvrir les contrôleurs des offres/avantages à JavaFX FXML
    opens controllers to javafx.fxml;

    // Ouvrir les entités au TableView (PropertyValueFactory)
    opens entities to javafx.base, javafx.fxml;

    // Ouvrir les services et utilitaires
    opens services;
    opens utils;

    // Conserver l'accès pour le point d'entrée App.java
    opens org.example to javafx.fxml;

    // Exportations
    exports org.example;
    exports controllers;
    exports entities;
    exports services;
    exports utils;
}