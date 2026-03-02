package org.example.services;

import org.example.model.Post;
import org.example.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour les posts du forum.
 * Utilise la table posts de talent_flow_db.
 */
public class PostService {
    private Connection getConn() {
        return MyConnection.getInstance().getConnection();
    }

    /** Ajouter un nouveau post */
    public void ajouter(Post p) throws SQLException {
        String req = "INSERT INTO posts (title, content, author_id, authorName, authorRole, upvotes, image_path) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = getConn().prepareStatement(req)) {
            pst.setString(1, p.getTitle());
            pst.setString(2, p.getContent());
            pst.setInt(3, p.getAuthorId());
            pst.setString(4, p.getAuthorName());
            pst.setString(5, p.getAuthorRole());
            pst.setInt(6, p.getUpvotes());
            pst.setString(7, p.getImagePath());
            pst.executeUpdate();
        }
    }

    /** Modifier un post existant */
    public void modifier(Post p) throws SQLException {
        if (p.getTitle().trim().isEmpty() || p.getContent().trim().isEmpty()) {
            throw new SQLException("Le titre et le contenu ne peuvent pas être vides.");
        }
        String req = "UPDATE posts SET title=?, content=?, image_path=? WHERE id=?";
        try (PreparedStatement pst = getConn().prepareStatement(req)) {
            pst.setString(1, p.getTitle());
            pst.setString(2, p.getContent());
            pst.setString(3, p.getImagePath());
            pst.setInt(4, p.getId());
            pst.executeUpdate();
        }
    }

    /** Supprimer un post par ID */
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM posts WHERE id=?";
        try (PreparedStatement pst = getConn().prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    /** Récupérer tous les posts (du plus récent au plus ancien) */
    public List<Post> afficher() throws SQLException {
        List<Post> list = new ArrayList<>();
        String req = "SELECT * FROM posts ORDER BY id DESC";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Post p = new Post(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("authorName"),
                        rs.getString("authorRole"),
                        rs.getInt("upvotes"),
                        rs.getString("image_path")
                );
                p.setAuthorId(rs.getInt("author_id"));
                list.add(p);
            }
        }
        return list;
    }

    /** Système de vote (upvote/downvote) avec gestion toggle */
    public void updateVotes(int postId, int userId, int newVoteType) throws SQLException {
        Connection conn = getConn();
        String checkReq = "SELECT vote_type FROM post_votes WHERE userId = ? AND postId = ?";
        String insertReq = "INSERT INTO post_votes (userId, postId, vote_type) VALUES (?, ?, ?)";
        String updateVoteReq = "UPDATE post_votes SET vote_type = ? WHERE userId = ? AND postId = ?";
        String updatePostReq = "UPDATE posts SET upvotes = upvotes + ? WHERE id = ?";
        String deleteVoteReq = "DELETE FROM post_votes WHERE userId = ? AND postId = ?";

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement psCheck = conn.prepareStatement(checkReq)) {
                psCheck.setInt(1, userId);
                psCheck.setInt(2, postId);
                ResultSet rs = psCheck.executeQuery();

                if (rs.next()) {
                    int existingVote = rs.getInt("vote_type");

                    if (existingVote == newVoteType) {
                        // Même vote → retirer
                        try (PreparedStatement psDel = conn.prepareStatement(deleteVoteReq)) {
                            psDel.setInt(1, userId);
                            psDel.setInt(2, postId);
                            psDel.executeUpdate();
                        }
                        try (PreparedStatement psPost = conn.prepareStatement(updatePostReq)) {
                            psPost.setInt(1, -existingVote);
                            psPost.setInt(2, postId);
                            psPost.executeUpdate();
                        }
                    } else {
                        // Changement de vote
                        try (PreparedStatement psUpdVote = conn.prepareStatement(updateVoteReq)) {
                            psUpdVote.setInt(1, newVoteType);
                            psUpdVote.setInt(2, userId);
                            psUpdVote.setInt(3, postId);
                            psUpdVote.executeUpdate();
                        }
                        try (PreparedStatement psPost = conn.prepareStatement(updatePostReq)) {
                            psPost.setInt(1, newVoteType - existingVote);
                            psPost.setInt(2, postId);
                            psPost.executeUpdate();
                        }
                    }
                } else {
                    // Premier vote
                    try (PreparedStatement psIns = conn.prepareStatement(insertReq)) {
                        psIns.setInt(1, userId);
                        psIns.setInt(2, postId);
                        psIns.setInt(3, newVoteType);
                        psIns.executeUpdate();
                    }
                    try (PreparedStatement psPost = conn.prepareStatement(updatePostReq)) {
                        psPost.setInt(1, newVoteType);
                        psPost.setInt(2, postId);
                        psPost.executeUpdate();
                    }
                }
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
