package services;

import entities.Candidature;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class CandidatureService {

    private final Connection connection;

    private static final Pattern EMAIL_RX =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final List<String> STATUTS = List.of("EN_ATTENTE", "ACCEPTE", "REFUSE");

    public CandidatureService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    private void validate(Candidature c, boolean isUpdate) {
        if (c == null) throw new IllegalArgumentException("Candidature null.");

        if (isUpdate && c.getId() <= 0) throw new IllegalArgumentException("ID invalide.");

        if (c.getNomCandidat() == null || c.getNomCandidat().trim().length() < 2)
            throw new IllegalArgumentException("Nom obligatoire (min 2 caractères).");

        if (c.getPrenomCandidat() == null || c.getPrenomCandidat().trim().length() < 2)
            throw new IllegalArgumentException("Prénom obligatoire (min 2 caractères).");

        if (c.getEmail() == null || c.getEmail().trim().isEmpty())
            throw new IllegalArgumentException("Email obligatoire.");

        if (!EMAIL_RX.matcher(c.getEmail().trim()).matches())
            throw new IllegalArgumentException("Email invalide.");

        if (c.getStatut() == null || c.getStatut().trim().isEmpty())
            throw new IllegalArgumentException("Statut obligatoire.");

        String s = c.getStatut().trim().toUpperCase();
        if (!STATUTS.contains(s))
            throw new IllegalArgumentException("Statut invalide (EN_ATTENTE / ACCEPTE / REFUSE).");

        c.setStatut(s);
        c.setEmail(c.getEmail().trim());
        c.setNomCandidat(c.getNomCandidat().trim());
        c.setPrenomCandidat(c.getPrenomCandidat().trim());
    }

    // CREATE (sans id)
    public void add(Candidature c) throws SQLException {
        addAndReturnId(c);
    }

    // CREATE (avec id généré)
    public int addAndReturnId(Candidature c) throws SQLException {
        validate(c, false);
        String sql = "INSERT INTO candidature (nom_candidat, prenom_candidat, email, statut) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNomCandidat());
            ps.setString(2, c.getPrenomCandidat());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getStatut());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    c.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Impossible de récupérer l'ID généré.");
    }

    // UPDATE
    public void update(Candidature c) throws SQLException {
        validate(c, true);
        String sql = "UPDATE candidature SET nom_candidat=?, prenom_candidat=?, email=?, statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getNomCandidat());
            ps.setString(2, c.getPrenomCandidat());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getStatut());
            ps.setInt(5, c.getId());
            ps.executeUpdate();
        }
    }

    // DELETE (supprime d'abord les entretiens liés pour éviter l'erreur FK)
    public void delete(int id) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("ID invalide.");

        // 1) supprimer les entretiens liés
        String deleteEntretiens = "DELETE FROM entretien WHERE candidature_id=?";
        try (PreparedStatement ps = connection.prepareStatement(deleteEntretiens)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }

        // 2) supprimer la candidature
        String sql = "DELETE FROM candidature WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // READ ALL
    public List<Candidature> findAll() throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = "SELECT id, nom_candidat, prenom_candidat, email, statut FROM candidature ORDER BY id DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // READ ONE
    public Optional<Candidature> findById(int id) throws SQLException {
        if (id <= 0) return Optional.empty();
        String sql = "SELECT id, nom_candidat, prenom_candidat, email, statut FROM candidature WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // EXISTS
    public boolean existsById(int id) throws SQLException {
        if (id <= 0) return false;
        String sql = "SELECT 1 FROM candidature WHERE id=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // pour ComboBox Entretien : "id - nom prenom"
    public List<String> findAllLabels() throws SQLException {
        List<String> labels = new ArrayList<>();
        String sql = "SELECT id, nom_candidat, prenom_candidat FROM candidature ORDER BY id DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom_candidat");
                String prenom = rs.getString("prenom_candidat");
                labels.add(id + " - " + nom + " " + prenom);
            }
        }
        return labels;
    }

    // ===== Helpers =====
    private Candidature mapRow(ResultSet rs) throws SQLException {
        Candidature c = new Candidature();
        c.setId(rs.getInt("id"));
        c.setNomCandidat(rs.getString("nom_candidat"));
        c.setPrenomCandidat(rs.getString("prenom_candidat"));
        c.setEmail(rs.getString("email"));
        c.setStatut(rs.getString("statut"));
        return c;
    }

    // Aliases
    public void ajouter(Candidature c) throws SQLException { add(c); }
    public List<Candidature> getAll() throws SQLException { return findAll(); }
}
