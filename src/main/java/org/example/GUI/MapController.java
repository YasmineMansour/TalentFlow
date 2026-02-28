package org.example.GUI;

import org.example.model.Offre;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.example.services.GeocodingService;
import org.example.services.OffreService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controleur de la carte interactive.
 * Affiche une carte Leaflet.js dans un WebView.
 */
public class MapController {

    @FXML private Label lblMapInfo;
    @FXML private TextField tfPosition;
    @FXML private ComboBox<String> comboRayon;
    @FXML private Label lblResultats;
    @FXML private WebView webViewMap;

    private final OffreService offreService = new OffreService();
    private final GeocodingService geoService = new GeocodingService();

    private List<Offre> toutesLesOffres = new ArrayList<>();
    private Offre offreCiblee;

    public void setOffre(Offre offre) {
        this.offreCiblee = offre;
    }

    @FXML
    public void initialize() {
        comboRayon.setItems(FXCollections.observableArrayList(
                "5 km", "10 km", "20 km", "50 km", "100 km", "Tout afficher"
        ));
        comboRayon.setValue("Tout afficher");
        tfPosition.setPromptText("Ex: Tunis, Ariana...");
        Platform.runLater(this::chargerCarte);
    }

    private void chargerCarte() {
        lblMapInfo.setText("Chargement des offres et g\u00e9ocodage...");

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                toutesLesOffres = offreService.afficher();
                List<MarkerData> markers = new ArrayList<>();
                for (Offre o : toutesLesOffres) {
                    double[] coords = geoService.geocode(o.getLocalisation());
                    if (coords != null) markers.add(new MarkerData(o, coords[0], coords[1]));
                }

                double centerLat = 34.0, centerLon = 9.0;
                int zoom = 7;
                if (offreCiblee != null) {
                    double[] c = geoService.geocode(offreCiblee.getLocalisation());
                    if (c != null) { centerLat = c[0]; centerLon = c[1]; zoom = 13; }
                }
                return buildHtml(markers, centerLat, centerLon, zoom);
            }
        };

        task.setOnSucceeded(e -> {
            loadInWebView(task.getValue());
            lblMapInfo.setText(toutesLesOffres.size() + " offres charg\u00e9es \u2014 carte affich\u00e9e");
        });

        task.setOnFailed(e -> {
            lblMapInfo.setText("Erreur : " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadInWebView(String html) {
        if (webViewMap != null) {
            WebEngine engine = webViewMap.getEngine();
            engine.loadContent(html, "text/html");
        }
    }

    @FXML
    private void handleFiltrerRayon() {
        String position = tfPosition.getText();
        String rayonStr = comboRayon.getValue();

        if (position == null || position.isBlank()) {
            showAlert("Veuillez saisir votre position (ex: Tunis, Ariana...)");
            return;
        }

        lblMapInfo.setText("G\u00e9ocodage de " + position + "...");

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                double[] origin = geoService.geocode(position);
                if (origin == null) throw new Exception("Impossible de localiser : " + position);

                double maxKm = "Tout afficher".equals(rayonStr) ? Double.MAX_VALUE : parseRayon(rayonStr);
                List<MarkerData> filteredMarkers = new ArrayList<>();
                int total = 0;

                for (Offre o : toutesLesOffres) {
                    double[] coords = geoService.geocode(o.getLocalisation());
                    if (coords != null) {
                        double dist = GeocodingService.distanceKm(origin[0], origin[1], coords[0], coords[1]);
                        if (dist <= maxKm) {
                            MarkerData md = new MarkerData(o, coords[0], coords[1]);
                            md.distance = dist;
                            filteredMarkers.add(md);
                            total++;
                        }
                    }
                }
                filteredMarkers.sort((a, b) -> Double.compare(a.distance, b.distance));
                return buildHtmlWithOrigin(filteredMarkers, origin[0], origin[1], maxKm, total);
            }
        };

        task.setOnSucceeded(e -> {
            loadInWebView(task.getValue());
            lblResultats.setText("Offres dans un rayon de " + rayonStr + " autour de " + position);
            lblMapInfo.setText("Carte mise \u00e0 jour");
        });

        task.setOnFailed(e -> {
            showAlert("Erreur : " + task.getException().getMessage());
            lblMapInfo.setText("");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleResetCarte() {
        tfPosition.clear();
        comboRayon.setValue("Tout afficher");
        lblResultats.setText("");
        chargerCarte();
    }

    @FXML
    private void retourOffres() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/OffresView.fxml"));
            Parent view = loader.load();
            StackPane contentArea = (StackPane) lblMapInfo.getScene().lookup(".content-area");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
                if (view instanceof javafx.scene.layout.Region region) {
                    region.prefWidthProperty().bind(contentArea.widthProperty());
                    region.prefHeightProperty().bind(contentArea.heightProperty());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==============================================================
    //  HTML GENERATION (Leaflet.js + OpenStreetMap)
    // ==============================================================

    private String buildHtml(List<MarkerData> markers, double centerLat, double centerLon, int zoom) {
        StringBuilder markersJs = new StringBuilder();
        for (MarkerData m : markers) {
            String popup = escapeJs(m.offre.getTitre())
                    + "<br/><b>" + escapeJs(m.offre.getLocalisation()) + "</b>"
                    + "<br/>" + escapeJs(m.offre.getTypeContrat() != null ? m.offre.getTypeContrat() : "")
                    + " - " + escapeJs(m.offre.getModeTravail() != null ? m.offre.getModeTravail() : "")
                    + "<br/>Salaire: " + escapeJs(m.offre.getSalaireRange())
                    + "<br/>Statut: " + escapeJs(m.offre.getStatut() != null ? m.offre.getStatut() : "");

            markersJs.append(String.format(Locale.US,
                    "L.marker([%f, %f]).addTo(map).bindPopup('<b>%s</b>');\n",
                    m.lat, m.lon, popup));
        }
        return leafletTemplate(centerLat, centerLon, zoom, markersJs.toString(), "");
    }

    private String buildHtmlWithOrigin(List<MarkerData> markers, double originLat, double originLon, double maxKm, int count) {
        StringBuilder markersJs = new StringBuilder();

        markersJs.append(String.format(Locale.US,
                "var redIcon = L.icon({iconUrl:'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',"
                + "shadowUrl:'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',"
                + "iconSize:[25,41],iconAnchor:[12,41],popupAnchor:[1,-34],shadowSize:[41,41]});\n"
                + "L.marker([%f, %f], {icon: redIcon}).addTo(map).bindPopup('<b>Votre position</b>').openPopup();\n",
                originLat, originLon));

        if (maxKm < Double.MAX_VALUE) {
            markersJs.append(String.format(Locale.US,
                    "L.circle([%f, %f], {radius: %f, color: '#6366F1', fillColor: '#6366F1', fillOpacity: 0.08, weight: 2}).addTo(map);\n",
                    originLat, originLon, maxKm * 1000));
        }

        for (MarkerData m : markers) {
            String dist = String.format(Locale.US, "%.1f km", m.distance);
            String popup = escapeJs(m.offre.getTitre())
                    + "<br/><b>" + escapeJs(m.offre.getLocalisation()) + "</b>"
                    + "<br/>Distance: " + dist
                    + "<br/>" + escapeJs(m.offre.getSalaireRange())
                    + "<br/>Statut: " + escapeJs(m.offre.getStatut() != null ? m.offre.getStatut() : "");

            markersJs.append(String.format(Locale.US,
                    "L.marker([%f, %f]).addTo(map).bindPopup('<b>%s</b>');\n",
                    m.lat, m.lon, popup));
        }

        int zoom = maxKm <= 10 ? 12 : maxKm <= 20 ? 11 : maxKm <= 50 ? 10 : maxKm <= 100 ? 9 : 7;

        String extra = "<div style='position:fixed;bottom:10px;left:10px;background:white;padding:8px 14px;"
                + "border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.15);z-index:9999;font-size:13px;"
                + "font-family:Segoe UI,sans-serif;'>"
                + "<b>" + count + " offre(s)</b> dans un rayon de <b>"
                + (maxKm < Double.MAX_VALUE ? String.format(Locale.US, "%.0f km", maxKm) : "illimit\u00e9") + "</b></div>";

        return leafletTemplate(originLat, originLon, zoom, markersJs.toString(), extra);
    }

    private String leafletTemplate(double lat, double lon, int zoom, String markersJs, String extraHtml) {
        return "<!DOCTYPE html>\n"
            + "<html lang='fr'>\n<head>\n"
            + "<meta charset='UTF-8'>\n"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n"
            + "<title>TalentFlow - Carte</title>\n"
            + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />\n"
            + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>\n"
            + "<style>\n"
            + "  * { margin:0; padding:0; box-sizing:border-box; }\n"
            + "  html, body { width:100%; height:100%; font-family:'Segoe UI',system-ui,sans-serif; }\n"
            + "  #map { width:100%; height:100%; }\n"
            + "  .leaflet-popup-content { font-family:'Segoe UI',sans-serif; font-size:13px; line-height:1.6; }\n"
            + "  .leaflet-popup-content b { color:#1E293B; }\n"
            + "</style>\n</head>\n<body>\n"
            + extraHtml + "\n"
            + "<div id='map'></div>\n"
            + "<script>\n"
            + String.format(Locale.US, "var map = L.map('map').setView([%f, %f], %d);\n", lat, lon, zoom)
            + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n"
            + "  attribution: '(c) OpenStreetMap',\n"
            + "  maxZoom: 19\n"
            + "}).addTo(map);\n"
            + "L.Icon.Default.mergeOptions({\n"
            + "  iconUrl:'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',\n"
            + "  iconRetinaUrl:'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',\n"
            + "  shadowUrl:'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png'\n"
            + "});\n"
            + markersJs + "\n"
            + "window.addEventListener('resize', function(){ map.invalidateSize(); });\n"
            + "</script>\n</body>\n</html>";
    }

    // ==============================================================
    //  HELPERS
    // ==============================================================

    private double parseRayon(String str) {
        if (str == null) return Double.MAX_VALUE;
        try { return Double.parseDouble(str.replace("km", "").trim()); }
        catch (NumberFormatException e) { return Double.MAX_VALUE; }
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "&quot;")
                .replace("\n", " ")
                .replace("\r", "");
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Carte");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private static class MarkerData {
        final Offre offre;
        final double lat, lon;
        double distance;
        MarkerData(Offre offre, double lat, double lon) {
            this.offre = offre; this.lat = lat; this.lon = lon;
        }
    }
}
