package org.example.utils;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.example.model.User;

import java.awt.Color;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service d'export PDF utilisant OpenPDF.
 * Génère des rapports professionnels avec le branding TalentFlow.
 */
public class PdfExportService {

    // Couleurs TalentFlow
    private static final Color PRIMARY = new Color(108, 92, 231);       // #6c5ce7
    private static final Color DARK = new Color(45, 52, 54);            // #2d3436
    private static final Color GRAY = new Color(99, 110, 114);          // #636e72
    private static final Color LIGHT_BG = new Color(248, 249, 250);     // #f8f9fa
    private static final Color GREEN = new Color(0, 184, 148);          // #00b894
    private static final Color ORANGE = new Color(253, 203, 110);       // #fdcb6e

    /**
     * Exporte la liste des utilisateurs en PDF.
     * @param users la liste des utilisateurs
     * @param filePath le chemin du fichier PDF de sortie
     * @return true si l'export a réussi
     */
    public static boolean exportUserList(List<User> users, String filePath) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // ===== EN-TÊTE =====
            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY);
            Paragraph title = new Paragraph("TalentFlow", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font subtitleFont = new Font(Font.HELVETICA, 14, Font.NORMAL, GRAY);
            Paragraph subtitle = new Paragraph("Rapport des Utilisateurs", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(5);
            document.add(subtitle);

            // Date de génération
            Font dateFont = new Font(Font.HELVETICA, 10, Font.ITALIC, GRAY);
            Paragraph date = new Paragraph(
                    "Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                    dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            // Ligne de séparation
            addSeparator(document);

            // ===== STATISTIQUES RAPIDES =====
            Font sectionFont = new Font(Font.HELVETICA, 13, Font.BOLD, DARK);

            long totalUsers = users.size();
            long adminCount = users.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
            long rhCount = users.stream().filter(u -> "RH".equalsIgnoreCase(u.getRole())).count();
            long candidatCount = users.stream().filter(u -> "CANDIDAT".equalsIgnoreCase(u.getRole())).count();

            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingBefore(10);
            statsTable.setSpacingAfter(20);

            addStatCell(statsTable, "Total", String.valueOf(totalUsers), PRIMARY);
            addStatCell(statsTable, "Admins", String.valueOf(adminCount), PRIMARY);
            addStatCell(statsTable, "RH", String.valueOf(rhCount), GREEN);
            addStatCell(statsTable, "Candidats", String.valueOf(candidatCount), ORANGE);

            document.add(statsTable);

            // ===== TABLEAU DES UTILISATEURS =====
            Paragraph tableTitle = new Paragraph("Liste détaillée des utilisateurs", sectionFont);
            tableTitle.setSpacingAfter(12);
            document.add(tableTitle);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.8f, 2f, 2f, 3f, 1.5f, 2f});

            // En-têtes du tableau
            String[] headers = {"ID", "Nom", "Prénom", "Email", "Rôle", "Téléphone"};
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(PRIMARY);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderWidth(0);
                table.addCell(cell);
            }

            // Données
            Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL, DARK);
            Font dataFontSmall = new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY);
            boolean alternate = false;

            for (User u : users) {
                Color bg = alternate ? LIGHT_BG : Color.WHITE;
                addDataCell(table, String.valueOf(u.getId()), dataFont, bg, Element.ALIGN_CENTER);
                addDataCell(table, u.getNom(), dataFont, bg, Element.ALIGN_LEFT);
                addDataCell(table, u.getPrenom(), dataFont, bg, Element.ALIGN_LEFT);
                addDataCell(table, u.getEmail(), dataFontSmall, bg, Element.ALIGN_LEFT);
                addRoleBadgeCell(table, u.getRole(), bg);
                addDataCell(table, u.getTelephone(), dataFont, bg, Element.ALIGN_CENTER);
                alternate = !alternate;
            }

            document.add(table);

            // ===== PIED DE PAGE =====
            document.add(new Paragraph("\n"));
            addSeparator(document);

            Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, GRAY);
            Paragraph footer = new Paragraph(
                    "TalentFlow © " + LocalDateTime.now().getYear() +
                    " — Document généré automatiquement — " + totalUsers + " utilisateur(s)",
                    footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            document.add(footer);

            document.close();
            System.out.println("✅ PDF exporté avec succès : " + filePath);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'export PDF : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ===== MÉTHODES UTILITAIRES =====

    private static void addDataCell(PdfPTable table, String text, Font font, Color bg, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(7);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(224, 224, 224));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addRoleBadgeCell(PdfPTable table, String role, Color bg) {
        Font roleFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        if (role == null) role = "—";

        switch (role.toUpperCase()) {
            case "ADMIN" -> roleFont.setColor(PRIMARY);
            case "RH" -> roleFont.setColor(GREEN);
            case "CANDIDAT" -> roleFont.setColor(new Color(214, 48, 49));
            default -> roleFont.setColor(GRAY);
        }

        PdfPCell cell = new PdfPCell(new Phrase(role, roleFont));
        cell.setBackgroundColor(bg);
        cell.setPadding(7);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(224, 224, 224));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private static void addStatCell(PdfPTable table, String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderWidth(1f);
        cell.setBorderColor(color);
        cell.setBackgroundColor(Color.WHITE);
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font valueFont = new Font(Font.HELVETICA, 20, Font.BOLD, color);
        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY);

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk(value + "\n", valueFont));
        p.add(new Chunk(label, labelFont));
        cell.addElement(p);

        table.addCell(cell);
    }

    private static void addSeparator(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorderWidth(0);
        lineCell.setBorderWidthBottom(1);
        lineCell.setBorderColorBottom(new Color(224, 224, 224));
        lineCell.setFixedHeight(1);
        line.addCell(lineCell);
        line.setSpacingAfter(10);
        document.add(line);
    }
}
