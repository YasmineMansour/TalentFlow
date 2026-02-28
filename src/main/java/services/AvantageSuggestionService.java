package services;

import entities.Avantage;
import entities.Offre;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IA – Générateur intelligent d'avantages.
 * Analyse les caractéristiques d'une offre (titre, description, type de contrat,
 * mode de travail, fourchette salariale) et suggère des avantages pertinents
 * classés par catégorie (FINANCIER, BIEN_ETRE, MATERIEL, AUTRE).
 *
 * Moteur 100 % local basé sur des règles pondérées et un dictionnaire de mots-clés.
 */
public class AvantageSuggestionService {

    // ─── Structure interne pour une suggestion pondérée ───────────────
    private static class SuggestionCandidate {
        final String nom;
        final String description;
        final String type;
        int score; // plus le score est élevé, plus la suggestion est pertinente

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

    // ─── Dictionnaire de mots-clés → domaines ───────────────────────
    private static final Map<String, String[]> DOMAIN_KEYWORDS = new LinkedHashMap<>();
    static {
        DOMAIN_KEYWORDS.put("tech",        new String[]{"développeur", "developer", "dev", "informatique", "software", "fullstack",
                "full-stack", "backend", "frontend", "devops", "cloud", "data", "machine learning",
                "intelligence artificielle", "ia", "ai", "programmeur", "ingénieur logiciel",
                "architecte", "sre", "sysadmin", "java", "python", "javascript", ".net", "react",
                "angular", "mobile", "android", "ios", "cybersécurité", "sécurité informatique"});
        DOMAIN_KEYWORDS.put("management",  new String[]{"manager", "directeur", "chef de projet", "responsable", "lead",
                "team lead", "head of", "coordinateur", "superviseur", "chef d'équipe", "cto", "cio"});
        DOMAIN_KEYWORDS.put("commercial",  new String[]{"commercial", "vente", "sales", "business developer", "account",
                "relation client", "négociateur", "conseiller", "chargé de clientèle"});
        DOMAIN_KEYWORDS.put("design",      new String[]{"designer", "ux", "ui", "graphiste", "design", "créatif",
                "directeur artistique", "webdesigner", "motion"});
        DOMAIN_KEYWORDS.put("rh",          new String[]{"ressources humaines", "rh", "recruteur", "recrutement", "talent",
                "paie", "formation", "gpec"});
        DOMAIN_KEYWORDS.put("finance",     new String[]{"comptable", "financier", "finance", "audit", "contrôle de gestion",
                "trésorerie", "analyste financier", "expert-comptable"});
        DOMAIN_KEYWORDS.put("marketing",   new String[]{"marketing", "communication", "community manager", "seo", "sem",
                "content", "brand", "digital marketing", "chef de produit"});
        DOMAIN_KEYWORDS.put("international", new String[]{"international", "export", "bilingue", "multilingue", "anglophone",
                "francophone", "global", "worldwide", "expatrié"});
        DOMAIN_KEYWORDS.put("senior",      new String[]{"senior", "sénior", "expert", "confirmé", "expérimenté", "principal",
                "staff", "distinguished"});
        DOMAIN_KEYWORDS.put("junior",      new String[]{"junior", "débutant", "stagiaire", "alternant", "apprenti", "stage",
                "alternance", "entrée"});
    }

    // ─── API publique ────────────────────────────────────────────────

    /**
     * Génère une liste d'avantages suggérés pour l'offre donnée.
     *
     * @param offre   L'offre à analyser
     * @param maxSugg Nombre maximum de suggestions à retourner
     * @return Liste de {@link Avantage} suggérés, triés par pertinence décroissante
     */
    public List<Avantage> suggerer(Offre offre, int maxSugg) {
        if (offre == null) return List.of();

        // Extraire le contexte de l'offre
        String titre       = safe(offre.getTitre());
        String description = safe(offre.getDescription());
        String contrat     = safe(offre.getTypeContrat());
        String mode        = safe(offre.getModeTravail());
        double salMin      = offre.getSalaireMin();
        double salMax      = offre.getSalaireMax();
        String fullText    = (titre + " " + description).toLowerCase();

        // Détecter les domaines
        Map<String, Integer> domainScores = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : DOMAIN_KEYWORDS.entrySet()) {
            int hits = 0;
            for (String kw : entry.getValue()) {
                if (fullText.contains(kw.toLowerCase())) hits++;
            }
            if (hits > 0) domainScores.put(entry.getKey(), hits);
        }

        // Construire la liste brute des suggestions
        List<SuggestionCandidate> candidates = new ArrayList<>();

        // ── 1. Suggestions basées sur le MODE DE TRAVAIL ──────────────
        if (contientUnDe(mode, "télétravail", "remote", "teletravail", "hybride", "hybrid")) {
            candidates.add(new SuggestionCandidate(
                    "Indemnité télétravail",
                    "Allocation mensuelle pour couvrir les frais liés au travail à domicile (internet, électricité, etc.)",
                    "FINANCIER", 90));
            candidates.add(new SuggestionCandidate(
                    "Equipement bureau à domicile",
                    "Budget pour l'achat d'un bureau ergonomique, écran, chaise, clavier et souris",
                    "MATERIEL", 85));
            candidates.add(new SuggestionCandidate(
                    "Horaires flexibles",
                    "Liberté d'organiser ses heures de travail pour un meilleur équilibre vie pro/perso",
                    "BIEN_ETRE", 80));
        }

        if (contientUnDe(mode, "présentiel", "presentiel", "sur site", "on-site", "onsite")) {
            candidates.add(new SuggestionCandidate(
                    "Titre de transport",
                    "Prise en charge à 100% de l'abonnement transport en commun",
                    "FINANCIER", 80));
            candidates.add(new SuggestionCandidate(
                    "Place de parking",
                    "Place de stationnement réservée sur le site de l'entreprise",
                    "MATERIEL", 60));
            candidates.add(new SuggestionCandidate(
                    "Cantine / Tickets restaurant",
                    "Prise en charge des repas via tickets restaurant ou cantine d'entreprise subventionnée",
                    "FINANCIER", 75));
        }

        // Hybride = bénéficie des deux
        if (contientUnDe(mode, "hybride", "hybrid")) {
            candidates.add(new SuggestionCandidate(
                    "Journées au choix",
                    "Flexibilité dans le choix des jours de présence et de télétravail",
                    "BIEN_ETRE", 70));
        }

        // ── 2. Suggestions basées sur le TYPE DE CONTRAT ──────────────
        if (contientUnDe(contrat, "cdi")) {
            candidates.add(new SuggestionCandidate(
                    "Plan d'épargne entreprise",
                    "PEE avec abondement de l'entreprise pour faire fructifier votre épargne",
                    "FINANCIER", 70));
            candidates.add(new SuggestionCandidate(
                    "Mutuelle premium",
                    "Couverture santé complète avec prise en charge à 80% par l'employeur",
                    "BIEN_ETRE", 85));
            candidates.add(new SuggestionCandidate(
                    "Congés supplémentaires",
                    "Jours de congés additionnels au-delà du minimum légal (RTT, ancienneté)",
                    "BIEN_ETRE", 65));
        }

        if (contientUnDe(contrat, "cdd", "intérim", "interim", "temporaire")) {
            candidates.add(new SuggestionCandidate(
                    "Prime de fin de contrat",
                    "Indemnité compensatrice versée à l'échéance du contrat",
                    "FINANCIER", 75));
        }

        if (contientUnDe(contrat, "stage", "alternance", "apprentissage")) {
            candidates.add(new SuggestionCandidate(
                    "Budget formation certifiante",
                    "Accès à des formations et certifications professionnelles pendant le contrat",
                    "AUTRE", 80));
            candidates.add(new SuggestionCandidate(
                    "Mentorat personnalisé",
                    "Accompagnement par un mentor expérimenté pour accélérer votre montée en compétences",
                    "BIEN_ETRE", 85));
            candidates.add(new SuggestionCandidate(
                    "Possibilité d'embauche",
                    "Perspective de CDI à l'issue du stage ou de l'alternance selon les performances",
                    "AUTRE", 70));
        }

        if (contientUnDe(contrat, "freelance", "indépendant", "consultant", "portage")) {
            candidates.add(new SuggestionCandidate(
                    "Tarif journalier compétitif",
                    "Rémunération au TJM attractif aligné sur le marché",
                    "FINANCIER", 80));
        }

        // ── 3. Suggestions basées sur le DOMAINE DÉTECTÉ ──────────────
        if (domainScores.containsKey("tech")) {
            candidates.add(new SuggestionCandidate(
                    "Matériel informatique dernière génération",
                    "Ordinateur portable performant, double écran, et périphériques de qualité au choix",
                    "MATERIEL", 90));
            candidates.add(new SuggestionCandidate(
                    "Budget conférences tech",
                    "Participation annuelle à des conférences et meetups techniques (Devoxx, AWS Summit, etc.)",
                    "AUTRE", 70));
            candidates.add(new SuggestionCandidate(
                    "Licence outils de développement",
                    "Accès aux IDE, outils et services cloud premium (JetBrains, GitHub Copilot, etc.)",
                    "MATERIEL", 65));
            candidates.add(new SuggestionCandidate(
                    "Veille technologique",
                    "Temps dédié chaque semaine à la veille, l'apprentissage et les side-projects internes",
                    "BIEN_ETRE", 60));
        }

        if (domainScores.containsKey("management")) {
            candidates.add(new SuggestionCandidate(
                    "Véhicule de fonction",
                    "Voiture de société ou indemnité kilométrique pour les déplacements professionnels",
                    "MATERIEL", 75));
            candidates.add(new SuggestionCandidate(
                    "Coaching leadership",
                    "Programme de coaching individuel pour développer vos compétences managériales",
                    "BIEN_ETRE", 70));
            candidates.add(new SuggestionCandidate(
                    "Carte de représentation",
                    "Budget repas et représentation pour les rendez-vous clients et partenaires",
                    "FINANCIER", 65));
        }

        if (domainScores.containsKey("commercial")) {
            candidates.add(new SuggestionCandidate(
                    "Commissions sur ventes",
                    "Variable déplafonné basé sur les performances commerciales",
                    "FINANCIER", 90));
            candidates.add(new SuggestionCandidate(
                    "Téléphone et forfait pro",
                    "Smartphone dernière génération avec forfait illimité inclus",
                    "MATERIEL", 70));
        }

        if (domainScores.containsKey("design")) {
            candidates.add(new SuggestionCandidate(
                    "Licence suite créative",
                    "Accès complet à Adobe Creative Cloud (Photoshop, Illustrator, Figma Pro, etc.)",
                    "MATERIEL", 80));
            candidates.add(new SuggestionCandidate(
                    "Tablette graphique",
                    "Wacom ou iPad Pro avec Apple Pencil fourni pour la création",
                    "MATERIEL", 70));
        }

        if (domainScores.containsKey("international")) {
            candidates.add(new SuggestionCandidate(
                    "Cours de langues",
                    "Formation linguistique prise en charge (anglais, espagnol, allemand, etc.)",
                    "AUTRE", 75));
            candidates.add(new SuggestionCandidate(
                    "Prime d'expatriation",
                    "Indemnité pour les missions à l'étranger couvrant logement et frais de vie",
                    "FINANCIER", 70));
        }

        if (domainScores.containsKey("rh")) {
            candidates.add(new SuggestionCandidate(
                    "Accès plateforme e-learning",
                    "Abonnement LinkedIn Learning, Coursera ou OpenClassrooms pour la formation continue",
                    "AUTRE", 65));
        }

        if (domainScores.containsKey("finance")) {
            candidates.add(new SuggestionCandidate(
                    "Prime d'objectif trimestrielle",
                    "Bonus trimestriel basé sur l'atteinte des KPIs financiers de l'équipe",
                    "FINANCIER", 75));
        }

        if (domainScores.containsKey("marketing")) {
            candidates.add(new SuggestionCandidate(
                    "Budget outils marketing",
                    "Accès aux outils premium : SEMrush, HubSpot, Hootsuite, Canva Pro",
                    "MATERIEL", 65));
        }

        // ── 4. Suggestions basées sur le NIVEAU DE SALAIRE ────────────
        double salaireMoyen = (salMin + salMax) / 2.0;

        if (salaireMoyen >= 5000) { // Poste très bien rémunéré
            candidates.add(new SuggestionCandidate(
                    "Stock-options / Actions gratuites",
                    "Participation au capital de l'entreprise via un plan de stock-options ou d'actions gratuites",
                    "FINANCIER", 85));
            candidates.add(new SuggestionCandidate(
                    "Assurance vie premium",
                    "Contrat d'assurance vie groupe avec conditions préférentielles",
                    "FINANCIER", 60));
            candidates.add(new SuggestionCandidate(
                    "Conciergerie d'entreprise",
                    "Services de conciergerie (pressing, courses, démarches administratives) sur le lieu de travail",
                    "BIEN_ETRE", 55));
        } else if (salaireMoyen >= 2500) { // Salaire intermédiaire
            candidates.add(new SuggestionCandidate(
                    "Prime de performance",
                    "Bonus annuel basé sur l'évaluation des performances individuelles et collectives",
                    "FINANCIER", 80));
            candidates.add(new SuggestionCandidate(
                    "Chèques cadeaux",
                    "Chèques cadeaux pour les événements (Noël, rentrée scolaire, naissance, mariage)",
                    "FINANCIER", 55));
        } else if (salaireMoyen > 0) { // Salaire plus modeste
            candidates.add(new SuggestionCandidate(
                    "Aide au logement",
                    "Participation de l'entreprise aux frais de logement (Action Logement, prime déménagement)",
                    "FINANCIER", 70));
            candidates.add(new SuggestionCandidate(
                    "Panier repas",
                    "Indemnité journalière pour les repas pris sur le lieu de travail",
                    "FINANCIER", 65));
        }

        // ── 5. Suggestions basées sur le NIVEAU D'EXPÉRIENCE ─────────
        if (domainScores.containsKey("senior")) {
            candidates.add(new SuggestionCandidate(
                    "Sabbatique rémunérée",
                    "Possibilité de prendre un congé sabbatique après 3 ans d'ancienneté, partiellement rémunéré",
                    "BIEN_ETRE", 60));
            candidates.add(new SuggestionCandidate(
                    "Budget conférences speaker",
                    "Prise en charge complète pour intervenir comme speaker dans des conférences de votre domaine",
                    "AUTRE", 55));
        }

        if (domainScores.containsKey("junior")) {
            candidates.add(new SuggestionCandidate(
                    "Programme d'onboarding structuré",
                    "Parcours d'intégration complet avec formations, buddy system et évaluations régulières",
                    "AUTRE", 75));
            candidates.add(new SuggestionCandidate(
                    "Aide à la mobilité",
                    "Prise en charge des frais de déménagement si relocation nécessaire",
                    "FINANCIER", 60));
        }

        // ── 6. Suggestions universelles (toujours pertinentes) ───────
        candidates.add(new SuggestionCandidate(
                "Abonnement sport / bien-être",
                "Prise en charge partielle d'un abonnement salle de sport, yoga, ou application bien-être",
                "BIEN_ETRE", 50));
        candidates.add(new SuggestionCandidate(
                "Téléconsultation médicale",
                "Accès gratuit à un service de téléconsultation médicale 24/7 pour vous et votre famille",
                "BIEN_ETRE", 45));
        candidates.add(new SuggestionCandidate(
                "Team building",
                "Activités régulières de cohésion d'équipe et événements d'entreprise",
                "BIEN_ETRE", 40));

        // ── Dédupliquer par nom ──────────────────────────────────────
        Map<String, SuggestionCandidate> dedup = new LinkedHashMap<>();
        for (SuggestionCandidate c : candidates) {
            String key = c.nom.toLowerCase();
            if (dedup.containsKey(key)) {
                // Garder le score le plus élevé
                if (c.score > dedup.get(key).score) {
                    dedup.put(key, c);
                }
            } else {
                dedup.put(key, c);
            }
        }

        // ── Trier par score décroissant et limiter ───────────────────
        List<Avantage> result = dedup.values().stream()
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(maxSugg)
                .map(c -> c.toAvantage(offre.getId()))
                .toList();

        return result;
    }

    /**
     * Version par défaut qui retourne max 8 suggestions.
     */
    public List<Avantage> suggerer(Offre offre) {
        return suggerer(offre, 8);
    }

    // ─── Utilitaires ─────────────────────────────────────────────────

    private static String safe(String s) {
        return s != null ? s.trim() : "";
    }

    private static boolean contientUnDe(String texte, String... motsCles) {
        if (texte == null || texte.isBlank()) return false;
        String lower = texte.toLowerCase();
        for (String mc : motsCles) {
            if (lower.contains(mc.toLowerCase())) return true;
        }
        return false;
    }
}
