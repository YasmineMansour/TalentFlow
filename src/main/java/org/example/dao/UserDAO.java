package org.example.dao;

import org.example.model.User;
import org.example.utils.MyConnection;
import org.example.utils.PasswordHasher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private Connection conn;

    public UserDAO() {
        refreshConnection();
    }

    /** Rafraîchit la connexion si elle est fermée ou nulle */
    private void refreshConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                this.conn = MyConnection.getInstance().getConnection();
            }
        } catch (SQLException e) {
            System.err.println("❌ UserDAO : Erreur lors de la vérification de la connexion : " + e.getMessage());
        }
    }

    /** Récupère la connexion active, en la rafraîchissant si nécessaire */
    private Connection getConn() {
        refreshConnection();
        return conn;
    }

    // --- MÉTHODE LOGIN (supporte les anciens mots de passe en clair + BCrypt) ---
    public User login(String email, String rawPassword) {
        if (getConn() == null) return null;
        String sql = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    boolean passwordMatch = false;

                    if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
                        // Le mot de passe est déjà haché avec BCrypt
                        passwordMatch = PasswordHasher.check(rawPassword, storedPassword);
                    } else {
                        // Ancien mot de passe en clair — comparaison directe
                        passwordMatch = storedPassword.equals(rawPassword);

                        // Migration automatique : hasher le mot de passe en clair
                        if (passwordMatch) {
                            String hashed = PasswordHasher.hash(rawPassword);
                            String updateSql = "UPDATE user SET password = ? WHERE id = ?";
                            try (PreparedStatement updateStmt = getConn().prepareStatement(updateSql)) {
                                updateStmt.setString(1, hashed);
                                updateStmt.setInt(2, rs.getInt("id"));
                                updateStmt.executeUpdate();
                                System.out.println("🔄 Mot de passe migré vers BCrypt pour : " + email);
                            }
                        }
                    }

                    if (passwordMatch) {
                        return mapUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- VÉRIFIER SI UN EMAIL EXISTE DÉJÀ ---
    public boolean emailExists(String email) {
        if (getConn() == null) return false;
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- MÉTHODE CREATE (avec hachage du mot de passe) ---
    public boolean create(User user) {
        if (getConn() == null) {
            System.err.println("❌ Impossible de créer : Connexion BDD inexistante.");
            return false;
        }
        // Vérifier l'unicité de l'email
        if (emailExists(user.getEmail())) {
            System.err.println("❌ Email déjà utilisé : " + user.getEmail());
            return false;
        }
        String sql = "INSERT INTO user (nom, prenom, email, password, role, telephone) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            // Hacher le mot de passe avant l'insertion
            pstmt.setString(4, PasswordHasher.hash(user.getPassword()));
            pstmt.setString(5, user.getRole());
            pstmt.setString(6, user.getTelephone());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("✅ Utilisateur inséré avec succès (ID: " + user.getId() + ").");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL lors de l'insertion : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // --- MÉTHODE READ ALL ---
    public List<User> readAll() {
        List<User> users = new ArrayList<>();
        if (getConn() == null) return users;
        String sql = "SELECT * FROM user ORDER BY id DESC";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // --- RECHERCHE PAR MOT-CLÉ (nom, prénom ou email) ---
    public List<User> search(String keyword) {
        List<User> users = new ArrayList<>();
        if (getConn() == null || keyword == null || keyword.trim().isEmpty()) return readAll();
        String sql = "SELECT * FROM user WHERE nom LIKE ? OR prenom LIKE ? OR email LIKE ? ORDER BY id DESC";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            String pattern = "%" + keyword.trim() + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // --- TROUVER PAR ID ---
    public User findById(int id) {
        if (getConn() == null) return null;
        String sql = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- TROUVER PAR EMAIL ---
    public User findByEmail(String email) {
        if (getConn() == null) return null;
        String sql = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- MÉTHODE UPDATE (sans modifier le mot de passe si vide) ---
    public boolean update(User user) {
        if (getConn() == null) return false;

        // Vérifier que l'email n'est pas déjà pris par un autre utilisateur
        String checkSql = "SELECT COUNT(*) FROM user WHERE email = ? AND id != ?";
        try (PreparedStatement checkStmt = getConn().prepareStatement(checkSql)) {
            checkStmt.setString(1, user.getEmail());
            checkStmt.setInt(2, user.getId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.err.println("❌ Email déjà utilisé par un autre utilisateur.");
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        String sql = "UPDATE user SET nom=?, prenom=?, email=?, password=?, role=?, telephone=? WHERE id=?";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, user.getNom());
            pstmt.setString(2, user.getPrenom());
            pstmt.setString(3, user.getEmail());
            // Si le mot de passe a été modifié (non-haché), le hacher
            String password = user.getPassword();
            if (password != null && !password.isEmpty() && !password.startsWith("$2a$")) {
                password = PasswordHasher.hash(password);
            }
            pstmt.setString(4, password);
            pstmt.setString(5, user.getRole());
            pstmt.setString(6, user.getTelephone());
            pstmt.setInt(7, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- MÉTHODE DELETE ---
    public boolean delete(int id) {
        if (getConn() == null) return false;
        String sql = "DELETE FROM user WHERE id=?";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- COMPTER LE NOMBRE TOTAL D'UTILISATEURS ---
    public int count() {
        if (getConn() == null) return 0;
        String sql = "SELECT COUNT(*) FROM user";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // --- COMPTER PAR RÔLE ---
    public int countByRole(String role) {
        if (getConn() == null) return 0;
        String sql = "SELECT COUNT(*) FROM user WHERE role = ?";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, role);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // --- DERNIERS UTILISATEURS INSCRITS ---
    public List<User> getRecentUsers(int limit) {
        List<User> users = new ArrayList<>();
        if (getConn() == null) return users;
        String sql = "SELECT * FROM user ORDER BY id DESC LIMIT ?";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // --- TROUVER OU CRÉER UN UTILISATEUR GOOGLE ---
    public User findOrCreateGoogleUser(String email, String nom, String prenom) {
        User existing = findByEmail(email);
        if (existing != null) return existing;

        // Créer un nouvel utilisateur avec un mot de passe aléatoire (connexion Google uniquement)
        User newUser = new User(0,
                nom != null && !nom.isEmpty() ? nom : "Utilisateur",
                prenom != null && !prenom.isEmpty() ? prenom : "Google",
                email,
                java.util.UUID.randomUUID().toString(),  // mot de passe aléatoire
                "CANDIDAT",
                ""
        );
        boolean created = create(newUser);
        if (created) {
            System.out.println("✅ Utilisateur Google créé : " + email);
            return findByEmail(email);
        }
        return null;
    }

    // --- MÉTHODE UTILITAIRE : Mapper un ResultSet vers un objet User ---
    private User mapUser(ResultSet rs) throws SQLException {
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

    // --- METTRE À JOUR LE MOT DE PASSE PAR EMAIL (pour réinitialisation) ---
    public boolean updatePassword(String email, String newPassword) {
        if (getConn() == null || email == null || newPassword == null) return false;
        String hashed = PasswordHasher.hash(newPassword);
        String sql = "UPDATE user SET password = ? WHERE email = ?";
        try (PreparedStatement pstmt = getConn().prepareStatement(sql)) {
            pstmt.setString(1, hashed);
            pstmt.setString(2, email);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                System.out.println("✅ Mot de passe réinitialisé pour : " + email);
            }
            return success;
        } catch (SQLException e) {
            System.err.println("❌ Erreur réinitialisation mot de passe : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}