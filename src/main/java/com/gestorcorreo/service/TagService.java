package com.gestorcorreo.service;

import com.gestorcorreo.model.Tag;
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
import java.util.List;

/**
 * Servicio para gestionar etiquetas de correo
 */
public class TagService {
    private static TagService instance;
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.correosymultiplicaos";
    private static final String TAGS_FILE = CONFIG_DIR + "/tags.json";
    private final Gson gson;
    private List<Tag> tags;
    
    private TagService() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.tags = new ArrayList<>();
        ensureConfigDirectory();
        loadTags();
    }
    
    public static synchronized TagService getInstance() {
        if (instance == null) {
            instance = new TagService();
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
     * Carga las etiquetas desde el archivo JSON
     */
    private void loadTags() {
        File tagsFile = new File(TAGS_FILE);
        
        if (!tagsFile.exists()) {
            // Crear etiquetas por defecto
            Tag[] defaultTags = Tag.getDefaultTags();
            for (Tag tag : defaultTags) {
                tags.add(tag);
            }
            saveTags();
            return;
        }
        
        try (FileReader reader = new FileReader(tagsFile)) {
            Type listType = new TypeToken<List<Tag>>(){}.getType();
            List<Tag> loadedTags = gson.fromJson(reader, listType);
            if (loadedTags != null) {
                tags = loadedTags;
            }
        } catch (IOException e) {
            System.err.println("Error al cargar etiquetas: " + e.getMessage());
        }
    }
    
    /**
     * Guarda las etiquetas en el archivo JSON
     */
    private void saveTags() {
        try (FileWriter writer = new FileWriter(TAGS_FILE)) {
            gson.toJson(tags, writer);
        } catch (IOException e) {
            System.err.println("Error al guardar etiquetas: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene todas las etiquetas
     */
    public List<Tag> getAllTags() {
        return new ArrayList<>(tags);
    }
    
    /**
     * Añade una nueva etiqueta
     */
    public boolean addTag(Tag tag) {
        if (tag == null || tag.getName() == null || tag.getName().trim().isEmpty()) {
            return false;
        }
        
        // Verificar si ya existe
        for (Tag existingTag : tags) {
            if (existingTag.getName().equalsIgnoreCase(tag.getName())) {
                return false; // Ya existe
            }
        }
        
        tags.add(tag);
        saveTags();
        return true;
    }
    
    /**
     * Actualiza una etiqueta existente
     */
    public boolean updateTag(String oldName, Tag newTag) {
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).getName().equals(oldName)) {
                tags.set(i, newTag);
                saveTags();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Elimina una etiqueta
     */
    public boolean removeTag(String tagName) {
        boolean removed = tags.removeIf(tag -> tag.getName().equals(tagName));
        if (removed) {
            saveTags();
        }
        return removed;
    }
    
    /**
     * Obtiene una etiqueta por nombre
     */
    public Tag getTagByName(String name) {
        for (Tag tag : tags) {
            if (tag.getName().equals(name)) {
                return tag;
            }
        }
        return null;
    }
    
    /**
     * Restaura las etiquetas por defecto
     */
    public void restoreDefaultTags() {
        tags.clear();
        Tag[] defaultTags = Tag.getDefaultTags();
        for (Tag tag : defaultTags) {
            tags.add(tag);
        }
        saveTags();
    }
}
