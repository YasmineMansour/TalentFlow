package Services;

import Entites.Comment; // Create a simple Comment entity similar to Post
import Utils.MyBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentService {
    Connection conn = MyBD.getInstance().getConn();

    public void ajouterCommentaire(int postId, String author, String content) throws SQLException {
        String req = "INSERT INTO comments (post_id, author_name, content) VALUES (?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        pst.setString(2, author);
        pst.setString(3, content);
        pst.executeUpdate();
    }

    public List<Comment> getCommentsByPost(int postId) throws SQLException {
        List<Comment> list = new ArrayList<>();
        String req = "SELECT * FROM comments WHERE post_id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, postId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            Comment c = new Comment();
            c.setId(rs.getInt("id"));
            c.setPostId(rs.getInt("post_id"));
            c.setAuthorName(rs.getString("author_name"));
            c.setContent(rs.getString("content"));
            list.add(c);
        }
        return list;
    }

    public void supprimerCommentaire(int commentId) throws SQLException {
        String req = "DELETE FROM comments WHERE id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, commentId);
        pst.executeUpdate();
    }
    public void modifierCommentaire(int commentId, String newText) throws SQLException {
        String req = "UPDATE comments SET content = ? WHERE id = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, newText);
        pst.setInt(2, commentId);
        pst.executeUpdate();
    }
}