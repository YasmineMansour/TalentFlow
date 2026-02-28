package org.example.services;

import org.example.model.Entretien;
import org.example.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.LocalDate;

public class EntretienService {

    private Connection cnx() throws SQLException {
        Connection c = MyConnection.getInstance().getConnection();
        if (c == null || c.isClosed()) throw new SQLException("Connexion BD indisponible.");
        return c;
    }

    private static final List<String> TYPES = List.of("EN_LIGNE", "PRESENTIEL", "TELEPHONIQUE");
    private static final List<String> STATUTS = List.of("PLANIFIE", "REALISE", "ANNULE");

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    // ---------------- VALIDATION ----------------
    private void validate(Entretien e, boolean isUpdate) {

        if (e == null)
            throw new IllegalArgumentException("Entretien vide.");

        if (isUpdate && e.getId() <= 0)
            throw new IllegalArgumentException("ID entretien invalide.");

        if (e.getDateEntretien() == null)
            throw new IllegalArgumentException("Date/heure obligatoire.");

        if (e.getCandidatureId() <= 0)
            throw new IllegalArgumentException("Candidature obligatoire.");

        if (e.getType() == null || e.getType().isBlank())
            throw new IllegalArgumentException("Type obligatoire.");

        String type = e.getType().trim().toUpperCase();
        if (!TYPES.contains(type))
            throw new IllegalArgumentException("Type invalide.");
        e.setType(type);

        if (e.getStatut() == null || e.getStatut().isBlank())
            throw new IllegalArgumentException("Statut obligatoire.");

        String statut = e.getStatut().trim().toUpperCase();
        if (!STATUTS.contains(statut))
            throw new IllegalArgumentException("Statut invalide.");
        e.setStatut(statut);
    }

    // ---------------- CRUD ----------------

    public int add(Entretien e) throws SQLException {

        validate(e, false);

        String sql = """
            INSERT INTO entretien
            (candidature_id, date_heure, type, statut, lieu, lien,
             note_technique, note_communication, commentaire)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps =
                     cnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, e.getCandidatureId());
            ps.setTimestamp(2, Timestamp.valueOf(e.getDateEntretien()));
            ps.setString(3, e.getType());
            ps.setString(4, e.getStatut());
            ps.setString(5, e.getLieu());
            ps.setString(6, e.getLien());
            ps.setObject(7, e.getNoteTechnique());
            ps.setObject(8, e.getNoteCommunication());
            ps.setString(9, e.getCommentaire());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    e.setId(id);
                    return id;
                }
            }
        }

        throw new SQLException("Impossible de récupérer l'ID généré.");
    }

    public void update(Entretien e) throws SQLException {

        validate(e, true);

        String sql = """
            UPDATE entretien
            SET candidature_id=?, date_heure=?, type=?, statut=?, lieu=?, lien=?,
                note_technique=?, note_communication=?,
                commentaire=?
            WHERE id=?
        """;

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {

            ps.setInt(1, e.getCandidatureId());
            ps.setTimestamp(2, Timestamp.valueOf(e.getDateEntretien()));
            ps.setString(3, e.getType());
            ps.setString(4, e.getStatut());
            ps.setString(5, e.getLieu());
            ps.setString(6, e.getLien());
            ps.setObject(7, e.getNoteTechnique());
            ps.setObject(8, e.getNoteCommunication());
            ps.setString(9, e.getCommentaire());
            ps.setInt(10, e.getId());

            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps =
                     cnx().prepareStatement("DELETE FROM entretien WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ---------------- READ ----------------

    public List<Entretien> findAll() throws SQLException {
        List<Entretien> list = new ArrayList<>();
        String sql = """
            SELECT e.*, c.email AS email_candidat
            FROM entretien e
            LEFT JOIN candidature c ON e.candidature_id = c.id
            ORDER BY e.date_heure DESC
        """;
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        }
        return list;
    }

    public Optional<Entretien> findById(int id) throws SQLException {
        String sql = """
            SELECT e.*, c.email AS email_candidat
            FROM entretien e
            LEFT JOIN candidature c ON e.candidature_id = c.id
            WHERE e.id=?
        """;
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    private Entretien mapRow(ResultSet rs) throws SQLException {

        Entretien e = new Entretien();
        e.setId(rs.getInt("id"));
        e.setCandidatureId(rs.getInt("candidature_id"));

        Timestamp ts = rs.getTimestamp("date_heure");
        if (ts != null)
            e.setDateEntretien(ts.toLocalDateTime());

        e.setType(rs.getString("type"));
        e.setStatut(rs.getString("statut"));
        e.setLieu(rs.getString("lieu"));
        e.setLien(rs.getString("lien"));
        e.setNoteTechnique((Integer) rs.getObject("note_technique"));
        e.setNoteCommunication((Integer) rs.getObject("note_communication"));
        e.setCommentaire(rs.getString("commentaire"));

        // email from JOIN (may be null if no JOIN or no candidature)
        try {
            e.setEmailCandidat(rs.getString("email_candidat"));
        } catch (SQLException ignored) {
            // column not present in this query
        }

        return e;
    }

    public boolean existeConflit(LocalDateTime dateHeure) throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM entretien WHERE date_heure = ? AND statut <> 'ANNULE'";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(dateHeure));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        }

        return false;
    }

    public List<Entretien> getEntretiensByDate(LocalDate date) throws SQLException {

        List<Entretien> list = new ArrayList<>();

        String sql = "SELECT * FROM entretien WHERE DATE(date_heure) = ?";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }

        return list;
    }

    public Optional<String> findEmailByEntretienId(int entretienId) throws SQLException {

        if (entretienId <= 0) return Optional.empty();

        String sql = "SELECT c.email FROM entretien e JOIN candidature c ON e.candidature_id = c.id WHERE e.id=?";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {

            ps.setInt(1, entretienId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("email"));
                }
            }
        }

        return Optional.empty();
    }

    public int findCandidatureIdByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) return -1;
        String sql = "SELECT id FROM candidature WHERE email=? LIMIT 1";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }

    public boolean existsById(int id) throws SQLException {

        if (id <= 0) return false;

        String sql = "SELECT 1 FROM entretien WHERE id=? LIMIT 1";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Entretien> getAll() throws SQLException {
        return findAll();
    }

    // ─────── Statut Automatique Intelligent ───────

    public int autoUpdateStatuts() throws SQLException {
        String sql = """
            UPDATE entretien
            SET statut = 'REALISE'
            WHERE statut = 'PLANIFIE'
              AND date_heure < NOW()
        """;
        try (Statement st = cnx().createStatement()) {
            return st.executeUpdate(sql);
        }
    }

    // ─────── Classement par score ───────

    public List<Entretien> getClassement() throws SQLException {
        String sql = """
            SELECT e.*, c.email AS email_candidat
            FROM entretien e
            LEFT JOIN candidature c ON e.candidature_id = c.id
            WHERE e.note_technique IS NOT NULL
              AND e.note_communication IS NOT NULL
            ORDER BY (e.note_technique * 0.7 + e.note_communication * 0.3) DESC
        """;
        List<Entretien> list = new ArrayList<>();
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        }
        return list;
    }
}
