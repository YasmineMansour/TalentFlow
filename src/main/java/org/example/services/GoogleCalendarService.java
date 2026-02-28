package org.example.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import org.example.model.Entretien;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service d'intégration Google Calendar API v3.
 *
 * Prérequis :
 *  1. Créer un projet sur https://console.cloud.google.com
 *  2. Activer "Google Calendar API"
 *  3. Créer des identifiants OAuth 2.0 (type "Application de bureau")
 *  4. Télécharger le JSON et le placer dans src/main/resources/credentials.json
 */
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "TalentFlow - Gestion Entretiens";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final String TOKENS_DIRECTORY = "tokens";
    private static final String CREDENTIALS_FILE = "/credentials.json";

    private static final List<String> SCOPES =
            Collections.singletonList(CalendarScopes.CALENDAR);

    private static final String TIMEZONE = "Africa/Tunis";

    private Calendar calendarService;

    // ════════════════════════ SINGLETON ════════════════════════

    private static GoogleCalendarService instance;

    public static synchronized GoogleCalendarService getInstance() {
        if (instance == null) {
            instance = new GoogleCalendarService();
        }
        return instance;
    }

    private GoogleCalendarService() {}

    // ════════════════════════ AUTH OAuth 2.0 ════════════════════════

    private Credential authorize(NetHttpTransport httpTransport) throws IOException {

        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE);

        if (in == null) {
            throw new FileNotFoundException(
                    "Fichier credentials.json introuvable dans resources/.\n" +
                    "Téléchargez-le depuis Google Cloud Console -> APIs -> Identifiants -> OAuth 2.0"
            );
        }

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private Calendar getService() throws IOException, GeneralSecurityException {
        if (calendarService == null) {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = authorize(httpTransport);

            calendarService = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            System.out.println("Google Calendar API connectée.");
        }
        return calendarService;
    }

    // ════════════════════════ VÉRIFICATION ════════════════════════

    public boolean isConfigured() {
        return GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE) != null;
    }

    public boolean isAuthenticated() {
        File tokenDir = new File(TOKENS_DIRECTORY);
        return tokenDir.exists() && tokenDir.list() != null && tokenDir.list().length > 0;
    }

    // ════════════════════════ CRÉER ÉVÉNEMENT ════════════════════════

    public String creerEvenement(Entretien entretien)
            throws IOException, GeneralSecurityException {

        Calendar service = getService();

        Event event = new Event();

        String titre = "Entretien " + entretien.getType()
                + (entretien.getEmailCandidat() != null
                    ? " — " + entretien.getEmailCandidat() : "");
        event.setSummary(titre);

        StringBuilder desc = new StringBuilder();
        desc.append("Entretien planifié via TalentFlow\n\n");
        desc.append("Candidat : ").append(entretien.getEmailCandidat()).append("\n");
        desc.append("Type : ").append(entretien.getType()).append("\n");

        if (entretien.getLieu() != null && !entretien.getLieu().isBlank()) {
            desc.append("Lieu : ").append(entretien.getLieu()).append("\n");
        }
        if (entretien.getLien() != null && !entretien.getLien().isBlank()) {
            desc.append("Lien : ").append(entretien.getLien()).append("\n");
        }

        event.setDescription(desc.toString());

        if ("EN_LIGNE".equals(entretien.getType())) {
            event.setLocation("En ligne (Google Meet)");
        } else if (entretien.getLieu() != null && !entretien.getLieu().isBlank()) {
            event.setLocation(entretien.getLieu());
        }

        LocalDateTime start = entretien.getDateEntretien();
        LocalDateTime end = start.plusHours(1);

        ZonedDateTime startZoned = start.atZone(ZoneId.of(TIMEZONE));
        ZonedDateTime endZoned = end.atZone(ZoneId.of(TIMEZONE));

        event.setStart(new EventDateTime()
                .setDateTime(new DateTime(startZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .setTimeZone(TIMEZONE));

        event.setEnd(new EventDateTime()
                .setDateTime(new DateTime(endZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .setTimeZone(TIMEZONE));

        if (entretien.getEmailCandidat() != null && !entretien.getEmailCandidat().isBlank()) {
            EventAttendee attendee = new EventAttendee();
            attendee.setEmail(entretien.getEmailCandidat());
            attendee.setDisplayName("Candidat");
            event.setAttendees(Collections.singletonList(attendee));

            event.setGuestsCanModify(false);
            event.setGuestsCanInviteOthers(false);
        }

        ConferenceData conferenceData = new ConferenceData();
        CreateConferenceRequest conferenceRequest = new CreateConferenceRequest();
        ConferenceSolutionKey solutionKey = new ConferenceSolutionKey();
        solutionKey.setType("hangoutsMeet");
        conferenceRequest.setConferenceSolutionKey(solutionKey);
        conferenceRequest.setRequestId("talentflow-" + System.currentTimeMillis());
        conferenceData.setCreateRequest(conferenceRequest);
        event.setConferenceData(conferenceData);

        Event.Reminders reminders = new Event.Reminders();
        reminders.setUseDefault(false);
        EventReminder reminder = new EventReminder();
        reminder.setMethod("popup");
        reminder.setMinutes(30);
        reminders.setOverrides(Collections.singletonList(reminder));
        event.setReminders(reminders);

        event.setColorId("9");

        Event created = service.events().insert("primary", event)
                .setConferenceDataVersion(1)
                .setSendUpdates("all")
                .execute();

        String eventId = created.getId();
        String meetLink = null;

        if (created.getConferenceData() != null
                && created.getConferenceData().getEntryPoints() != null) {
            for (EntryPoint ep : created.getConferenceData().getEntryPoints()) {
                if ("video".equals(ep.getEntryPointType())) {
                    meetLink = ep.getUri();
                    break;
                }
            }
        }

        System.out.println("Événement Google Calendar créé : " + created.getHtmlLink());
        if (meetLink != null) {
            System.out.println("Lien Meet : " + meetLink);
        }

        return eventId + "|" + (meetLink != null ? meetLink : "");
    }

    public CompletableFuture<String> creerEvenementAsync(Entretien entretien) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return creerEvenement(entretien);
            } catch (Exception e) {
                throw new RuntimeException("Google Calendar: " + e.getMessage(), e);
            }
        });
    }

    // ════════════════════════ SUPPRIMER ÉVÉNEMENT ════════════════════════

    public void supprimerEvenement(String eventId)
            throws IOException, GeneralSecurityException {

        if (eventId == null || eventId.isBlank()) return;

        getService().events().delete("primary", eventId)
                .setSendUpdates("all")
                .execute();

        System.out.println("Événement Google Calendar supprimé : " + eventId);
    }

    // ════════════════════════ MODIFIER ÉVÉNEMENT ════════════════════════

    public void modifierEvenement(String eventId, Entretien entretien)
            throws IOException, GeneralSecurityException {

        if (eventId == null || eventId.isBlank()) return;

        Calendar service = getService();

        Event event = service.events().get("primary", eventId).execute();

        event.setSummary("Entretien " + entretien.getType()
                + " — " + entretien.getEmailCandidat());

        LocalDateTime start = entretien.getDateEntretien();
        LocalDateTime end = start.plusHours(1);

        ZonedDateTime startZoned = start.atZone(ZoneId.of(TIMEZONE));
        ZonedDateTime endZoned = end.atZone(ZoneId.of(TIMEZONE));

        event.setStart(new EventDateTime()
                .setDateTime(new DateTime(startZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .setTimeZone(TIMEZONE));

        event.setEnd(new EventDateTime()
                .setDateTime(new DateTime(endZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .setTimeZone(TIMEZONE));

        if ("EN_LIGNE".equals(entretien.getType())) {
            event.setLocation("En ligne (Google Meet)");
        } else if (entretien.getLieu() != null) {
            event.setLocation(entretien.getLieu());
        }

        service.events().update("primary", eventId, event)
                .setSendUpdates("all")
                .execute();

        System.out.println("Événement Google Calendar modifié : " + eventId);
    }

    // ════════════════════════ RÉCUPÉRER LIEN MEET ════════════════════════

    public String getMeetLink(String eventId)
            throws IOException, GeneralSecurityException {

        if (eventId == null || eventId.isBlank()) return null;

        Event event = getService().events().get("primary", eventId).execute();

        if (event.getConferenceData() != null
                && event.getConferenceData().getEntryPoints() != null) {
            for (EntryPoint ep : event.getConferenceData().getEntryPoints()) {
                if ("video".equals(ep.getEntryPointType())) {
                    return ep.getUri();
                }
            }
        }
        return null;
    }

    // ════════════════════════ DÉCONNEXION ════════════════════════

    public void logout() {
        File tokenDir = new File(TOKENS_DIRECTORY);
        if (tokenDir.exists()) {
            File[] files = tokenDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            tokenDir.delete();
        }
        calendarService = null;
        System.out.println("Déconnexion Google Calendar.");
    }
}
