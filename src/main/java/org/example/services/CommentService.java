package org.example.services;

import org.example.model.Comment;
import org.example.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour les commentaires du forum.
 * Utilise la table comments de talent_flow_db.
 */
public class CommentService {
    private Connection getConn() {
        return MyConnection.getInstance().getConnection();
    }

    /** Ajouter un commentaire à un post */
    public void ajouterCommentaire(int postId, int authorId, String authorName, String content) throws SQLException {
        String req = "INSERT INTO comments (postId, author_id, authorName, content) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = getConn().prepareStatement(req)) {
            pst.setInt(1, postId);
            pst.setInt(2, authorId);
            pst.setString(3, authorName);
            pst.setString(4, content);
            pst.executeUpdate();
        }
    }

    /** Récupérer tous les commentaires d'un post */
    public List<Comment> getCommentsByPost(int postId) throws SQLException {
        List<Comment> list = new ArrayList<>();
        String req = "SELECT * FROM comments WHERE postId = ? ORDER BY createdAt ASC";
        try (PreparedStatement pst = getConn().prepareStatement(req)) {
            pst.setInt(1, postId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Comment c = new Comment();
                    c.setId(rs.getInt("id"));
                    c.setPostId(rs.getInt("postId"));
                    c.setAuthorId(rs.getInt("author_id"));
                    c.setAuthorName(rs.getString("authorName"));
                    c.setContent(rs.getString("content"));
                    list.add(c);
                }
            }
        }
        return list;
    }

    /** Supprimer un commentaire */
    public void supprimerCommentaire(int commentId) throws SQLException {
        String req = "DELETE FROM comments WHERE id = ?";
        try (PreparedStatement pst = getConn().prepareStatement(req)) {
            pst.setInt(1, commentId);
            pst.executeUpdate();
        }
    }

    /** Modifier un commentaire */
    public void modifierCommentaire(int commentId, String newText) throws SQLException {
        String req = "UPDATE comments SET content = ? WHERE id = ?";
        try (PreparedStatement pst = getConn().prepareStatement(req)) {
            pst.setString(1, newText);
            pst.setInt(2, commentId);
            pst.executeUpdate();
        }
    }
}
