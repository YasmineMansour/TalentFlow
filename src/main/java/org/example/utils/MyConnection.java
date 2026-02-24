package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private final String url = "jdbc:mysql://localhost:3306/talent_flow_db?useSSL=false&serverTimezone=UTC&autoReconnect=true";
    private final String login = "root";
    private final String pwd = "";
    private Connection connection;
    private static MyConnection instance;

    // Constructeur privé (Singleton)
    private MyConnection() {
        connect();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(url, login, pwd);
            System.out.println("✅ Connexion à la base de données réussie !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("🔄 Reconnexion à la base de données...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification de la connexion : " + e.getMessage());
            connect();
        }
        return connection;
    }

    public static synchronized MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }
}