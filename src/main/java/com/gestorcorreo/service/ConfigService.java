package com.gestorcorreo.service;

import com.gestorcorreo.model.EmailConfig;
import com.gestorcorreo.security.EncryptionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio para gestionar la configuración de cuentas de correo
 */
public class ConfigService {
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".correosymultiplicaos";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "accounts.json";
    private static ConfigService instance;
    private List<EmailConfig> accounts;
    private EmailConfig defaultAccount;
    private Gson gson;
    
    private ConfigService() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        accounts = new ArrayList<>();
        loadAccounts();
    }
    
    public static ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }
    
    /**
     * Añade una nueva cuenta de correo
     */
    public void addAccount(EmailConfig config) {
        // Encriptar la contraseña antes de guardar
        try {
            String encryptedPassword = EncryptionService.getInstance().encrypt(config.getPassword());
            config.setPassword(encryptedPassword);
        } catch (Exception e) {
            System.err.println("Error al encriptar contraseña: " + e.getMessage());
        }
        
        accounts.add(config);
        if (config.isDefaultAccount() || accounts.size() == 1) {
            defaultAccount = config;
        }
        saveAccounts();
    }
    
    /**
     * Actualiza una cuenta existente
     */
    public void updateAccount(EmailConfig config) {
        // Encriptar la contraseña antes de guardar
        try {
            String encryptedPassword = EncryptionService.getInstance().encrypt(config.getPassword());
            config.setPassword(encryptedPassword);
        } catch (Exception e) {
            System.err.println("Error al encriptar contraseña: " + e.getMessage());
        }
        
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getEmail().equals(config.getEmail())) {
                accounts.set(i, config);
                if (config.isDefaultAccount()) {
                    defaultAccount = config;
                }
                break;
            }
        }
        saveAccounts();
    }
    
    /**
     * Elimina una cuenta
     */
    public void removeAccount(String email) {
        accounts.removeIf(account -> account.getEmail().equals(email));
        if (defaultAccount != null && defaultAccount.getEmail().equals(email)) {
            defaultAccount = accounts.isEmpty() ? null : accounts.get(0);
        }
        saveAccounts();
    }
    
    /**
     * Obtiene todas las cuentas (con contraseñas ya desencriptadas en memoria)
     */
    public List<EmailConfig> getAllAccounts() {
        return new ArrayList<>(accounts);
    }
    
    /**
     * Obtiene una copia de una cuenta con la contraseña desencriptada
     * (redundante ya que las contraseñas se desencriptan al cargar)
     */
    public EmailConfig getDecryptedAccount(String email) {
        EmailConfig account = getAccountByEmail(email);
        if (account != null) {
            // Las contraseñas ya están desencriptadas en memoria
            return account;
        }
        return null;
    }
    
    /**
     * Obtiene la cuenta predeterminada
     */
    public EmailConfig getDefaultAccount() {
        return defaultAccount;
    }
    
    /**
     * Establece una cuenta como predeterminada
     */
    public void setDefaultAccount(String email) {
        for (EmailConfig account : accounts) {
            account.setDefaultAccount(account.getEmail().equals(email));
            if (account.getEmail().equals(email)) {
                defaultAccount = account;
            }
        }
        saveAccounts();
    }
    
    /**
     * Guarda las cuentas en el archivo de configuración
     */
    private void saveAccounts() {
        try {
            // Crear directorio si no existe
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            File configFile = new File(CONFIG_FILE);
            
            // Guardar en JSON
            try (Writer writer = new FileWriter(configFile)) {
                gson.toJson(accounts, writer);
            }
            
            // Establecer permisos seguros (solo lectura/escritura para el usuario)
            setSecureFilePermissions(configFile);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Establece permisos seguros en el archivo de configuración
     * Solo el usuario puede leer/escribir (600 en Unix)
     */
    private void setSecureFilePermissions(File file) {
        try {
            // En sistemas Unix/Linux
            if (System.getProperty("os.name").toLowerCase().contains("nix") || 
                System.getProperty("os.name").toLowerCase().contains("nux") ||
                System.getProperty("os.name").toLowerCase().contains("mac")) {
                
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(file.toPath(), perms);
            } else {
                // En Windows, establecer solo lectura/escritura para el propietario
                file.setReadable(true, true);
                file.setWritable(true, true);
                file.setExecutable(false, false);
            }
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudieron establecer permisos seguros en " + file.getName());
        }
    }
    
    /**
     * Carga las cuentas desde el archivo de configuración
     */
    private void loadAccounts() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return;
        }
        
        try (Reader reader = new FileReader(configFile)) {
            EmailConfig[] loadedAccounts = gson.fromJson(reader, EmailConfig[].class);
            if (loadedAccounts != null) {
                accounts.clear();
                for (EmailConfig account : loadedAccounts) {
                    // Desencriptar la contraseña al cargar
                    try {
                        String decryptedPassword = EncryptionService.getInstance().decrypt(account.getPassword());
                        account.setPassword(decryptedPassword);
                    } catch (Exception e) {
                        System.err.println("Error al desencriptar contraseña para " + account.getEmail() + ": " + e.getMessage());
                        // Si falla la desencriptación, mantener la contraseña como está (compatibilidad con datos antiguos)
                    }
                    
                    accounts.add(account);
                    if (account.isDefaultAccount()) {
                        defaultAccount = account;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Verifica si hay cuentas configuradas
     */
    public boolean hasAccounts() {
        return !accounts.isEmpty();
    }
    
    /**
     * Obtiene una cuenta por email
     */
    public EmailConfig getAccountByEmail(String email) {
        return accounts.stream()
                .filter(account -> account.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }
}
