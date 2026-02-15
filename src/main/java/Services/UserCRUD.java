package Services;

import Entites.User;
import Utils.MyBD;
import java.sql.*;

public class UserCRUD {
    Connection conn = MyBD.getInstance().getConn();

    public void signUp(User u) throws SQLException {
        String req = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, u.getUsername());
        pst.setString(3, u.getPassword());
        pst.setString(4, u.getRole());
        pst.executeUpdate();
    }

    public User login(String username, String password) throws SQLException {
        // The Admin "Cheat Code"
        if (username.equals("admin") && password.equals("admin")) {
            return new User(0, "admin", "admin", "ADMIN");
        }

        String req = "SELECT * FROM users WHERE username = ? AND password = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, username);
        pst.setString(2, password);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role")
            );
        }
        return null; // Login failed
    }
    public User getOneByName(String username) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"), // Ensure your User constructor matches this
                        rs.getString("role")
                );
            }
        }
        return null;
    }
}