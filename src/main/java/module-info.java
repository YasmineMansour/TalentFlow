module org.example.talentflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.graphics;
    requires mysql.connector.j; // Ajout important pour la base de données

    // Autorise JavaFX à accéder aux contrôleurs
    opens org.example.GUI to javafx.fxml;

    // Autorise JavaFX à lire les FXML qui sont à la racine du dossier org.example
    opens org.example to javafx.fxml;

    // Autorise le TableView à lire les attributs (nom, email...) de tes modèles
    opens org.example.model to javafx.base;

    // Exportations pour rendre les classes visibles au moteur JavaFX
    exports org.example;
    exports org.example.GUI;
    exports org.example.model;
    exports org.example.dao;
}