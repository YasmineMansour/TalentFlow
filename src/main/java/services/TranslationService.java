package services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service de traduction automatique basé sur l'API MyMemory (gratuit, sans clé).
 * Supporte :
 *   - Français  → Anglais  (fr|en)
 *   - Français  → Arabe    (fr|ar)
 *   - Anglais   → Arabe    (en|ar)
 *   - Anglais   → Français (en|fr)
 *   - Arabe     → Français (ar|fr)
 *   - Arabe     → Anglais  (ar|en)
 */
public class TranslationService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";

    // Cache pour éviter les appels réseau répétés
    private final Map<String, String> cache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 200; // limite à 200 entrées max
        }
    };

    /**
     * Langues disponibles pour la traduction.
     */
    public enum Langue {
        FRANCAIS("fr", "Français"),
        ANGLAIS("en", "English"),
        ARABE("ar", "العربية");

        private final String code;
        private final String label;

        Langue(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() { return code; }
        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }
    }

    /**
     * Traduit un texte d'une langue source vers une langue cible.
     *
     * @param texte  Le texte à traduire
     * @param source La langue source (ex: Langue.FRANCAIS)
     * @param cible  La langue cible (ex: Langue.ANGLAIS)
     * @return Le texte traduit, ou null en cas d'erreur
     */
    public String traduire(String texte, Langue source, Langue cible) {
        if (texte == null || texte.isBlank()) return "";
        if (source == cible) return texte;

        String langPair = source.getCode() + "|" + cible.getCode();
        String cacheKey = langPair + ":" + texte.trim();

        // Vérifier le cache
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        try {
            String encoded = URLEncoder.encode(texte.trim(), StandardCharsets.UTF_8);
            String urlStr = API_URL + "?q=" + encoded + "&langpair=" + langPair;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "TalentFlow/1.0 (student-project)");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Translation API HTTP " + responseCode);
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            String json = sb.toString();
            String translated = extraireTraduction(json);

            if (translated != null && !translated.isBlank()) {
                cache.put(cacheKey, translated);
                return translated;
            }

            return null;

        } catch (Exception e) {
            System.err.println("Erreur de traduction : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Traduit tous les champs textuels d'une offre.
     *
     * @param titre        Titre de l'offre
     * @param description  Description de l'offre
     * @param localisation Localisation de l'offre
     * @param source       Langue source
     * @param cible        Langue cible
     * @return Un tableau [titreT, descriptionT, localisationT] ou null en cas d'erreur
     */
    public String[] traduireOffre(String titre, String description, String localisation,
                                   Langue source, Langue cible) {
        String titreT = traduire(titre, source, cible);
        String descT = traduire(description, source, cible);
        String locT = traduire(localisation, source, cible);

        if (titreT == null && descT == null && locT == null) return null;

        return new String[]{
                titreT != null ? titreT : titre,
                descT != null ? descT : description,
                locT != null ? locT : localisation
        };
    }

    /**
     * Extrait le texte traduit du JSON de réponse MyMemory.
     * Format : {"responseData":{"translatedText":"...","match":...},...}
     */
    private String extraireTraduction(String json) {
        try {
            // Chercher "translatedText":"..."
            String marker = "\"translatedText\":\"";
            int start = json.indexOf(marker);
            if (start == -1) return null;

            start += marker.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return null;

            String result = json.substring(start, end);

            // Décoder les séquences unicode (\\uXXXX)
            result = decodeUnicode(result);

            // Vérifier que la réponse n'est pas une erreur
            if (result.toUpperCase().startsWith("MYMEMORY WARNING")
                    || result.toUpperCase().contains("PLEASE PROVIDE")
                    || result.isEmpty()) {
                return null;
            }

            return result;

        } catch (Exception e) {
            System.err.println("Erreur parsing JSON traduction : " + e.getMessage());
            return null;
        }
    }

    /**
     * Décode les séquences Unicode \\uXXXX dans une chaîne.
     */
    private String decodeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (i + 5 < input.length() && input.charAt(i) == '\\' && input.charAt(i + 1) == 'u') {
                try {
                    String hex = input.substring(i + 2, i + 6);
                    sb.append((char) Integer.parseInt(hex, 16));
                    i += 6;
                    continue;
                } catch (NumberFormatException e) {
                    // pas une séquence unicode valide
                }
            }
            sb.append(input.charAt(i));
            i++;
        }
        return sb.toString();
    }
}
