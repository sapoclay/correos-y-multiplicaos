package com.gestorcorreo.service;

import com.gestorcorreo.model.EmailConfig;
import com.gestorcorreo.model.EmailMessage;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import com.gestorcorreo.model.Attachment;
import java.util.Properties;

/**
 * Servicio para enviar correos electrónicos
 */
public class EmailSendService {
    
    /**
     * Envía un correo electrónico (versión simplificada)
     */
    public static void sendEmail(EmailConfig config, String[] to, String[] cc, String subject, String body, String[] attachments) throws Exception {
        EmailMessage message = new EmailMessage();
        for (String recipient : to) {
            message.addTo(recipient.trim());
        }
        if (cc != null) {
            for (String recipient : cc) {
                if (!recipient.trim().isEmpty()) {
                    message.addCc(recipient.trim());
                }
            }
        }
        message.setSubject(subject);
        message.setBody(body);
        // TODO: Implementar adjuntos si es necesario
        
        sendEmail(config, message);
    }
    
    /**
     * Envía un correo electrónico
     */
    public static void sendEmail(EmailConfig config, EmailMessage message) throws Exception {
        // Configurar propiedades
        Properties props = new Properties();
        props.put("mail.smtp.auth", config.isUseAuth());
        props.put("mail.smtp.starttls.enable", config.isUseTls());
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort());
        props.put("mail.smtp.ssl.trust", config.getSmtpHost());
        
        // Si usa SSL en lugar de TLS
        if (config.isUseSsl() && config.getSmtpPort() == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        
        // Crear sesión
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        });
        
        try {
            // Crear mensaje
            Message mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(config.getEmail(), config.getDisplayName()));
            
            // Destinatarios
            String toAddresses = String.join(",", message.getTo());
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddresses));
            
            // CC si existe
            if (message.getCc() != null && !message.getCc().isEmpty()) {
                String ccAddresses = String.join(",", message.getCc());
                mimeMessage.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccAddresses));
            }
            
            // BCC si existe
            if (message.getBcc() != null && !message.getBcc().isEmpty()) {
                String bccAddresses = String.join(",", message.getBcc());
                mimeMessage.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccAddresses));
            }
            
            // Asunto con codificación UTF-8
            ((MimeMessage)mimeMessage).setSubject(message.getSubject(), "UTF-8");
            
            // Detectar si el contenido es HTML
            boolean isHtml = message.getBody() != null && 
                           (message.getBody().trim().startsWith("<html") || 
                            message.getBody().trim().startsWith("<!DOCTYPE"));
            
            if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
                // Separar inline y adjuntos normales
                java.util.List<Attachment> inline = new java.util.ArrayList<>();
                java.util.List<Attachment> regular = new java.util.ArrayList<>();
                for (Attachment a : message.getAttachments()) {
                    if (a.isInline()) inline.add(a); else regular.add(a);
                }

                // Cuerpo principal (puede tener inline)
                MimeMultipart rootMultipart = new MimeMultipart("mixed");

                // Parte cuerpo (text/html + posibles inline)
                MimeBodyPart bodyPart = new MimeBodyPart();
                if (!inline.isEmpty()) {
                    MimeMultipart related = new MimeMultipart("related");
                    MimeBodyPart htmlPart = new MimeBodyPart();
                    if (isHtml) {
                        htmlPart.setContent(message.getBody(), "text/html; charset=UTF-8");
                    } else {
                        htmlPart.setText(message.getBody(), "UTF-8", "plain");
                    }
                    related.addBodyPart(htmlPart);
                    for (Attachment a : inline) {
                        MimeBodyPart img = new MimeBodyPart();
                        img.attachFile(a.getFilePath());
                        img.setHeader("Content-ID", "<" + a.getContentId() + ">");
                        img.setDisposition(Part.INLINE);
                        related.addBodyPart(img);
                    }
                    bodyPart.setContent(related);
                } else {
                    if (isHtml) {
                        bodyPart.setContent(message.getBody(), "text/html; charset=UTF-8");
                    } else {
                        bodyPart.setText(message.getBody(), "UTF-8", "plain");
                    }
                }
                rootMultipart.addBodyPart(bodyPart);

                // Adjuntos normales
                for (Attachment a : regular) {
                    MimeBodyPart attachPart = new MimeBodyPart();
                    attachPart.attachFile(a.getFilePath());
                    rootMultipart.addBodyPart(attachPart);
                }
                mimeMessage.setContent(rootMultipart);
            } else {
                if (isHtml) {
                    mimeMessage.setContent(message.getBody(), "text/html; charset=UTF-8");
                } else {
                    ((MimeMessage)mimeMessage).setText(message.getBody(), "UTF-8", "plain");
                }
            }
            
            // Enviar
            Transport.send(mimeMessage);
            
        } catch (MessagingException e) {
            throw new Exception("Error al enviar el correo: " + e.getMessage(), e);
        }
    }
    
    /**
     * Prueba la conexión con el servidor SMTP
     */
    public static boolean testConnection(EmailConfig config) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", config.isUseAuth());
        props.put("mail.smtp.starttls.enable", config.isUseTls());
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort());
        props.put("mail.smtp.ssl.trust", config.getSmtpHost());
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");
        
        if (config.isUseSsl() && config.getSmtpPort() == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        });
        
        try {
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}
