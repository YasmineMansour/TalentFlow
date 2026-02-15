package Services;

import Entites.Message;
import Entites.User;
import Utils.MyBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private Connection conn = MyBD.getInstance().getConn();

    public void sendMessage(int senderId, int receiverId, String content) throws SQLException {
        if (content == null || content.trim().isEmpty()) return;
        String query = "INSERT INTO messages (sender_id, receiver_id, content, sent_at) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    public void modifierMessage(int messageId, String newContent) throws SQLException {
        if (newContent == null || newContent.trim().isEmpty()) return;
        String query = "UPDATE messages SET content = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, newContent);
            ps.setInt(2, messageId);
            ps.executeUpdate();
        }
    }

    public void supprimerMessage(int messageId) throws SQLException {
        String query = "DELETE FROM messages WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        }
    }

    public List<Message> getChatHistory(int user1Id, int user2Id) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT * FROM messages WHERE (sender_id = ? AND receiver_id = ?) " +
                "OR (sender_id = ? AND receiver_id = ?) ORDER BY sent_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, user1Id); ps.setInt(2, user2Id);
            ps.setInt(3, user2Id); ps.setInt(4, user1Id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(new Message(rs.getInt("id"), rs.getInt("sender_id"),
                        rs.getInt("receiver_id"), rs.getString("content"), rs.getTimestamp("sent_at")));
            }
        }
        return messages;
    }

    public List<User> getChatPartners(int currentUserId) throws SQLException {
        List<User> partners = new ArrayList<>();
        String query = "SELECT DISTINCT u.id, u.username FROM users u " +
                "JOIN messages m ON (u.id = m.sender_id OR u.id = m.receiver_id) " +
                "WHERE (m.sender_id = ? OR m.receiver_id = ?) AND u.id != ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, currentUserId);
            ps.setInt(3, currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                partners.add(u);
            }
        }
        return partners;
    }
}