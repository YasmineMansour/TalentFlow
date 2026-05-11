package org.example.services;

import org.example.model.Message;
import org.example.model.User;
import org.example.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de messagerie privée du forum.
 * Utilise la table messages de talent_flow_db.
 * Jointure avec la table user pour récupérer les partenaires de chat.
 *
 * IMPORTANT : Symfony stocke les rôles dans la colonne `roles` (JSON),
 * pas dans une colonne `role`. On utilise parseRole() pour extraire le rôle.
 */
public class MessageService {
    private Connection getConn() {
        return MyConnection.getInstance().getConnection();
    }

    /** Envoyer un message */
    public void sendMessage(int senderId, int receiverId, String content) throws SQLException {
        if (content == null || content.trim().isEmpty()) return;
        String query = "INSERT INTO messages (senderId, receiverId, content, sentAt) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = getConn().prepareStatement(query)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    /** Modifier un message */
    public void modifierMessage(int messageId, String newContent) throws SQLException {
        if (newContent == null || newContent.trim().isEmpty()) return;
        String query = "UPDATE messages SET content = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(query)) {
            ps.setString(1, newContent);
            ps.setInt(2, messageId);
            ps.executeUpdate();
        }
    }

    /** Supprimer un message */
    public void supprimerMessage(int messageId) throws SQLException {
        String query = "DELETE FROM messages WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(query)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        }
    }

    /** Récupérer l'historique de chat entre deux utilisateurs */
    public List<Message> getChatHistory(int user1Id, int user2Id) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT * FROM messages WHERE (senderId = ? AND receiverId = ?) " +
                "OR (senderId = ? AND receiverId = ?) ORDER BY sentAt ASC";
        try (PreparedStatement ps = getConn().prepareStatement(query)) {
            ps.setInt(1, user1Id);
            ps.setInt(2, user2Id);
            ps.setInt(3, user2Id);
            ps.setInt(4, user1Id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message(
                            rs.getInt("id"),
                            rs.getInt("senderId"),
                            rs.getInt("receiverId"),
                            rs.getString("content"),
                            rs.getTimestamp("sentAt")
                    ));
                }
            }
        }
        return messages;
    }

    /**
     * Récupérer les partenaires de chat de l'utilisateur courant.
     * Jointure avec la table user de TalentFlow.
     * Utilise `u.roles` (JSON Symfony) au lieu de `u.role`.
     */
    public List<User> getChatPartners(int currentUserId) throws SQLException {
        List<User> partners = new ArrayList<>();
        String query = "SELECT DISTINCT u.id, u.nom, u.prenom, u.email, u.roles " +
                "FROM user u " +
                "JOIN messages m ON (u.id = m.senderId OR u.id = m.receiverId) " +
                "WHERE (m.senderId = ? OR m.receiverId = ?) AND u.id != ?";
        try (PreparedStatement ps = getConn().prepareStatement(query)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, currentUserId);
            ps.setInt(3, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            "", // password non nécessaire
                            parseRole(rs.getString("roles")),
                            ""  // telephone non nécessaire
                    );
                    partners.add(u);
                }
            }
        }
        return partners;
    }

    /**
     * Rechercher un utilisateur par nom complet (prénom + nom).
     * Utilise `u.roles` (JSON Symfony) au lieu de `u.role`.
     */
    public User getUserByFullName(String fullName) throws SQLException {
        String query = "SELECT id, nom, prenom, email, roles FROM user WHERE CONCAT(prenom, ' ', nom) = ?";
        try (PreparedStatement ps = getConn().prepareStatement(query)) {
            ps.setString(1, fullName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            "",
                            parseRole(rs.getString("roles")),
                            ""
                    );
                }
            }
        }
        return null;
    }

    /**
     * Extrait le premier rôle depuis le JSON Symfony.
     * Symfony stocke les rôles sous forme JSON : ["ROLE_ADMIN","ROLE_USER"]
     */
    private String parseRole(String rolesJson) {
        if (rolesJson == null || rolesJson.isEmpty()) return "ROLE_USER";
        int start = rolesJson.indexOf('"');
        if (start < 0) return "ROLE_USER";
        int end = rolesJson.indexOf('"', start + 1);
        if (end < 0) return "ROLE_USER";
        return rolesJson.substring(start + 1, end);
    }
}