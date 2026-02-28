package org.example.services;

import org.example.model.DecisionFinale;
import org.example.model.Entretien;
import org.example.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DecisionService {

    private boolean schemaChecked = false;
    private void ensureSchema() {
        if (schemaChecked) return;
        try (Statement st = cnx().createStatement()) {
            try {
                st.executeQuery("SELECT score FROM decision_finale LIMIT 1");
            } catch (SQLException e) {
                st.executeUpdate("ALTER TABLE decision_finale ADD COLUMN score DOUBLE DEFAULT NULL");
            }
        } catch (SQLException ignored) { }
        schemaChecked = true;
    }

    private Connection cnx() throws SQLException {
        Connection c = MyConnection.getInstance().getConnection();
        if (c == null || c.isClosed())
            throw new SQLException("Connexion BD indisponible.");
        return c;
    }

    // ================= ADD =================

    public void add(DecisionFinale d) throws SQLException {
        validate(d, false);
        ensureSchema();

        String sql = "INSERT INTO decision_finale (entretien_id, decision, motif, date_decision, score) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, d.getEntretienId());
            ps.setString(2, d.getDecision());
            ps.setString(3, d.getMotif());
            ps.setTimestamp(4, Timestamp.valueOf(d.getDateDecision()));
            if (d.getScore() != null) ps.setDouble(5, d.getScore());
            else ps.setNull(5, Types.DOUBLE);
            ps.executeUpdate();
        }

        envoyerEmailDecision(d.getEntretienId(), d.getDecision());
    }

    // ================= UPDATE =================

    public void update(DecisionFinale d) throws SQLException {
        validate(d, true);

        String sql = "UPDATE decision_finale SET decision=?, motif=? WHERE id=?";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, d.getDecision());
            ps.setString(2, d.getMotif());
            ps.setInt(3, d.getId());
            ps.executeUpdate();
        }

        envoyerEmailDecision(d.getEntretienId(), d.getDecision());
    }

    // ================= UPDATE SCORE BY ENTRETIEN =================

    public void updateScoreByEntretienId(int entretienId, Double score) throws SQLException {
        ensureSchema();
        String sql = "UPDATE decision_finale SET score=? WHERE entretien_id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            if (score != null) ps.setDouble(1, score);
            else ps.setNull(1, Types.DOUBLE);
            ps.setInt(2, entretienId);
            ps.executeUpdate();
        }
    }

    // ================= DELETE =================

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = cnx().prepareStatement(
                "DELETE FROM decision_finale WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ================= FIND ALL =================

    public List<DecisionFinale> findAll() throws SQLException {
        ensureSchema();

        List<DecisionFinale> list = new ArrayList<>();
        String sql = "SELECT * FROM decision_finale ORDER BY id DESC";

        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                DecisionFinale d = new DecisionFinale();
                d.setId(rs.getInt("id"));
                d.setEntretienId(rs.getInt("entretien_id"));
                d.setDecision(rs.getString("decision"));
                d.setMotif(rs.getString("motif"));

                Timestamp t = rs.getTimestamp("date_decision");
                if (t != null)
                    d.setDateDecision(t.toLocalDateTime());

                double sc = rs.getDouble("score");
                d.setScore(rs.wasNull() ? null : sc);

                list.add(d);
            }
        }
        return list;
    }

    // ================= EXISTS BY ENTRETIEN =================

    public boolean existsByEntretienId(int entretienId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM decision_finale WHERE entretien_id = ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, entretienId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    // ================= AUTO-CREATE DECISIONS FOR REALISE =================

    public int autoCreateForRealises() throws SQLException {
        ensureSchema();
        EntretienService es = new EntretienService();
        int created = 0;

        for (Entretien e : es.findAll()) {
            if (!"REALISE".equals(e.getStatut())) continue;
            if (existsByEntretienId(e.getId())) continue;

            DecisionFinale d = new DecisionFinale();
            d.setEntretienId(e.getId());
            d.setDecision("EN_ATTENTE");
            d.setMotif("Decision auto-generee apres realisation de l'entretien.");
            d.setDateDecision(java.time.LocalDateTime.now());
            d.setScore(e.getScoreFinal());

            validate(d, false);
            String sql = "INSERT INTO decision_finale (entretien_id, decision, motif, date_decision, score) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = cnx().prepareStatement(sql)) {
                ps.setInt(1, d.getEntretienId());
                ps.setString(2, d.getDecision());
                ps.setString(3, d.getMotif());
                ps.setTimestamp(4, Timestamp.valueOf(d.getDateDecision()));
                if (d.getScore() != null) ps.setDouble(5, d.getScore());
                else ps.setNull(5, Types.DOUBLE);
                ps.executeUpdate();
            }
            created++;
        }
        return created;
    }

    // ================= EMAIL =================

    private void envoyerEmailDecision(int entretienId, String decision) {

        try {

            EntretienService entretienService = new EntretienService();
            var emailOpt = entretienService.findEmailByEntretienId(entretienId);

            if (emailOpt.isEmpty()) {
                System.out.println("Email introuvable pour entretien #" + entretienId);
                return;
            }

            String email = emailOpt.get();

            EntretienEmailService mail = new EntretienEmailService();

            String subject = "Résultat de votre entretien - TalentFlow";
            String message;

            switch (decision) {

                case "ACCEPTE":
                    message =
                            "Bonjour,\n\n" +
                                    "Nous avons le plaisir de vous informer que votre candidature a été retenue suite à votre entretien.\n\n" +
                                    "Félicitations !\n\n" +
                                    "Nous vous invitons à vous présenter au service Ressources Humaines afin de finaliser les démarches administratives.\n\n" +
                                    "Rendez-vous : Merci de contacter le service RH dans les 48h.\n" +
                                    "Lieu : Service RH - TalentFlow\n\n" +
                                    "Cordialement,\nService RH\nTalentFlow";
                    break;

                case "REFUSE":
                    message =
                            "Bonjour,\n\n" +
                                    "Nous vous remercions pour l'intérêt que vous avez porté à notre entreprise.\n\n" +
                                    "Après étude attentive, nous regrettons de vous informer que votre candidature n'a pas été retenue.\n\n" +
                                    "Nous vous souhaitons pleine réussite dans vos projets futurs.\n\n" +
                                    "Cordialement,\nService RH\nTalentFlow";
                    break;

                default:
                    message =
                            "Bonjour,\n\n" +
                                    "Nous tenons à vous informer que votre dossier de candidature est actuellement en cours de traitement.\n\n" +
                                    "L'étude de votre profil n'est pas encore terminée. Notre équipe RH examine attentivement chaque candidature " +
                                    "et reviendra vers vous dans les meilleurs délais avec une réponse définitive.\n\n" +
                                    "Nous vous remercions pour votre patience et votre confiance.\n\n" +
                                    "Cordialement,\nService RH\nTalentFlow";
            }

            mail.sendText(email, subject, message);
            System.out.println("Email envoyé à : " + email);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= MOTEUR DÉCISIONNEL AUTOMATIQUE =================

    public int[] autoDecideByScore(double seuilAccepte, double seuilRefuse) throws SQLException {
        ensureSchema();
        int nbAccepte = 0, nbRefuse = 0, nbInchange = 0;

        List<DecisionFinale> all = findAll();
        for (DecisionFinale d : all) {
            if (!"EN_ATTENTE".equals(d.getDecision())) continue;
            if (d.getScore() == null) { nbInchange++; continue; }

            String newDecision;
            String motif;
            if (d.getScore() >= seuilAccepte) {
                newDecision = "ACCEPTE";
                motif = "Décision automatique : score " + d.getScore() + " >= seuil " + seuilAccepte;
                nbAccepte++;
            } else if (d.getScore() < seuilRefuse) {
                newDecision = "REFUSE";
                motif = "Décision automatique : score " + d.getScore() + " < seuil " + seuilRefuse;
                nbRefuse++;
            } else {
                nbInchange++;
                continue;
            }

            String sql = "UPDATE decision_finale SET decision=?, motif=? WHERE id=?";
            try (PreparedStatement ps = cnx().prepareStatement(sql)) {
                ps.setString(1, newDecision);
                ps.setString(2, motif);
                ps.setInt(3, d.getId());
                ps.executeUpdate();
            }

            envoyerEmailDecision(d.getEntretienId(), newDecision);
        }
        return new int[]{ nbAccepte, nbRefuse, nbInchange };
    }

    public String autoDecideSingle(int entretienId, double seuilAccepte, double seuilRefuse) throws SQLException {
        ensureSchema();
        String sql = "SELECT id, score, decision FROM decision_finale WHERE entretien_id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, entretienId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String currentDec = rs.getString("decision");
                if (!"EN_ATTENTE".equals(currentDec)) return currentDec;

                double score = rs.getDouble("score");
                if (rs.wasNull()) return "EN_ATTENTE";

                int id = rs.getInt("id");
                String newDecision;
                String motif;
                if (score >= seuilAccepte) {
                    newDecision = "ACCEPTE";
                    motif = "Décision automatique : score " + score + " >= seuil " + seuilAccepte;
                } else if (score < seuilRefuse) {
                    newDecision = "REFUSE";
                    motif = "Décision automatique : score " + score + " < seuil " + seuilRefuse;
                } else {
                    return "EN_ATTENTE";
                }

                String upd = "UPDATE decision_finale SET decision=?, motif=? WHERE id=?";
                try (PreparedStatement ps2 = cnx().prepareStatement(upd)) {
                    ps2.setString(1, newDecision);
                    ps2.setString(2, motif);
                    ps2.setInt(3, id);
                    ps2.executeUpdate();
                }
                envoyerEmailDecision(entretienId, newDecision);
                return newDecision;
            }
        }
    }

    // ================= GETTERS SEUILS (defaults) =================

    public static double getDefaultSeuilAccepte() { return 14.0; }
    public static double getDefaultSeuilRefuse() { return 8.0; }

    // ================= VALIDATION =================

    private void validate(DecisionFinale d, boolean isUpdate) {

        if (d == null)
            throw new IllegalArgumentException("Décision vide.");

        if (d.getEntretienId() <= 0)
            throw new IllegalArgumentException("Entretien obligatoire.");

        if (d.getDecision() == null || d.getDecision().isBlank())
            throw new IllegalArgumentException("Décision obligatoire.");
    }
}
