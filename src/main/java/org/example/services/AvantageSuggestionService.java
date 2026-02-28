package org.example.services;

import org.example.model.Avantage;
import org.example.model.Offre;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IA - Generateur intelligent d'avantages.
 * Analyse les caracteristiques d'une offre et suggere des avantages pertinents
 * classes par categorie (FINANCIER, BIEN_ETRE, MATERIEL, AUTRE).
 */
public class AvantageSuggestionService {

    private static class SuggestionCandidate {
        final String nom;
        final String description;
        final String type;
        int score;

        SuggestionCandidate(String nom, String description, String type, int score) {
            this.nom = nom;
            this.description = description;
            this.type = type;
            this.score = score;
        }

        Avantage toAvantage(int offreId) {
            return new Avantage(nom, description, type, offreId);
        }
    }

    private static final Map<String, String[]> DOMAIN_KEYWORDS = new LinkedHashMap<>();
    static {
        DOMAIN_KEYWORDS.put("tech",        new String[]{"developpeur", "developer", "dev", "informatique", "software", "fullstack",
                "full-stack", "backend", "frontend", "devops", "cloud", "data", "machine learning",
                "intelligence artificielle", "ia", "ai", "programmeur", "ingenieur logiciel",
                "architecte", "sre", "sysadmin", "java", "python", "javascript", ".net", "react",
                "angular", "mobile", "android", "ios", "cybersecurite", "securite informatique"});
        DOMAIN_KEYWORDS.put("management",  new String[]{"manager", "directeur", "chef de projet", "responsable", "lead",
                "team lead", "head of", "coordinateur", "superviseur", "chef d'equipe", "cto", "cio"});
        DOMAIN_KEYWORDS.put("commercial",  new String[]{"commercial", "vente", "sales", "business developer", "account",
                "relation client", "negociateur", "conseiller", "charge de clientele"});
        DOMAIN_KEYWORDS.put("design",      new String[]{"designer", "ux", "ui", "graphiste", "design", "creatif",
                "directeur artistique", "webdesigner", "motion"});
        DOMAIN_KEYWORDS.put("rh",          new String[]{"ressources humaines", "rh", "recruteur", "recrutement", "talent",
                "paie", "formation", "gpec"});
        DOMAIN_KEYWORDS.put("finance",     new String[]{"comptable", "financier", "finance", "audit", "controle de gestion",
                "tresorerie", "analyste financier", "expert-comptable"});
        DOMAIN_KEYWORDS.put("marketing",   new String[]{"marketing", "communication", "community manager", "seo", "sem",
                "content", "brand", "digital marketing", "chef de produit"});
        DOMAIN_KEYWORDS.put("international", new String[]{"international", "export", "bilingue", "multilingue", "anglophone",
                "francophone", "global", "worldwide", "expatrie"});
        DOMAIN_KEYWORDS.put("senior",      new String[]{"senior", "expert", "confirme", "experimente", "principal", "staff", "distinguished"});
        DOMAIN_KEYWORDS.put("junior",      new String[]{"junior", "debutant", "stagiaire", "alternant", "apprenti", "stage", "alternance", "entree"});
    }

    public List<Avantage> suggerer(Offre offre, int maxSugg) {
        if (offre == null) return List.of();

        String titre       = safe(offre.getTitre());
        String description = safe(offre.getDescription());
        String contrat     = safe(offre.getTypeContrat());
        String mode        = safe(offre.getModeTravail());
        double salMin      = offre.getSalaireMin();
        double salMax      = offre.getSalaireMax();
        String fullText    = (titre + " " + description).toLowerCase();

        Map<String, Integer> domainScores = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : DOMAIN_KEYWORDS.entrySet()) {
            int hits = 0;
            for (String kw : entry.getValue()) {
                if (fullText.contains(kw.toLowerCase())) hits++;
            }
            if (hits > 0) domainScores.put(entry.getKey(), hits);
        }

        List<SuggestionCandidate> candidates = new ArrayList<>();

        // Mode de travail
        if (contientUnDe(mode, "teletravail", "remote", "hybride", "hybrid")) {
            candidates.add(new SuggestionCandidate("Indemnite teletravail", "Allocation mensuelle pour couvrir les frais lies au travail a domicile", "FINANCIER", 90));
            candidates.add(new SuggestionCandidate("Equipement bureau a domicile", "Budget pour l'achat d'un bureau ergonomique, ecran, chaise", "MATERIEL", 85));
            candidates.add(new SuggestionCandidate("Horaires flexibles", "Liberte d'organiser ses heures de travail", "BIEN_ETRE", 80));
        }
        if (contientUnDe(mode, "presentiel", "sur site", "on-site", "onsite", "on_site")) {
            candidates.add(new SuggestionCandidate("Titre de transport", "Prise en charge a 100% de l'abonnement transport en commun", "FINANCIER", 80));
            candidates.add(new SuggestionCandidate("Place de parking", "Place de stationnement reservee", "MATERIEL", 60));
            candidates.add(new SuggestionCandidate("Cantine / Tickets restaurant", "Prise en charge des repas", "FINANCIER", 75));
        }
        if (contientUnDe(mode, "hybride", "hybrid")) {
            candidates.add(new SuggestionCandidate("Journees au choix", "Flexibilite dans le choix des jours de presence et de teletravail", "BIEN_ETRE", 70));
        }

        // Type de contrat
        if (contientUnDe(contrat, "cdi")) {
            candidates.add(new SuggestionCandidate("Plan d'epargne entreprise", "PEE avec abondement de l'entreprise", "FINANCIER", 70));
            candidates.add(new SuggestionCandidate("Mutuelle premium", "Couverture sante complete avec prise en charge a 80%", "BIEN_ETRE", 85));
            candidates.add(new SuggestionCandidate("Conges supplementaires", "Jours de conges additionnels (RTT, anciennete)", "BIEN_ETRE", 65));
        }
        if (contientUnDe(contrat, "cdd", "interim", "temporaire")) {
            candidates.add(new SuggestionCandidate("Prime de fin de contrat", "Indemnite compensatrice versee a l'echeance du contrat", "FINANCIER", 75));
        }
        if (contientUnDe(contrat, "stage", "alternance", "apprentissage")) {
            candidates.add(new SuggestionCandidate("Budget formation certifiante", "Acces a des formations et certifications professionnelles", "AUTRE", 80));
            candidates.add(new SuggestionCandidate("Mentorat personnalise", "Accompagnement par un mentor experimente", "BIEN_ETRE", 85));
            candidates.add(new SuggestionCandidate("Possibilite d'embauche", "Perspective de CDI a l'issue du stage ou de l'alternance", "AUTRE", 70));
        }
        if (contientUnDe(contrat, "freelance", "independant", "consultant", "portage")) {
            candidates.add(new SuggestionCandidate("Tarif journalier competitif", "Remuneration au TJM attractif aligne sur le marche", "FINANCIER", 80));
        }

        // Domaines
        if (domainScores.containsKey("tech")) {
            candidates.add(new SuggestionCandidate("Materiel informatique derniere generation", "Ordinateur portable performant, double ecran", "MATERIEL", 90));
            candidates.add(new SuggestionCandidate("Budget conferences tech", "Participation annuelle a des conferences techniques", "AUTRE", 70));
            candidates.add(new SuggestionCandidate("Licence outils de developpement", "Acces aux IDE et outils premium", "MATERIEL", 65));
            candidates.add(new SuggestionCandidate("Veille technologique", "Temps dedie a la veille et l'apprentissage", "BIEN_ETRE", 60));
        }
        if (domainScores.containsKey("management")) {
            candidates.add(new SuggestionCandidate("Vehicule de fonction", "Voiture de societe ou indemnite kilometrique", "MATERIEL", 75));
            candidates.add(new SuggestionCandidate("Coaching leadership", "Programme de coaching individuel", "BIEN_ETRE", 70));
            candidates.add(new SuggestionCandidate("Carte de representation", "Budget repas et representation", "FINANCIER", 65));
        }
        if (domainScores.containsKey("commercial")) {
            candidates.add(new SuggestionCandidate("Commissions sur ventes", "Variable deplafonne base sur les performances", "FINANCIER", 90));
            candidates.add(new SuggestionCandidate("Telephone et forfait pro", "Smartphone avec forfait illimite inclus", "MATERIEL", 70));
        }
        if (domainScores.containsKey("design")) {
            candidates.add(new SuggestionCandidate("Licence suite creative", "Acces complet a Adobe Creative Cloud, Figma Pro", "MATERIEL", 80));
            candidates.add(new SuggestionCandidate("Tablette graphique", "Wacom ou iPad Pro fourni pour la creation", "MATERIEL", 70));
        }
        if (domainScores.containsKey("international")) {
            candidates.add(new SuggestionCandidate("Cours de langues", "Formation linguistique prise en charge", "AUTRE", 75));
            candidates.add(new SuggestionCandidate("Prime d'expatriation", "Indemnite pour les missions a l'etranger", "FINANCIER", 70));
        }
        if (domainScores.containsKey("rh")) {
            candidates.add(new SuggestionCandidate("Acces plateforme e-learning", "Abonnement LinkedIn Learning, Coursera", "AUTRE", 65));
        }
        if (domainScores.containsKey("finance")) {
            candidates.add(new SuggestionCandidate("Prime d'objectif trimestrielle", "Bonus trimestriel base sur les KPIs financiers", "FINANCIER", 75));
        }
        if (domainScores.containsKey("marketing")) {
            candidates.add(new SuggestionCandidate("Budget outils marketing", "Acces aux outils premium : SEMrush, HubSpot", "MATERIEL", 65));
        }

        // Salaire
        double salaireMoyen = (salMin + salMax) / 2.0;
        if (salaireMoyen >= 5000) {
            candidates.add(new SuggestionCandidate("Stock-options / Actions gratuites", "Participation au capital de l'entreprise", "FINANCIER", 85));
            candidates.add(new SuggestionCandidate("Assurance vie premium", "Contrat d'assurance vie groupe", "FINANCIER", 60));
            candidates.add(new SuggestionCandidate("Conciergerie d'entreprise", "Services de conciergerie sur le lieu de travail", "BIEN_ETRE", 55));
        } else if (salaireMoyen >= 2500) {
            candidates.add(new SuggestionCandidate("Prime de performance", "Bonus annuel base sur l'evaluation des performances", "FINANCIER", 80));
            candidates.add(new SuggestionCandidate("Cheques cadeaux", "Cheques cadeaux pour les evenements", "FINANCIER", 55));
        } else if (salaireMoyen > 0) {
            candidates.add(new SuggestionCandidate("Aide au logement", "Participation de l'entreprise aux frais de logement", "FINANCIER", 70));
            candidates.add(new SuggestionCandidate("Panier repas", "Indemnite journaliere pour les repas", "FINANCIER", 65));
        }

        // Experience
        if (domainScores.containsKey("senior")) {
            candidates.add(new SuggestionCandidate("Sabbatique remuneree", "Possibilite de conge sabbatique apres 3 ans", "BIEN_ETRE", 60));
            candidates.add(new SuggestionCandidate("Budget conferences speaker", "Prise en charge pour intervenir comme speaker", "AUTRE", 55));
        }
        if (domainScores.containsKey("junior")) {
            candidates.add(new SuggestionCandidate("Programme d'onboarding structure", "Parcours d'integration complet", "AUTRE", 75));
            candidates.add(new SuggestionCandidate("Aide a la mobilite", "Prise en charge des frais de demenagement", "FINANCIER", 60));
        }

        // Universelles
        candidates.add(new SuggestionCandidate("Abonnement sport / bien-etre", "Prise en charge partielle d'un abonnement sport", "BIEN_ETRE", 50));
        candidates.add(new SuggestionCandidate("Teleconsultation medicale", "Acces gratuit a un service de teleconsultation 24/7", "BIEN_ETRE", 45));
        candidates.add(new SuggestionCandidate("Team building", "Activites regulieres de cohesion d'equipe", "BIEN_ETRE", 40));

        // Dedupliquer
        Map<String, SuggestionCandidate> dedup = new LinkedHashMap<>();
        for (SuggestionCandidate c : candidates) {
            String key = c.nom.toLowerCase();
            if (dedup.containsKey(key)) {
                if (c.score > dedup.get(key).score) dedup.put(key, c);
            } else {
                dedup.put(key, c);
            }
        }

        return dedup.values().stream()
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(maxSugg)
                .map(c -> c.toAvantage(offre.getId()))
                .toList();
    }

    public List<Avantage> suggerer(Offre offre) {
        return suggerer(offre, 8);
    }

    private static String safe(String s) { return s != null ? s.trim() : ""; }

    private static boolean contientUnDe(String texte, String... motsCles) {
        if (texte == null || texte.isBlank()) return false;
        String lower = texte.toLowerCase();
        for (String mc : motsCles) {
            if (lower.contains(mc.toLowerCase())) return true;
        }
        return false;
    }
}
