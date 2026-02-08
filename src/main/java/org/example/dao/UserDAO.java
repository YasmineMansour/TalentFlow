package org.example.dao;

import org.example.model.User;
import org.example.utils.MyConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    // Connexion à la base de données via ton utilitaire MyConnection
    private Connection conn = MyConnection.getInstance().getConnection();

    // --- MÉTHODE LOGIN (Pour LoginController) ---
    // Résout l'erreur : cannot find symbol method login
    public User login(String email, String password) {
        String sql = "SELECT * FROM user WHERE email = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Retourne un objet User complet trouvé en base
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
        } catch (SQLException e) {
            System.err.println("Erreur lors du login : " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Retourne null si aucun utilisateur n'est trouvé
    }

    // --- MÉTHODE CREATE (Pour handleInsert) ---
    // Résout l'erreur : cannot resolve method 'create'
    public void create(User user) {
        String sql = "INSERT INTO user (nom, prenom, email, password, role, telephone) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, user.getRole());
            pstmt.setString(6, user.getTelephone());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- MÉTHODE READ ALL (Pour le TableView) ---
    // Résout l'erreur : cannot resolve method 'readAll'
    public List<User> readAll() {
        List<User> users = new ArrayList<>();
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
    // Résout l'erreur : cannot resolve method 'update'
    public void update(User user) {
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
    // Résout l'erreur : cannot resolve method 'delete'
    public void delete(int id) {
        String sql = "DELETE FROM user WHERE id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}