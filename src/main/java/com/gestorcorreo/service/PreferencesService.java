package com.gestorcorreo.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.Properties;

/**
 * Servicio para gestionar las preferencias de la aplicación
 */
public class PreferencesService {
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".correosymultiplicaos";
    private static final String PREFERENCES_FILE = CONFIG_DIR + File.separator + "preferences.json";
    private static PreferencesService instance;
    private Preferences preferences;
    private Gson gson;
    
    private PreferencesService() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        loadPreferences();
    }
    
    public static PreferencesService getInstance() {
        if (instance == null) {
            instance = new PreferencesService();
        }
        return instance;
    }
    
    /**
     * Carga las preferencias desde el archivo
     */
    private void loadPreferences() {
        File preferencesFile = new File(PREFERENCES_FILE);
        if (!preferencesFile.exists()) {
            preferences = new Preferences();
            savePreferences();
            return;
        }
        
        try (Reader reader = new FileReader(preferencesFile)) {
            preferences = gson.fromJson(reader, Preferences.class);
            if (preferences == null) {
                preferences = new Preferences();
            }
        } catch (IOException e) {
            e.printStackTrace();
            preferences = new Preferences();
        }
    }
    
    /**
     * Guarda las preferencias en el archivo
     */
    public void savePreferences() {
        try {
            // Crear directorio si no existe
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            // Guardar en JSON
            try (Writer writer = new FileWriter(PREFERENCES_FILE)) {
                gson.toJson(preferences, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Preferences getPreferences() {
        return preferences;
    }
    
    public void updatePreferences(Preferences newPreferences) {
        this.preferences = newPreferences;
        savePreferences();
    }
    
    /**
     * Clase interna para almacenar las preferencias
     */
    public static class Preferences {
        // General
        private boolean autoCheckEmail = true;
        private int checkIntervalMinutes = 5;
        private boolean playSoundOnNewEmail = true;
        private boolean showSystemNotifications = true;
        private boolean markAsReadOnOpen = false;
        
        // Apariencia
        private String theme = "Sistema"; // Sistema, Claro, Oscuro
        private String fontSize = "Normal"; // Pequeña, Normal, Grande
        private boolean showTrayIcon = true;
        private boolean minimizeToTray = true;
        
        // Seguridad - Contraseña de bloqueo
        private boolean requireUnlockPassword = false;
        private String unlockPasswordHash = null; // Hash SHA-256 de la contraseña

    // Calendario / Recordatorios
    private int reminderHour = 18; // Hora diaria para recordar citas del día siguiente (0-23)
    private boolean reminderOnlyOnChanges = false; // Si true, solo notificar si las citas de mañana cambiaron
        
        // Getters y setters
        public boolean isAutoCheckEmail() { return autoCheckEmail; }
        public void setAutoCheckEmail(boolean autoCheckEmail) { this.autoCheckEmail = autoCheckEmail; }
        
        public int getCheckIntervalMinutes() { return checkIntervalMinutes; }
        public void setCheckIntervalMinutes(int checkIntervalMinutes) { this.checkIntervalMinutes = checkIntervalMinutes; }
        
        public boolean isPlaySoundOnNewEmail() { return playSoundOnNewEmail; }
        public void setPlaySoundOnNewEmail(boolean playSoundOnNewEmail) { this.playSoundOnNewEmail = playSoundOnNewEmail; }
        
        public boolean isShowSystemNotifications() { return showSystemNotifications; }
        public void setShowSystemNotifications(boolean showSystemNotifications) { this.showSystemNotifications = showSystemNotifications; }
        
        public boolean isMarkAsReadOnOpen() { return markAsReadOnOpen; }
        public void setMarkAsReadOnOpen(boolean markAsReadOnOpen) { this.markAsReadOnOpen = markAsReadOnOpen; }
        
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
        
        public String getFontSize() { return fontSize; }
        public void setFontSize(String fontSize) { this.fontSize = fontSize; }
        
        public boolean isShowTrayIcon() { return showTrayIcon; }
        public void setShowTrayIcon(boolean showTrayIcon) { this.showTrayIcon = showTrayIcon; }
        
        public boolean isMinimizeToTray() { return minimizeToTray; }
        public void setMinimizeToTray(boolean minimizeToTray) { this.minimizeToTray = minimizeToTray; }
        
        public boolean isRequireUnlockPassword() { return requireUnlockPassword; }
        public void setRequireUnlockPassword(boolean requireUnlockPassword) { this.requireUnlockPassword = requireUnlockPassword; }
        
        public String getUnlockPasswordHash() { return unlockPasswordHash; }
        public void setUnlockPasswordHash(String unlockPasswordHash) { this.unlockPasswordHash = unlockPasswordHash; }

        public int getReminderHour() { return reminderHour; }
        public void setReminderHour(int reminderHour) {
            if (reminderHour < 0) reminderHour = 0;
            if (reminderHour > 23) reminderHour = 23;
            this.reminderHour = reminderHour;
        }

        public boolean isReminderOnlyOnChanges() { return reminderOnlyOnChanges; }
        public void setReminderOnlyOnChanges(boolean reminderOnlyOnChanges) { this.reminderOnlyOnChanges = reminderOnlyOnChanges; }
    }
}
