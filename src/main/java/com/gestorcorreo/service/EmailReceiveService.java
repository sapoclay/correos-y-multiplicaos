package com.gestorcorreo.service;

import com.gestorcorreo.model.EmailConfig;
import com.gestorcorreo.model.EmailMessage;
import com.gestorcorreo.model.Attachment;
import com.gestorcorreo.security.RateLimitService;
import com.gestorcorreo.security.SecureLogger;
import com.gestorcorreo.security.SSLCertificateValidator;

import jakarta.mail.*;
import jakarta.mail.search.HeaderTerm;
import jakarta.mail.internet.MimeMultipart;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Servicio para recibir correos electrónicos usando IMAP
 */
public class EmailReceiveService {
    
    /**
     * Lista todas las carpetas disponibles en el servidor IMAP
     * @param config Configuración de la cuenta
     * @return Lista de nombres de carpetas
     */
    public static List<String> listFolders(EmailConfig config) throws Exception {
        List<String> folderNames = new ArrayList<>();
        Store store = null;
        
        try {
            // Configurar propiedades para IMAP
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", config.getImapHost());
            props.put("mail.imaps.port", config.getImapPort());
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.ssl.trust", "*");
            props.put("mail.imaps.timeout", "10000");
            props.put("mail.imaps.connectiontimeout", "10000");
            
            // Crear sesión
            Session session = Session.getInstance(props);
            
            // Conectar al servidor IMAP
            store = session.getStore("imaps");
            store.connect(config.getImapHost(), config.getUsername(), config.getPassword());
            
            // Obtener todas las carpetas
            Folder defaultFolder = store.getDefaultFolder();
            Folder[] folders = defaultFolder.list("*");
            
            for (Folder folder : folders) {
                folderNames.add(folder.getFullName());
            }
            
        } finally {
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
        
        return folderNames;
    }
    
    /**
     * Descarga los correos de una cuenta específica
     * @param config Configuración de la cuenta
     * @param folderName Nombre de la carpeta (INBOX, SENT, etc.)
     * @param maxMessages Número máximo de mensajes a descargar
     * @return Lista de mensajes de correo
     */
    public static List<EmailMessage> fetchEmails(EmailConfig config, String folderName, int maxMessages) throws Exception {
        String resource = config.getEmail() + "@" + config.getImapHost();
        
        // Verificar rate limiting
        if (RateLimitService.getInstance().isBlocked(resource)) {
            long waitTime = RateLimitService.getInstance().getWaitTimeSeconds(resource);
            throw new Exception("Demasiados intentos fallidos. Espera " + waitTime + " segundos antes de reintentar.");
        }
        
        List<EmailMessage> messages = new ArrayList<>();
        Store store = null;
        Folder folder = null;
        
        try {
            SecureLogger.info("Conectando a IMAP: " + config.getImapHost());
            
            // Configurar propiedades para IMAP con validación SSL
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", config.getImapHost());
            props.put("mail.imaps.port", config.getImapPort());
            props.put("mail.imaps.timeout", "10000");
            props.put("mail.imaps.connectiontimeout", "10000");
            
            // Configurar SSL con validación de certificados
            SSLCertificateValidator.getInstance().configureSSLProperties(props, 
                config.getImapHost(), config.getImapPort(), "imaps");
            
            // Crear sesión
            Session session = Session.getInstance(props);
            
            // Conectar al servidor IMAP
            store = session.getStore("imaps");
            store.connect(config.getImapHost(), config.getUsername(), config.getPassword());
            
            // Registrar éxito en rate limiting
            RateLimitService.getInstance().recordSuccess(resource);
            SecureLogger.info("Conexión IMAP exitosa");
            
            // Resolver y abrir la carpeta (con alias comunes)
            folder = store.getFolder(folderName);
            if (folder == null || !folder.exists()) {
                Folder alt = findAlternativeFolder(store, folderName);
                if (alt != null) {
                    folder = alt;
                } else {
                    throw new FolderNotFoundException(store.getFolder(folderName), "Carpeta no encontrada: " + folderName);
                }
            }
            try {
                folder.open(Folder.READ_ONLY);
            } catch (FolderNotFoundException fnf) {
                // Intentar alias si el open falla
                Folder alt = findAlternativeFolder(store, folderName);
                if (alt != null) {
                    folder = alt;
                    folder.open(Folder.READ_ONLY);
                } else {
                    throw fnf;
                }
            }
            
            // Obtener mensajes
            Message[] fetchedMessages = folder.getMessages();
            int totalMessages = fetchedMessages.length;
            int startIndex = Math.max(0, totalMessages - maxMessages);
            
            // Procesar mensajes (de más recientes a más antiguos)
            for (int i = totalMessages - 1; i >= startIndex; i--) {
                try {
                    Message msg = fetchedMessages[i];
                    EmailMessage emailMessage = convertToEmailMessage(msg);
                    messages.add(emailMessage);
                } catch (Exception e) {
                    SecureLogger.error("Error al procesar mensaje", e);
                }
            }
            
        } catch (AuthenticationFailedException e) {
            SecureLogger.error("Autenticación fallida para " + config.getEmail());
            RateLimitService.getInstance().recordFailure(resource);
            throw new Exception("Autenticación fallida. Verifica usuario y contraseña.", e);
        } catch (FolderNotFoundException e) {
            // No contar como fallo de rate limit; es un problema de carpeta/alias
            SecureLogger.error("Carpeta IMAP no encontrada: " + e.getMessage());
            throw e;
        } catch (MessagingException e) {
            SecureLogger.error("Error de conexión IMAP", e);
            RateLimitService.getInstance().recordFailure(resource);
            throw e;
        } finally {
            // Cerrar conexiones
            if (folder != null && folder.isOpen()) {
                folder.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
        
        return messages;
    }
    
    /**
     * Convierte un mensaje de Jakarta Mail a nuestro modelo EmailMessage
     */
    private static EmailMessage convertToEmailMessage(Message msg) throws Exception {
        EmailMessage emailMessage = new EmailMessage();
        
        // Remitente
        if (msg.getFrom() != null && msg.getFrom().length > 0) {
            emailMessage.setFrom(msg.getFrom()[0].toString());
        }
        
        // Destinatarios
        if (msg.getRecipients(Message.RecipientType.TO) != null) {
            for (Address addr : msg.getRecipients(Message.RecipientType.TO)) {
                emailMessage.addTo(addr.toString());
            }
        }
        
        // CC
        if (msg.getRecipients(Message.RecipientType.CC) != null) {
            for (Address addr : msg.getRecipients(Message.RecipientType.CC)) {
                emailMessage.addCc(addr.toString());
            }
        }
        
        // Asunto
        emailMessage.setSubject(msg.getSubject() != null ? msg.getSubject() : "(Sin asunto)");
        
        // Fecha de envío
        Date sentDate = msg.getSentDate();
        if (sentDate != null) {
            emailMessage.setSentDate(LocalDateTime.ofInstant(sentDate.toInstant(), ZoneId.systemDefault()));
        }
        
        // Fecha de recepción
        Date receivedDate = msg.getReceivedDate();
        if (receivedDate != null) {
            emailMessage.setReceivedDate(LocalDateTime.ofInstant(receivedDate.toInstant(), ZoneId.systemDefault()));
        }
        
        // Estado de lectura
        emailMessage.setRead(msg.isSet(Flags.Flag.SEEN));
        
        // ID del mensaje
        String[] messageIdHeader = msg.getHeader("Message-ID");
        if (messageIdHeader != null && messageIdHeader.length > 0) {
            emailMessage.setMessageId(messageIdHeader[0]);
        }
        
        // Contenido y adjuntos
        processMessageContent(msg, emailMessage);
        
        return emailMessage;
    }
    
    /**
     * Procesa el contenido del mensaje y los adjuntos
     */
    private static void processMessageContent(Part part, EmailMessage emailMessage) throws Exception {
        if (part.isMimeType("text/plain")) {
            // Texto plano
            String content = part.getContent().toString();
            if (emailMessage.getBody() == null) {
                emailMessage.setBody(content);
                emailMessage.setHtml(false);
            }
        } else if (part.isMimeType("text/html")) {
            // HTML
            String content = part.getContent().toString();
            emailMessage.setBody(content);
            emailMessage.setHtml(true);
        } else if (part.isMimeType("multipart/*")) {
            // Multipart (texto + adjuntos)
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            int count = multipart.getCount();
            
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                processMessageContent(bodyPart, emailMessage);
            }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
            // Adjunto
            Attachment attachment = new Attachment();
            attachment.setFileName(part.getFileName());
            attachment.setMimeType(part.getContentType());
            attachment.setFileSize(part.getSize());
            emailMessage.addAttachment(attachment);
        }
    }
    
    /**
     * Obtiene el número de mensajes no leídos en una carpeta
     */
    public static int getUnreadCount(EmailConfig config, String folderName) {
        Store store = null;
        Folder folder = null;
        
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", config.getImapHost());
            props.put("mail.imaps.port", config.getImapPort());
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.ssl.trust", "*");
            props.put("mail.imaps.timeout", "5000");
            
            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect(config.getImapHost(), config.getUsername(), config.getPassword());
            
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            
            return folder.getUnreadMessageCount();
            
        } catch (Exception e) {
            System.err.println("Error al obtener mensajes no leídos: " + e.getMessage());
            return 0;
        } finally {
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(false);
                }
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (Exception e) {
                // Ignorar errores al cerrar
            }
        }
    }

    /**
     * Mueve un mensaje del servidor IMAP desde una carpeta origen a otra destino.
     * Intenta localizar el mensaje por Message-ID y, si no está disponible,
     * por combinación de remitente + asunto + fecha de envío.
     *
     * @return true si se movió correctamente; false si no se encontró o hubo error no crítico
     */
    public static boolean moveMessageToFolder(EmailConfig config, String sourceFolderName, String targetFolderName, EmailMessage reference) {
        Store store = null;
        Folder source = null;
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", config.getImapHost());
            props.put("mail.imaps.port", config.getImapPort());
            props.put("mail.imaps.timeout", "10000");
            props.put("mail.imaps.connectiontimeout", "10000");

            SSLCertificateValidator.getInstance().configureSSLProperties(props,
                config.getImapHost(), config.getImapPort(), "imaps");

            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect(config.getImapHost(), config.getUsername(), config.getPassword());

            source = store.getFolder(sourceFolderName);
            if (source == null || !source.exists()) {
                // Intentar con alias comunes
                source = findAlternativeFolder(store, sourceFolderName);
            }
            if (source == null) {
                SecureLogger.warn("Carpeta origen no encontrada: " + sourceFolderName);
                return false;
            }
            source.open(Folder.READ_WRITE);

            Message targetMsg = null;
            try {
                String mid = reference.getMessageId();
                if (mid != null && !mid.isEmpty()) {
                    Message[] found = source.search(new HeaderTerm("Message-ID", mid));
                    if (found != null && found.length > 0) {
                        targetMsg = found[0];
                    }
                }
            } catch (Exception ignored) {}

            if (targetMsg == null) {
                // Búsqueda de respaldo por remitente + asunto + fecha de envío
                Message[] all = source.getMessages();
                for (int i = all.length - 1; i >= 0; i--) {
                    try {
                        Message m = all[i];
                        String from = (m.getFrom() != null && m.getFrom().length > 0) ? m.getFrom()[0].toString() : "";
                        String subj = m.getSubject() != null ? m.getSubject() : "";
                        Date sent = m.getSentDate();
                        boolean match = safeEquals(from, reference.getFrom()) &&
                                        safeEquals(subj, reference.getSubject()) &&
                                        datesEqual(sent, reference.getSentDate());
                        if (match) { targetMsg = m; break; }
                    } catch (Exception ignore) {}
                }
            }

            if (targetMsg == null) {
                SecureLogger.warn("No se encontró el mensaje a mover en servidor (" + sourceFolderName + ")");
                return false;
            }

            Folder destination = store.getFolder(targetFolderName);
            if (destination == null || !destination.exists()) {
                destination = findAlternativeFolder(store, targetFolderName);
                if (destination == null) {
                    // Intentar crear carpeta destino si no existe
                    destination = store.getFolder(targetFolderName);
                    try {
                        if (!destination.exists()) {
                            destination.create(Folder.HOLDS_MESSAGES);
                        }
                    } catch (Exception createEx) {
                        SecureLogger.warn("No se pudo crear la carpeta destino: " + targetFolderName);
                        // intentar nombre genérico "Trash"
                        Folder fallback = store.getFolder("Trash");
                        if (fallback != null && fallback.exists()) {
                            destination = fallback;
                        } else {
                            return false;
                        }
                    }
                }
            }

            if (!destination.isOpen()) {
                destination.open(Folder.READ_WRITE);
            }

            // Copiar y marcar borrado para simular "mover"
            source.copyMessages(new Message[]{targetMsg}, destination);
            targetMsg.setFlag(Flags.Flag.DELETED, true);
            // Expurgar cambios
            try { source.close(true); } catch (Exception ignore) {}
            try { destination.close(false); } catch (Exception ignore) {}
            try { store.close(); } catch (Exception ignore) {}
            return true;

        } catch (Exception ex) {
            SecureLogger.warn("Fallo al mover mensaje en servidor: " + ex.getMessage());
            try {
                if (source != null && source.isOpen()) source.close(false);
                if (store != null && store.isConnected()) store.close();
            } catch (Exception ignore) {}
            return false;
        }
    }

    /** Mueve a Papelera (Trash) */
    public static boolean moveToTrash(EmailConfig config, String sourceFolderName, EmailMessage reference) {
        // Aliases comunes de Trash serán resueltos por findAlternativeFolder
        return moveMessageToFolder(config, sourceFolderName, "Trash", reference);
    }

    private static Folder findAlternativeFolder(Store store, String preferredName) throws MessagingException {
        String[] alternatives;
        switch (preferredName) {
            case "Trash":
                alternatives = new String[]{
                    "[Gmail]/Papelera", "[Gmail]/Trash", "Deleted", "Deleted Items", "Deleted Messages", "Papelera", "INBOX.Trash", "INBOX/Trash", "[Google Mail]/Papelera", "[Google Mail]/Trash"
                }; break;
            case "Sent":
                alternatives = new String[]{
                    "[Gmail]/Enviados", "[Gmail]/Sent Mail", "Sent Items", "Sent Mail", "Enviados"
                }; break;
            case "Drafts":
                alternatives = new String[]{
                    "[Gmail]/Borradores", "[Gmail]/Drafts", "Draft", "Drafts", "Borradores"
                }; break;
            case "Junk":
                alternatives = new String[]{
                    "[Gmail]/Spam", "Spam", "Correo no deseado", "Junk", "Junk Mail"
                }; break;
            default:
                alternatives = new String[]{};
        }

        for (String name : alternatives) {
            Folder f = store.getFolder(name);
            if (f != null && f.exists()) return f;
        }
        return null;
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static boolean datesEqual(Date date, java.time.LocalDateTime ldt) {
        if (date == null && ldt == null) return true;
        if (date == null || ldt == null) return false;
        java.time.LocalDateTime d2 = java.time.LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
        return d2.equals(ldt);
    }
}
