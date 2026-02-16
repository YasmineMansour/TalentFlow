package services;

import entities.Entretien;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntretienService {

    private final Connection connection;
    private final CandidatureService candidatureService;

    private static final List<String> TYPES = List.of("EN_LIGNE", "PRESENTIEL", "TELEPHONIQUE");
    private static final List<String> STATUTS = List.of("PLANIFIE", "REALISE", "ANNULE");

    public EntretienService() {
        connection = MyDataBase.getInstance().getConnection();
        candidatureService = new CandidatureService();
    }

    // ---------------- VALIDATION ----------------
    private void validate(Entretien e, boolean isUpdate) throws SQLException {
        if (e == null) throw new IllegalArgumentException("Entretien null.");

        if (isUpdate && e.getId() <= 0)
            throw new IllegalArgumentException("ID entretien invalide.");

        if (e.getCandidatureId() <= 0)
            throw new IllegalArgumentException("Candidature obligatoire.");

        if (!candidatureService.existsById(e.getCandidatureId()))
            throw new IllegalArgumentException("Candidature inexistante.");

        if (e.getDateHeure() == null)
            throw new IllegalArgumentException("Date/heure obligatoire.");

        if (e.getDateHeure().isBefore(LocalDateTime.now().minusMinutes(1)))
            throw new IllegalArgumentException("Date/heure doit être dans le futur.");

        if (e.getType() == null || e.getType().isBlank())
            throw new IllegalArgumentException("Type obligatoire.");

        String type = e.getType().trim().toUpperCase();
        if (!TYPES.contains(type))
            throw new IllegalArgumentException("Type invalide (EN_LIGNE / PRESENTIEL / TELEPHONIQUE).");
        e.setType(type);

        if (e.getStatut() == null || e.getStatut().isBlank())
            throw new IllegalArgumentException("Statut obligatoire.");

        String statut = e.getStatut().trim().toUpperCase();
        if (!STATUTS.contains(statut))
            throw new IllegalArgumentException("Statut invalide (PLANIFIE / REALISE / ANNULE).");
        e.setStatut(statut);

        if (type.equals("EN_LIGNE")) {
            if (e.getLien() == null || e.getLien().isBlank())
                throw new IllegalArgumentException("Lien obligatoire pour un entretien en ligne.");
            e.setLieu(null);
            e.setLien(e.getLien().trim());
        } else if (type.equals("PRESENTIEL")) {
            if (e.getLieu() == null || e.getLieu().isBlank())
                throw new IllegalArgumentException("Lieu obligatoire pour un entretien présentiel.");
            e.setLien(null);
            e.setLieu(e.getLieu().trim());
        } else { // TELEPHONIQUE
            if (e.getLieu() != null && e.getLieu().isBlank()) e.setLieu(null);
            if (e.getLien() != null && e.getLien().isBlank()) e.setLien(null);
        }

        if (e.getNoteTechnique() != null && (e.getNoteTechnique() < 0 || e.getNoteTechnique() > 20))
            throw new IllegalArgumentException("Note technique doit être entre 0 et 20.");

        if (e.getNoteCommunication() != null && (e.getNoteCommunication() < 0 || e.getNoteCommunication() > 20))
            throw new IllegalArgumentException("Note communication doit être entre 0 et 20.");

        if (e.getCommentaire() != null) e.setCommentaire(e.getCommentaire().trim());
    }

    // ---------------- CRUD ----------------

    public void add(Entretien e) throws SQLException {
        addAndReturnId(e);
    }

    public int addAndReturnId(Entretien e) throws SQLException {
        validate(e, false);

        String sql = "INSERT INTO entretien " +
                "(candidature_id, date_heure, type, lieu, lien, statut, note_technique, note_communication, commentaire) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getCandidatureId());
            ps.setTimestamp(2, Timestamp.valueOf(e.getDateHeure()));
            ps.setString(3, e.getType());
            ps.setString(4, e.getLieu());
            ps.setString(5, e.getLien());
            ps.setString(6, e.getStatut());
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
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new SQLException("Candidature invalide (contrainte BD).", ex);
        }

        throw new SQLException("Impossible de récupérer l'ID généré.");
    }

    public void update(Entretien e) throws SQLException {
        validate(e, true);

        String sql = "UPDATE entretien SET " +
                "candidature_id=?, date_heure=?, type=?, lieu=?, lien=?, statut=?, " +
                "note_technique=?, note_communication=?, commentaire=? " +
                "WHERE id=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, e.getCandidatureId());
            ps.setTimestamp(2, Timestamp.valueOf(e.getDateHeure()));
            ps.setString(3, e.getType());
            ps.setString(4, e.getLieu());
            ps.setString(5, e.getLien());
            ps.setString(6, e.getStatut());
            ps.setObject(7, e.getNoteTechnique());
            ps.setObject(8, e.getNoteCommunication());
            ps.setString(9, e.getCommentaire());
            ps.setInt(10, e.getId());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new SQLException("Candidature invalide (contrainte BD).", ex);
        }
    }

    public void delete(int id) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("ID invalide.");
        String sql = "DELETE FROM entretien WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int deleteAllForDebug() throws SQLException {
        String sql = "DELETE FROM entretien";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    // ---------------- READ ----------------

    public List<Entretien> findAll() throws SQLException {
        List<Entretien> list = new ArrayList<>();

        String sql =
                "SELECT e.*, c.nom_candidat, c.prenom_candidat " +
                        "FROM entretien e " +
                        "LEFT JOIN candidature c ON e.candidature_id = c.id " +
                        "ORDER BY e.date_heure DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }

        return list;
    }

    public Optional<Entretien> findById(int id) throws SQLException {
        if (id <= 0) return Optional.empty();
        String sql =
                "SELECT e.*, c.nom_candidat, c.prenom_candidat " +
                        "FROM entretien e " +
                        "LEFT JOIN candidature c ON e.candidature_id = c.id " +
                        "WHERE e.id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public boolean existsById(int id) throws SQLException {
        if (id <= 0) return false;
        String sql = "SELECT 1 FROM entretien WHERE id=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<String> findAllCandidaturesLabels() throws SQLException {
        List<String> labels = new ArrayList<>();
        String sql = "SELECT id, nom_candidat, prenom_candidat FROM candidature ORDER BY id DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom_candidat");
                String prenom = rs.getString("prenom_candidat");
                String label = id + " - " + (nom == null ? "" : nom) + " " + (prenom == null ? "" : prenom);
                labels.add(label.trim());
            }
        }
        return labels;
    }

    private Entretien mapRow(ResultSet rs) throws SQLException {
        Entretien e = new Entretien();

        e.setId(rs.getInt("id"));
        e.setCandidatureId(rs.getInt("candidature_id"));

        Timestamp ts = rs.getTimestamp("date_heure");
        if (ts != null) e.setDateHeure(ts.toLocalDateTime());

        e.setType(rs.getString("type"));
        e.setLieu(rs.getString("lieu"));
        e.setLien(rs.getString("lien"));
        e.setStatut(rs.getString("statut"));
        e.setNoteTechnique((Integer) rs.getObject("note_technique"));
        e.setNoteCommunication((Integer) rs.getObject("note_communication"));
        e.setCommentaire(rs.getString("commentaire"));

        String nom = rs.getString("nom_candidat");
        String prenom = rs.getString("prenom_candidat");
        String full = ((nom == null) ? "" : nom) + " " + ((prenom == null) ? "" : prenom);
        full = full.trim();
        e.setNomComplet(full.isEmpty() ? ("Candidature #" + e.getCandidatureId()) : full);

        return e;
    }
}
