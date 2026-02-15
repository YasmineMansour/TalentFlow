package Services;

import Entites.Post;
import Utils.MyBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostService implements IntrefaceCRUD<Post> {
    Connection conn = MyBD.getInstance().getConn();

    @Override
    public void ajouter(Post p) throws SQLException {
        // Input Control: Basic Validation
        if (p.getTitle() == null || p.getTitle().trim().isEmpty() || p.getContent() == null || p.getContent().trim().isEmpty()) {
            throw new SQLException("Post title and content cannot be empty.");
        }

        // Anti-duplicate check
        String checkReq = "SELECT count(*) FROM posts WHERE title = ?";
        PreparedStatement pstCheck = conn.prepareStatement(checkReq);
        pstCheck.setString(1, p.getTitle());
        ResultSet rs = pstCheck.executeQuery();
        if (rs.next() && rs.getInt(1) > 0) {
            throw new SQLException("A post with this title already exists!");
        }

        String req = "INSERT INTO posts (title, content, author_name, author_role, upvotes) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, p.getTitle());
        pst.setString(2, p.getContent());
        pst.setString(3, p.getAuthorName());
        pst.setString(4, p.getAuthorRole());
        pst.setInt(5, p.getUpvotes());
        pst.executeUpdate();
    }

    @Override
    public void modifier(Post p) throws SQLException {
        // Input Control
        if (p.getTitle().trim().isEmpty() || p.getContent().trim().isEmpty()) {
            throw new SQLException("Updated title and content cannot be empty.");
        }

        String req = "UPDATE posts SET title=?, content=? WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, p.getTitle());
        pst.setString(2, p.getContent());
        pst.setInt(3, p.getId());
        pst.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM posts WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
    }

    @Override
    public List<Post> afficher() throws SQLException {
        List<Post> list = new ArrayList<>();
        String req = "SELECT * FROM posts ORDER BY id DESC";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(new Post(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("author_name"),
                    rs.getString("author_role"),
                    rs.getInt("upvotes")
            ));
        }
        return list;
    }
    public void updateVotes(int postId, int userId, int newVoteType) throws SQLException {
        // newVoteType is 1 for Up, -1 for Down
        String checkReq = "SELECT vote_type FROM post_votes WHERE user_id = ? AND post_id = ?";
        String insertReq = "INSERT INTO post_votes (user_id, post_id, vote_type) VALUES (?, ?, ?)";
        String updateVoteReq = "UPDATE post_votes SET vote_type = ? WHERE user_id = ? AND post_id = ?";
        String updatePostReq = "UPDATE posts SET upvotes = upvotes + ? WHERE id = ?";
        String deleteVoteReq = "DELETE FROM post_votes WHERE user_id = ? AND post_id = ?";

        try {
            conn.setAutoCommit(false);
            PreparedStatement psCheck = conn.prepareStatement(checkReq);
            psCheck.setInt(1, userId);
            psCheck.setInt(2, postId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                int existingVote = rs.getInt("vote_type");

                if (existingVote == newVoteType) {
                    // User clicked the same button again -> REMOVE VOTE
                    PreparedStatement psDel = conn.prepareStatement(deleteVoteReq);
                    psDel.setInt(1, userId);
                    psDel.setInt(2, postId);
                    psDel.executeUpdate();

                    PreparedStatement psPost = conn.prepareStatement(updatePostReq);
                    psPost.setInt(1, -existingVote); // Subtract the vote
                    psPost.setInt(2, postId);
                    psPost.executeUpdate();
                } else {
                    // User changed mind (e.g., Up to Down) -> SWITCH VOTE
                    PreparedStatement psUpdVote = conn.prepareStatement(updateVoteReq);
                    psUpdVote.setInt(1, newVoteType);
                    psUpdVote.setInt(2, userId);
                    psUpdVote.setInt(3, postId);
                    psUpdVote.executeUpdate();

                    PreparedStatement psPost = conn.prepareStatement(updatePostReq);
                    // Difference is (new - old). If switching Up(1) to Down(-1), diff is -2.
                    psPost.setInt(1, newVoteType - existingVote);
                    psPost.setInt(2, postId);
                    psPost.executeUpdate();
                }
            } else {
                // First time voting -> INSERT
                PreparedStatement psIns = conn.prepareStatement(insertReq);
                psIns.setInt(1, userId);
                psIns.setInt(2, postId);
                psIns.setInt(3, newVoteType);
                psIns.executeUpdate();

                PreparedStatement psPost = conn.prepareStatement(updatePostReq);
                psPost.setInt(1, newVoteType);
                psPost.setInt(2, postId);
                psPost.executeUpdate();
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