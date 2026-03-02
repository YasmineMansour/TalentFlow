package org.example.services;

import org.example.model.PieceJointe;
import org.example.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PieceJointeService {

    private Connection getConn() {
        return MyConnection.getInstance().getConnection();
    }

    // Table piece_jointe est créée par CandidatureService.static{}

    public List<PieceJointe> getByCandidature(int candidatureId) throws SQLException {
        List<PieceJointe> list = new ArrayList<>();
        String sql = "SELECT * FROM piece_jointe WHERE candidature_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, candidatureId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public void ajouter(PieceJointe p) throws SQLException {
        String sql = "INSERT INTO piece_jointe (candidature_id, titre, type_doc, url) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, p.getCandidatureId());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getTypeDoc());
            ps.setString(4, p.getUrl());
            ps.executeUpdate();
        }
    }

    public void modifier(PieceJointe p) throws SQLException {
        String sql = "UPDATE piece_jointe SET titre=?, type_doc=?, url=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, p.getTitre());
            ps.setString(2, p.getTypeDoc());
            ps.setString(3, p.getUrl());
            ps.setInt(4, p.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM piece_jointe WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<PieceJointe> afficherTout() throws SQLException {
        List<PieceJointe> list = new ArrayList<>();
        String sql = "SELECT * FROM piece_jointe ORDER BY created_at DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public int countByCandidature(int candidatureId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM piece_jointe WHERE candidature_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, candidatureId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private PieceJointe map(ResultSet rs) throws SQLException {
        return new PieceJointe(
                rs.getInt("id"),
                rs.getInt("candidature_id"),
                rs.getString("titre"),
                rs.getString("type_doc"),
                rs.getString("url"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
        );
    }
}
