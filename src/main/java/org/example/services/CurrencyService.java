package org.example.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service de conversion multi-devises base sur l'API ExchangeRate-API (gratuit, sans cle).
 */
public class CurrencyService {

    private static final String API_URL = "https://open.er-api.com/v6/latest/";

    public enum Devise {
        TND("TND", "Dinar Tunisien", "DT"),
        EUR("EUR", "Euro", "\u20AC"),
        USD("USD", "Dollar US", "$");

        private final String code;
        private final String label;
        private final String symbole;

        Devise(String code, String label, String symbole) {
            this.code = code;
            this.label = label;
            this.symbole = symbole;
        }

        public String getCode()    { return code; }
        public String getLabel()   { return label; }
        public String getSymbole() { return symbole; }

        @Override
        public String toString() { return symbole + "  " + label + " (" + code + ")"; }
    }

    public static class ConversionResult {
        private final double salaireMinOriginal;
        private final double salaireMaxOriginal;
        private final Devise deviseSource;
        private final Map<Devise, double[]> conversions = new LinkedHashMap<>();
        private String lastUpdate;
        private String errorMessage;

        public ConversionResult(double salaireMin, double salaireMax, Devise source) {
            this.salaireMinOriginal = salaireMin;
            this.salaireMaxOriginal = salaireMax;
            this.deviseSource = source;
        }

        public void addConversion(Devise devise, double min, double max) {
            conversions.put(devise, new double[]{min, max});
        }

        public double getSalaireMinOriginal()          { return salaireMinOriginal; }
        public double getSalaireMaxOriginal()           { return salaireMaxOriginal; }
        public Devise getDeviseSource()                 { return deviseSource; }
        public Map<Devise, double[]> getConversions()   { return conversions; }
        public String getLastUpdate()                   { return lastUpdate; }
        public void setLastUpdate(String lastUpdate)    { this.lastUpdate = lastUpdate; }
        public String getErrorMessage()                 { return errorMessage; }
        public void setErrorMessage(String msg)         { this.errorMessage = msg; }

        public String toDisplayText() {
            StringBuilder sb = new StringBuilder();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                sb.append("ERREUR : ").append(errorMessage).append("\n");
                return sb.toString();
            }
            sb.append("Conversion depuis ").append(deviseSource.getLabel())
              .append(" (").append(deviseSource.getCode()).append(")\n");
            sb.append("---------------------------------------------\n\n");
            sb.append("Salaire d'origine :\n");
            if (salaireMinOriginal == 0 && salaireMaxOriginal == 0) {
                sb.append("  Non precise\n");
                return sb.toString();
            }
            sb.append("  ").append(deviseSource.getCode()).append(" : ")
              .append(fmt(salaireMinOriginal)).append(" - ")
              .append(fmt(salaireMaxOriginal)).append(" ").append(deviseSource.getSymbole()).append("\n\n");
            if (conversions.isEmpty()) {
                sb.append("Aucune conversion disponible.\nVerifiez votre connexion internet.\n");
                return sb.toString();
            }
            sb.append("Equivalences :\n\n");
            for (var entry : conversions.entrySet()) {
                Devise d = entry.getKey();
                double[] vals = entry.getValue();
                sb.append("  ").append(d.getCode()).append(" : ")
                  .append(fmt(vals[0])).append(" - ")
                  .append(fmt(vals[1])).append(" ").append(d.getSymbole()).append("\n");
            }
            if (lastUpdate != null && !lastUpdate.isEmpty()) {
                sb.append("\nTaux du : ").append(lastUpdate);
            }
            return sb.toString();
        }

        private static String fmt(double montant) {
            if (montant == 0) return "0.00";
            return String.format(Locale.US, "%,.2f", montant);
        }
    }

    private final Map<String, Map<String, Double>> ratesCache = new LinkedHashMap<>();
    private final Map<String, String> updateCache = new LinkedHashMap<>();
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 10L * 60L * 1000L;

    public ConversionResult convertir(double salaireMin, double salaireMax, Devise deviseSource) {
        ConversionResult result = new ConversionResult(salaireMin, salaireMax, deviseSource);
        if (salaireMin == 0 && salaireMax == 0) return result;
        try {
            Map<String, Double> rates = fetchRates(deviseSource.getCode());
            if (rates == null || rates.isEmpty()) {
                result.setErrorMessage("Impossible de recuperer les taux de change.");
                return result;
            }
            for (Devise d : Devise.values()) {
                if (d == deviseSource) continue;
                Double rate = rates.get(d.getCode());
                if (rate != null && rate > 0) {
                    result.addConversion(d, salaireMin * rate, salaireMax * rate);
                }
            }
            result.setLastUpdate(updateCache.getOrDefault(deviseSource.getCode(), ""));
        } catch (Exception e) {
            result.setErrorMessage("Erreur : " + e.getMessage());
        }
        return result;
    }

    private Map<String, Double> fetchRates(String baseCurrency) {
        long now = System.currentTimeMillis();
        if (ratesCache.containsKey(baseCurrency) && (now - lastFetchTime) < CACHE_DURATION_MS) {
            return ratesCache.get(baseCurrency);
        }
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(API_URL + baseCurrency).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "TalentFlow/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);
                }
                String json = response.toString();
                Map<String, Double> rates = parseRates(json);
                String lastUpdate = parseLastUpdate(json);
                if (rates != null && !rates.isEmpty()) {
                    ratesCache.put(baseCurrency, rates);
                    updateCache.put(baseCurrency, lastUpdate != null ? lastUpdate : "");
                    lastFetchTime = now;
                }
                return rates;
            }
        } catch (Exception e) {
            System.err.println("[CurrencyService] Erreur reseau : " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return ratesCache.get(baseCurrency);
    }

    private Map<String, Double> parseRates(String json) {
        Map<String, Double> rates = new LinkedHashMap<>();
        try {
            int ratesIdx = json.indexOf("\"rates\"");
            if (ratesIdx == -1) return null;
            int braceStart = json.indexOf("{", ratesIdx);
            if (braceStart == -1) return null;
            int braceEnd = json.indexOf("}", braceStart);
            if (braceEnd == -1) return null;
            String ratesBlock = json.substring(braceStart + 1, braceEnd);
            for (Devise d : Devise.values()) {
                String key = "\"" + d.getCode() + "\":";
                int idx = ratesBlock.indexOf(key);
                if (idx != -1) {
                    int valueStart = idx + key.length();
                    int valueEnd = ratesBlock.indexOf(",", valueStart);
                    if (valueEnd == -1) valueEnd = ratesBlock.length();
                    String valueStr = ratesBlock.substring(valueStart, valueEnd).trim();
                    try { rates.put(d.getCode(), Double.parseDouble(valueStr)); }
                    catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) { return null; }
        return rates.isEmpty() ? null : rates;
    }

    private String parseLastUpdate(String json) {
        String key = "\"time_last_update_utc\":\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
