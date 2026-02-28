package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DB {

    private static DB instance;
    private Connection connection;

    private final String URL = "jdbc:mysql://localhost:3306/talent_flow_db?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = ""; // Mets ton mot de passe si nécessaire

    private DB() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            connection.setAutoCommit(true);
            System.out.println("Connected to talent_flow_db");
            createTablesIfNeeded();
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTablesIfNeeded() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS offre (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  titre VARCHAR(255) NOT NULL," +
                "  description TEXT," +
                "  localisation VARCHAR(255)," +
                "  type_contrat VARCHAR(50) DEFAULT 'CDI'," +
                "  mode_travail VARCHAR(50) DEFAULT 'ON_SITE'," +
                "  salaire_min DOUBLE DEFAULT 0," +
                "  salaire_max DOUBLE DEFAULT 0," +
                "  is_active BOOLEAN DEFAULT TRUE," +
                "  statut VARCHAR(50) DEFAULT 'PUBLISHED'" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS avantage (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  nom VARCHAR(255) NOT NULL," +
                "  description TEXT," +
                "  type VARCHAR(50) DEFAULT 'AUTRE'," +
                "  offre_id INT NOT NULL," +
                "  FOREIGN KEY (offre_id) REFERENCES offre(id) ON DELETE CASCADE" +
                ")"
            );
            System.out.println("Tables verified/created successfully");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static DB getInstance() {
        if (instance == null) {
            instance = new DB();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(3)) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                connection.setAutoCommit(true);
                System.out.println("Reconnected to talent_flow_db");
            }
        } catch (SQLException e) {
            System.err.println("Failed to reconnect: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }
}
