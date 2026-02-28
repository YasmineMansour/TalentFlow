package org.example.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Service d'envoi d'emails pour le module Entretiens.
 * Lit la configuration depuis mail.properties dans les resources.
 */
public class EntretienEmailService {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean tls;
    private final boolean debug;

    public EntretienEmailService() {
        Properties cfg = loadConfig("mail.properties");

        this.host = require(cfg.getProperty("mail.host"), "mail.host");
        this.port = Integer.parseInt(require(cfg.getProperty("mail.port"), "mail.port"));
        this.username = require(cfg.getProperty("mail.username"), "mail.username");
        this.password = require(cfg.getProperty("mail.appPassword"), "mail.appPassword");
        this.tls = Boolean.parseBoolean(cfg.getProperty("mail.tls", "true"));
        this.debug = Boolean.parseBoolean(cfg.getProperty("mail.debug", "false"));
    }

    public EntretienEmailService(String host, int port, String username, String password, boolean tls) {
        this(host, port, username, password, tls, false);
    }

    public EntretienEmailService(String host, int port, String username, String password, boolean tls, boolean debug) {
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.username = Objects.requireNonNull(username);
        this.password = Objects.requireNonNull(password);
        this.tls = tls;
        this.debug = debug;
    }

    public void sendText(String to, String subject, String body) throws MessagingException {
        send(to, subject, body, false);
    }

    public void sendHtml(String to, String subject, String html) throws MessagingException {
        send(to, subject, html, true);
    }

    public CompletableFuture<Void> sendTextAsync(String to, String subject, String body) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendText(to, subject, body);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> sendHtmlAsync(String to, String subject, String html) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendHtml(to, subject, html);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ───────────────────────── internal ─────────────────────────

    private void send(String to, String subject, String content, boolean isHtml) throws MessagingException {
        if (to == null || to.isBlank()) throw new IllegalArgumentException("Destinataire vide.");
        if (subject == null) subject = "";
        if (content == null) content = "";

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");

        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        if (tls) props.put("mail.smtp.starttls.enable", "true");

        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", host);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        session.setDebug(debug);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject, "UTF-8");

        if (isHtml) {
            message.setContent(content, "text/html; charset=UTF-8");
        } else {
            message.setText(content, "UTF-8");
        }

        Transport.send(message);
    }

    private static Properties loadConfig(String resourceName) {
        try (InputStream in = EntretienEmailService.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Fichier " + resourceName + " introuvable. Mets-le dans src/main/resources/"
                );
            }
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire " + resourceName + " : " + e.getMessage(), e);
        }
    }

    private static String require(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Clé manquante dans mail.properties : " + key);
        }
        return value.trim();
    }
}
