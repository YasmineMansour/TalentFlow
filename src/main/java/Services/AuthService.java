package Services;

import Entites.User;
import Utils.MyBD;
import java.sql.*;

public class AuthService {
    private static Connection conn = MyBD.getInstance().getConn();

    // This variable stays active as long as the app is open
    public static User currentUser;

    // 1. SIGN UP (Register)
    public static void signUp(String username, String password, String role) throws SQLException {
        String req = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, username);
        pst.setString(2, password);
        pst.setString(3, role);
        pst.executeUpdate();
    }

    // 2. LOGIN
    public static boolean login(String username, String password) throws SQLException {
        String req = "SELECT * FROM users WHERE username = ? AND password = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, username);
        pst.setString(2, password);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            currentUser =   new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role")
            );
            return true;
        }
        return false;
    }
}