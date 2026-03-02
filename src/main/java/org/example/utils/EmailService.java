package org.example.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * Service d'envoi d'emails via Jakarta Mail (SMTP Gmail).
 *
 * CONFIGURATION REQUISE :
 * 1. Activez la vérification en 2 étapes sur votre compte Google
 * 2. Créez un "Mot de passe d'application" : https://myaccount.google.com/apppasswords
 * 3. Remplacez EMAIL_FROM et EMAIL_PASSWORD ci-dessous
 */
public class EmailService {

    // ===== CONFIGURATION SMTP — À MODIFIER =====
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "yasminemansour912@gmail.com";           // ← Remplacez par votre email
    private static final String EMAIL_PASSWORD = "mlisaeceegtruqst";      // ← Remplacez par le mot de passe d'application

    /** Vérifie si le service email est configuré */
    public static boolean isConfigured() {
        return !EMAIL_FROM.equals("votre.email@gmail.com");
    }

    /** Crée une session SMTP authentifiée */
    private static Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });
    }

    /**
     * Envoie un email HTML.
     * @param to adresse du destinataire
     * @param subject sujet de l'email
     * @param htmlBody contenu HTML
     * @return true si l'envoi a réussi
     */
    public static boolean sendEmail(String to, String subject, String htmlBody) {
        if (!isConfigured()) {
            System.err.println("⚠️ EmailService non configuré. Modifiez EMAIL_FROM et EMAIL_PASSWORD.");
            return false;
        }

        try {
            Message message = new MimeMessage(getSession());
            message.setFrom(new InternetAddress(EMAIL_FROM, "TalentFlow"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email envoyé à : " + to);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email à " + to + " : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ===========================
    //     TEMPLATES D'EMAILS
    // ===========================

    /** Envoie un code de vérification 2FA */
    public static boolean sendVerificationCode(String to, String code) {
        String html = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 500px; margin: 0 auto;
                        background: #ffffff; border-radius: 12px; overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                <div style="background: linear-gradient(135deg, #6c5ce7, #a29bfe); padding: 30px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 24px;">🔐 TalentFlow</h1>
                    <p style="color: rgba(255,255,255,0.85); margin-top: 8px;">Vérification de votre identité</p>
                </div>
                <div style="padding: 30px;">
                    <p style="color: #2d3436; font-size: 15px;">Bonjour,</p>
                    <p style="color: #636e72; font-size: 14px;">
                        Voici votre code de vérification pour accéder à votre compte TalentFlow :
                    </p>
                    <div style="background: #f8f9fa; border: 2px dashed #6c5ce7; border-radius: 10px;
                                padding: 20px; text-align: center; margin: 20px 0;">
                        <span style="font-size: 36px; font-weight: bold; color: #6c5ce7; letter-spacing: 8px;">
                            %s
                        </span>
                    </div>
                    <p style="color: #636e72; font-size: 13px;">
                        ⏰ Ce code est valide pendant <strong>5 minutes</strong>.<br>
                        Si vous n'avez pas demandé ce code, ignorez cet email.
                    </p>
                </div>
                <div style="background: #f8f9fa; padding: 15px; text-align: center;">
                    <p style="color: #b2bec3; font-size: 11px; margin: 0;">
                        © TalentFlow — Plateforme de gestion des talents
                    </p>
                </div>
            </div>
            """.formatted(code);

        return sendEmail(to, "🔐 Code de vérification TalentFlow", html);
    }

    /** Envoie un code de réinitialisation de mot de passe */
    public static boolean sendPasswordResetCode(String to, String code) {
        String html = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 500px; margin: 0 auto;
                        background: #ffffff; border-radius: 12px; overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                <div style="background: linear-gradient(135deg, #ff7675, #fab1a0); padding: 30px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 24px;">🔑 TalentFlow</h1>
                    <p style="color: rgba(255,255,255,0.85); margin-top: 8px;">Réinitialisation du mot de passe</p>
                </div>
                <div style="padding: 30px;">
                    <p style="color: #2d3436; font-size: 15px;">Bonjour,</p>
                    <p style="color: #636e72; font-size: 14px;">
                        Vous avez demandé la réinitialisation de votre mot de passe. 
                        Utilisez le code suivant :
                    </p>
                    <div style="background: #fff5f5; border: 2px dashed #ff7675; border-radius: 10px;
                                padding: 20px; text-align: center; margin: 20px 0;">
                        <span style="font-size: 36px; font-weight: bold; color: #d63031; letter-spacing: 8px;">
                            %s
                        </span>
                    </div>
                    <p style="color: #636e72; font-size: 13px;">
                        ⏰ Ce code est valide pendant <strong>5 minutes</strong>.<br>
                        Si vous n'avez pas fait cette demande, sécurisez votre compte immédiatement.
                    </p>
                </div>
                <div style="background: #f8f9fa; padding: 15px; text-align: center;">
                    <p style="color: #b2bec3; font-size: 11px; margin: 0;">
                        © TalentFlow — Plateforme de gestion des talents
                    </p>
                </div>
            </div>
            """.formatted(code);

        return sendEmail(to, "🔑 Réinitialisation mot de passe - TalentFlow", html);
    }

    /** Envoie un email de bienvenue après inscription */
    public static boolean sendWelcomeEmail(String to, String prenom) {
        String html = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 500px; margin: 0 auto;
                        background: #ffffff; border-radius: 12px; overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                <div style="background: linear-gradient(135deg, #00b894, #55efc4); padding: 30px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 24px;">🎉 TalentFlow</h1>
                    <p style="color: rgba(255,255,255,0.85); margin-top: 8px;">Bienvenue parmi nous !</p>
                </div>
                <div style="padding: 30px;">
                    <p style="color: #2d3436; font-size: 16px;">
                        Bonjour <strong>%s</strong>,
                    </p>
                    <p style="color: #636e72; font-size: 14px;">
                        Votre compte TalentFlow a été créé avec succès ! 🚀
                    </p>
                    <p style="color: #636e72; font-size: 14px;">
                        Vous pouvez maintenant vous connecter et accéder à toutes les fonctionnalités 
                        de notre plateforme de gestion des talents.
                    </p>
                    <div style="background: #f0fff4; border-left: 4px solid #00b894; padding: 15px; 
                                border-radius: 0 8px 8px 0; margin: 20px 0;">
                        <p style="color: #2d3436; font-size: 13px; margin: 0;">
                            💡 <strong>Conseil :</strong> Complétez votre profil pour augmenter 
                            vos chances d'être repéré par les recruteurs.
                        </p>
                    </div>
                </div>
                <div style="background: #f8f9fa; padding: 15px; text-align: center;">
                    <p style="color: #b2bec3; font-size: 11px; margin: 0;">
                        © TalentFlow — Plateforme de gestion des talents
                    </p>
                </div>
            </div>
            """.formatted(prenom);

        return sendEmail(to, "🎉 Bienvenue sur TalentFlow, " + prenom + " !", html);
    }

    // ===========================
    //     TEMPLATES CANDIDATURES
    // ===========================

    /** Envoie une confirmation de candidature au candidat */
    public static boolean sendCandidatureConfirmation(String to, int offreId) {
        String html = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 500px; margin: 0 auto;
                        background: #ffffff; border-radius: 12px; overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                <div style="background: linear-gradient(135deg, #6c5ce7, #a29bfe); padding: 30px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 24px;">📄 TalentFlow</h1>
                    <p style="color: rgba(255,255,255,0.85); margin-top: 8px;">Candidature Confirmée</p>
                </div>
                <div style="padding: 30px;">
                    <p style="color: #2d3436; font-size: 15px;">Bonjour,</p>
                    <p style="color: #636e72; font-size: 14px;">
                        Nous confirmons la bonne réception de votre candidature pour l'offre
                        <strong>#%d</strong>.
                    </p>
                    <div style="background: #f0f0ff; border-left: 4px solid #6c5ce7; padding: 15px;
                                border-radius: 0 8px 8px 0; margin: 20px 0;">
                        <p style="color: #2d3436; font-size: 13px; margin: 0;">
                            ✅ Notre équipe RH examinera votre dossier dans les meilleurs délais.
                            Vous serez notifié(e) par email de l'avancement.
                        </p>
                    </div>
                </div>
                <div style="background: #f8f9fa; padding: 15px; text-align: center;">
                    <p style="color: #b2bec3; font-size: 11px; margin: 0;">
                        © TalentFlow — Plateforme de gestion des talents
                    </p>
                </div>
            </div>
            """.formatted(offreId);

        return sendEmail(to, "📄 Candidature reçue — Offre #" + offreId, html);
    }

    /** Envoie une notification de changement de statut */
    public static boolean sendStatutNotification(String to, int candidatureId, String newStatut) {
        String emoji = switch (newStatut) {
            case "ACCEPTE" -> "🎉";
            case "EN_COURS" -> "🔄";
            case "REFUSE" -> "❌";
            default -> "📌";
        };
        String color = switch (newStatut) {
            case "ACCEPTE" -> "#27ae60";
            case "EN_COURS" -> "#3498db";
            case "REFUSE" -> "#e74c3c";
            default -> "#f39c12";
        };

        String html = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 500px; margin: 0 auto;
                        background: #ffffff; border-radius: 12px; overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                <div style="background: linear-gradient(135deg, %s, %s88); padding: 30px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 24px;">%s TalentFlow</h1>
                    <p style="color: rgba(255,255,255,0.85); margin-top: 8px;">Mise à jour de candidature</p>
                </div>
                <div style="padding: 30px;">
                    <p style="color: #2d3436; font-size: 15px;">Bonjour,</p>
                    <p style="color: #636e72; font-size: 14px;">
                        Le statut de votre candidature <strong>#%d</strong> a été mis à jour :
                    </p>
                    <div style="background: #f8f9fa; border: 2px dashed %s; border-radius: 10px;
                                padding: 20px; text-align: center; margin: 20px 0;">
                        <span style="font-size: 28px; font-weight: bold; color: %s;">
                            %s %s
                        </span>
                    </div>
                </div>
                <div style="background: #f8f9fa; padding: 15px; text-align: center;">
                    <p style="color: #b2bec3; font-size: 11px; margin: 0;">
                        © TalentFlow — Plateforme de gestion des talents
                    </p>
                </div>
            </div>
            """.formatted(color, color, emoji, candidatureId, color, color, emoji, newStatut);

        return sendEmail(to, emoji + " Candidature #" + candidatureId + " — " + newStatut, html);
    }
}
