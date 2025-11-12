package com.gestorcorreo.service;

import com.gestorcorreo.model.EmailMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para almacenar y recuperar correos electrónicos localmente
 */
public class EmailStorageService {
    private static final String STORAGE_DIR = System.getProperty("user.home") + "/.correosymultiplicaos/emails";
    private static EmailStorageService instance;
    private final Gson gson;

    private EmailStorageService() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();
        ensureStorageDirectoryExists();
    }

    public static EmailStorageService getInstance() {
        if (instance == null) {
            instance = new EmailStorageService();
        }
        return instance;
    }

    /**
     * Asegura que el directorio de almacenamiento existe
     */
    private void ensureStorageDirectoryExists() {
        try {
            Path path = Paths.get(STORAGE_DIR);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("Error creando directorio de almacenamiento: " + e.getMessage());
        }
    }

    /**
     * Obtiene el archivo de almacenamiento para una cuenta específica
     */
    private File getStorageFile(String emailAccount, String folderName) {
        // Sanitizar el nombre de la cuenta para usarlo como nombre de archivo
        String sanitizedEmail = emailAccount.replaceAll("[^a-zA-Z0-9@._-]", "_");
        String sanitizedFolder = folderName.replaceAll("[^a-zA-Z0-9_-]", "_");
        return new File(STORAGE_DIR, sanitizedEmail + "_" + sanitizedFolder + ".json");
    }

    /**
     * Guarda una lista de correos para una cuenta y carpeta específica
     */
    public void saveEmails(String emailAccount, String folderName, List<EmailMessage> emails) {
        File storageFile = getStorageFile(emailAccount, folderName);
        
        try (FileWriter writer = new FileWriter(storageFile)) {
            gson.toJson(emails, writer);
            System.out.println("Guardados " + emails.size() + " correos para " + emailAccount + " en " + folderName);
        } catch (IOException e) {
            System.err.println("Error guardando correos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga los correos guardados para una cuenta y carpeta específica
     */
    public List<EmailMessage> loadEmails(String emailAccount, String folderName) {
        File storageFile = getStorageFile(emailAccount, folderName);
        
        if (!storageFile.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(storageFile)) {
            Type listType = new TypeToken<List<EmailMessage>>(){}.getType();
            List<EmailMessage> emails = gson.fromJson(reader, listType);
            System.out.println("Cargados " + emails.size() + " correos de " + emailAccount + " en " + folderName);
            return emails != null ? emails : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error cargando correos: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Fusiona correos nuevos con correos existentes, evitando duplicados
     * Los correos se identifican como únicos por: from + subject + sentDate
     */
    public List<EmailMessage> mergeEmails(List<EmailMessage> existing, List<EmailMessage> newEmails) {
        // Crear una lista con los correos existentes
        List<EmailMessage> merged = new ArrayList<>(existing);
        
        // Agregar solo correos nuevos que no existan
        for (EmailMessage newEmail : newEmails) {
            if (!containsEmail(merged, newEmail)) {
                merged.add(newEmail);
            }
        }
        
        // Ordenar por fecha de recepción (más recientes primero)
        merged.sort((e1, e2) -> {
            LocalDateTime date1 = e1.getReceivedDate() != null ? e1.getReceivedDate() : LocalDateTime.MIN;
            LocalDateTime date2 = e2.getReceivedDate() != null ? e2.getReceivedDate() : LocalDateTime.MIN;
            return date2.compareTo(date1);
        });
        
        return merged;
    }

    /**
     * Verifica si un correo ya existe en la lista
     */
    private boolean containsEmail(List<EmailMessage> emails, EmailMessage target) {
        for (EmailMessage email : emails) {
            // Comparar por remitente, asunto y fecha de envío
            if (email.getFrom().equals(target.getFrom()) &&
                email.getSubject().equals(target.getSubject()) &&
                email.getSentDate().equals(target.getSentDate())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Elimina todos los correos guardados para una cuenta específica
     */
    public void deleteAccountEmails(String emailAccount) {
        File storageDir = new File(STORAGE_DIR);
        String sanitizedEmail = emailAccount.replaceAll("[^a-zA-Z0-9@._-]", "_");
        
        File[] files = storageDir.listFiles((dir, name) -> 
            name.startsWith(sanitizedEmail + "_") && name.endsWith(".json"));
        
        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    System.out.println("Eliminado archivo de correos: " + file.getName());
                }
            }
        }
    }

    /**
     * Obtiene el número total de correos guardados para una cuenta
     */
    public int getStoredEmailCount(String emailAccount, String folderName) {
        return loadEmails(emailAccount, folderName).size();
    }

    /**
     * Limpia correos antiguos (opcional, para no acumular demasiados)
     * Mantiene solo los últimos 'maxEmails' correos
     */
    public void cleanOldEmails(String emailAccount, String folderName, int maxEmails) {
        List<EmailMessage> emails = loadEmails(emailAccount, folderName);
        
        if (emails.size() > maxEmails) {
            // Ordenar por fecha (más recientes primero)
            emails.sort((e1, e2) -> {
                LocalDateTime date1 = e1.getReceivedDate() != null ? e1.getReceivedDate() : LocalDateTime.MIN;
                LocalDateTime date2 = e2.getReceivedDate() != null ? e2.getReceivedDate() : LocalDateTime.MIN;
                return date2.compareTo(date1);
            });
            
            // Mantener solo los más recientes
            List<EmailMessage> recentEmails = emails.stream()
                    .limit(maxEmails)
                    .collect(Collectors.toList());
            
            saveEmails(emailAccount, folderName, recentEmails);
            System.out.println("Limpiados correos antiguos, manteniendo " + maxEmails + " más recientes");
        }
    }
}
