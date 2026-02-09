package org.example.dao;

import org.example.model.User;
import org.example.utils.MyConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private Connection conn;

    public UserDAO() {
        // Initialisation sécurisée dans le constructeur
        try {
            this.conn = MyConnection.getInstance().getConnection();
            if (this.conn == null) {
                System.err.println("❌ UserDAO : La connexion est nulle. Vérifiez XAMPP / MySQL.");
            }
        } catch (Exception e) {
            System.err.println("❌ UserDAO : Erreur lors de l'accès à MyConnection : " + e.getMessage());
        }
    }

    // --- MÉTHODE LOGIN ---
    public User login(String email, String password) {
        if (conn == null) return null;
        String sql = "SELECT * FROM user WHERE email = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("telephone")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- MÉTHODE CREATE ---
    public void create(User user) {
        if (conn == null) {
            System.err.println("❌ Impossible de créer : Connexion BDD inexistante.");
            return;
        }
        String sql = "INSERT INTO user (nom, prenom, email, password, role, telephone) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, user.getRole());
            pstmt.setString(6, user.getTelephone());
            pstmt.executeUpdate();
            System.out.println("✅ Utilisateur inséré avec succès.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL lors de l'insertion : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- MÉTHODE READ ALL ---
    public List<User> readAll() {
        List<User> users = new ArrayList<>();
        if (conn == null) return users;
        String sql = "SELECT * FROM user";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("telephone")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // --- MÉTHODE UPDATE ---
    public void update(User user) {
        if (conn == null) return;
        String sql = "UPDATE user SET nom=?, prenom=?, email=?, password=?, role=?, telephone=? WHERE id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, user.getRole());
            pstmt.setString(6, user.getTelephone());
            pstmt.setInt(7, user.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- MÉTHODE DELETE ---
    public void delete(int id) {
        if (conn == null) return;
        String sql = "DELETE FROM user WHERE id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}