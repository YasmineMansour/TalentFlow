package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static final String URL =
            "jdbc:mysql://localhost:3306/module_entretien?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyDataBase instance;
    private Connection connection;

    private MyDataBase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion MySQL réussie");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur connexion MySQL", e);
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Erreur de reconnexion : " + e.getMessage());
        }
        return connection;
    }
}
