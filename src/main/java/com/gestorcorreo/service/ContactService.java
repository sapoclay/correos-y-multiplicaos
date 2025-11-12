package com.gestorcorreo.service;

import com.gestorcorreo.model.Contact;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar la libreta de direcciones
 */
public class ContactService {
    private static ContactService instance;
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.correosymultiplicaos";
    private static final String CONTACTS_FILE = CONFIG_DIR + "/contacts.json";
    private final Gson gson;
    private List<Contact> contacts;
    
    private ContactService() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.contacts = new ArrayList<>();
        ensureConfigDirectory();
        loadContacts();
    }
    
    public static synchronized ContactService getInstance() {
        if (instance == null) {
            instance = new ContactService();
        }
        return instance;
    }
    
    private void ensureConfigDirectory() {
        Path configPath = Paths.get(CONFIG_DIR);
        if (!Files.exists(configPath)) {
            try {
                Files.createDirectories(configPath);
            } catch (IOException e) {
                System.err.println("Error al crear directorio de configuración: " + e.getMessage());
            }
        }
    }
    
    /**
     * Carga los contactos desde el archivo JSON
     */
    private void loadContacts() {
        File contactsFile = new File(CONTACTS_FILE);
        
        if (!contactsFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(contactsFile)) {
            Type listType = new TypeToken<List<Contact>>(){}.getType();
            List<Contact> loadedContacts = gson.fromJson(reader, listType);
            if (loadedContacts != null) {
                contacts = loadedContacts;
            }
        } catch (IOException e) {
            System.err.println("Error al cargar contactos: " + e.getMessage());
        }
    }
    
    /**
     * Guarda los contactos en el archivo JSON
     */
    private void saveContacts() {
        try (FileWriter writer = new FileWriter(CONTACTS_FILE)) {
            gson.toJson(contacts, writer);
        } catch (IOException e) {
            System.err.println("Error al guardar contactos: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene todos los contactos
     */
    public List<Contact> getAllContacts() {
        return new ArrayList<>(contacts);
    }
    
    /**
     * Añade un nuevo contacto
     */
    public boolean addContact(Contact contact) {
        if (contact == null || contact.getEmail() == null || contact.getEmail().trim().isEmpty()) {
            return false;
        }
        
        // Verificar si ya existe
        for (Contact existingContact : contacts) {
            if (existingContact.getEmail().equalsIgnoreCase(contact.getEmail())) {
                return false; // Ya existe
            }
        }
        
        contacts.add(contact);
        saveContacts();
        return true;
    }
    
    /**
     * Actualiza un contacto existente
     */
    public boolean updateContact(String oldEmail, Contact newContact) {
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getEmail().equalsIgnoreCase(oldEmail)) {
                contacts.set(i, newContact);
                saveContacts();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Elimina un contacto
     */
    public boolean removeContact(String email) {
        boolean removed = contacts.removeIf(contact -> contact.getEmail().equalsIgnoreCase(email));
        if (removed) {
            saveContacts();
        }
        return removed;
    }
    
    /**
     * Busca un contacto por email
     */
    public Contact getContactByEmail(String email) {
        for (Contact contact : contacts) {
            if (contact.getEmail().equalsIgnoreCase(email)) {
                return contact;
            }
        }
        return null;
    }
    
    /**
     * Busca contactos que coincidan con el texto (nombre o email)
     * Ordenados por frecuencia de uso
     */
    public List<Contact> searchContacts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return contacts.stream()
                .sorted(Comparator.comparingInt(Contact::getFrequency).reversed())
                .collect(Collectors.toList());
        }
        
        String lowerQuery = query.toLowerCase();
        return contacts.stream()
            .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(lowerQuery)) ||
                        c.getEmail().toLowerCase().contains(lowerQuery))
            .sorted(Comparator.comparingInt(Contact::getFrequency).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Añade un contacto automáticamente desde un correo
     * Si ya existe, incrementa su frecuencia
     */
    public void addOrUpdateFromEmail(String email, String name) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }
        
        email = email.trim();
        Contact existing = getContactByEmail(email);
        
        if (existing != null) {
            // Incrementar frecuencia
            existing.incrementFrequency();
            // Actualizar nombre si está vacío y se proporciona uno
            if ((existing.getName() == null || existing.getName().isEmpty()) && 
                name != null && !name.isEmpty()) {
                existing.setName(name);
            }
            saveContacts();
        } else {
            // Crear nuevo contacto
            Contact newContact = new Contact(name, email);
            newContact.incrementFrequency();
            contacts.add(newContact);
            saveContacts();
        }
    }
    
    /**
     * Importa contactos desde un archivo CSV
     * Formato: nombre,email,telefono,empresa,notas
     */
    public int importFromCSV(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        int imported = 0;
        
        for (int i = 0; i < lines.size(); i++) {
            // Saltar cabecera si existe
            if (i == 0 && lines.get(0).toLowerCase().contains("nombre")) {
                continue;
            }
            
            String[] parts = lines.get(i).split(",");
            if (parts.length >= 2) {
                String name = parts[0].trim();
                String email = parts[1].trim();
                String phone = parts.length > 2 ? parts[2].trim() : "";
                String company = parts.length > 3 ? parts[3].trim() : "";
                String notes = parts.length > 4 ? parts[4].trim() : "";
                
                Contact contact = new Contact(name, email, phone, company, notes);
                if (addContact(contact)) {
                    imported++;
                }
            }
        }
        
        return imported;
    }
    
    /**
     * Exporta contactos a un archivo CSV
     */
    public void exportToCSV(String filePath) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Nombre,Email,Teléfono,Empresa,Notas\n");
        
        for (Contact contact : contacts) {
            csv.append(csvEscape(contact.getName())).append(",");
            csv.append(csvEscape(contact.getEmail())).append(",");
            csv.append(csvEscape(contact.getPhone())).append(",");
            csv.append(csvEscape(contact.getCompany())).append(",");
            csv.append(csvEscape(contact.getNote())).append("\n");
        }
        
        Files.write(Paths.get(filePath), csv.toString().getBytes());
    }
    
    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
