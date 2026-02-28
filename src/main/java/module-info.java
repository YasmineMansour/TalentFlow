module org.example.talentflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires javafx.graphics;
    requires mysql.connector.j;     // Base de données MySQL
    requires jbcrypt;               // Hachage des mots de passe (BCrypt)
    requires jakarta.mail;          // Envoi d'emails (Jakarta Mail)
    requires java.net.http;         // Appels HTTP pour Twilio SMS API
    requires com.github.librepdf.openpdf;  // Export PDF (OpenPDF)
    requires java.desktop;          // java.awt.Color pour OpenPDF + java.awt.Desktop pour Google OAuth
    requires jdk.httpserver;         // Serveur HTTP local pour callback Google OAuth

    // Google Calendar API
    requires google.api.client;
    requires com.google.api.services.calendar;
    requires com.google.api.client.extensions.jetty.auth;
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.json.gson;
    requires com.google.api.client;
    requires com.google.api.client.auth;
    requires com.google.gson;

    // Autorise JavaFX à accéder aux contrôleurs
    opens org.example.GUI to javafx.fxml;

    // Autorise JavaFX à lire les FXML qui sont à la racine du dossier org.example
    opens org.example to javafx.fxml;

    // Autorise le TableView à lire les attributs (nom, email...) de tes modèles
    opens org.example.model to javafx.base;

    // Services (pour FXML et JavaFX)
    opens org.example.services to javafx.fxml;

    // Exportations pour rendre les classes visibles au moteur JavaFX
    exports org.example;
    exports org.example.GUI;
    exports org.example.model;
    exports org.example.dao;
    exports org.example.utils;
    exports org.example.services;
}