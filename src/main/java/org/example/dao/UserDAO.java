package org.example.dao;

import org.example.model.User;
import org.example.utils.MyConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    // Récupération de l'instance de connexion unique (Singleton)
    private Connection conn = MyConnection.getInstance().getConnection();

    // 1. CREATE : Ajouter un utilisateur
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
            System.out.println("Utilisateur inséré avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur SQL (create) : " + e.getMessage());
        }
    }

    // 2. READ ALL : Lister tous les utilisateurs
    public List<User> readAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
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
            System.err.println("Erreur SQL (readAll) : " + e.getMessage());
        }
        return users;
    }

    // 3. UPDATE : Modifier un utilisateur existant
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

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Utilisateur ID " + user.getId() + " mis à jour avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL (update) : " + e.getMessage());
        }
    }

    // 4. DELETE : Supprimer un utilisateur
    public void delete(int id) {
        String sql = "DELETE FROM user WHERE id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Utilisateur ID " + id + " supprimé avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL (delete) : " + e.getMessage());
        }
    }

    // 5. READ BY ID (Optionnel mais recommandé)
    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
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
        } catch (SQLException e) {
            System.err.println("Erreur SQL (findById) : " + e.getMessage());
        }
        return null;
    }
}