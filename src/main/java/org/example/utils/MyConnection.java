package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    // Connexion à TA base de données talent_flow_db
    private final String url = "jdbc:mysql://localhost:3306/talent_flow_db?useSSL=false&serverTimezone=UTC";
    private final String login = "root";
    private final String pwd = "";
    private Connection connection;
    private static MyConnection instance;

    // Constructeur privé (Singleton)
    private MyConnection() {
        try {
            connection = DriverManager.getConnection(url, login, pwd);
            System.out.println("You have been successfully connected to the database !");
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public static synchronized MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }
}