package com.gestorcorreo.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Servicio de encriptación para proteger contraseñas y datos sensibles
 * Usa AES-256-GCM con derivación de clave PBKDF2
 */
public class EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    
    private static EncryptionService instance;
    private SecretKey masterKey;
    
    private EncryptionService() {
        try {
            // Generar clave maestra basada en información del sistema
            // En producción, esta clave debería derivarse de una contraseña maestra del usuario
            String systemData = System.getProperty("user.name") + 
                              System.getProperty("user.home") +
                              "CorreosYMultiplicaos2025";
            
            byte[] salt = generateDeterministicSalt(systemData);
            this.masterKey = deriveKey(systemData.toCharArray(), salt);
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar el servicio de encriptación", e);
        }
    }
    
    public static EncryptionService getInstance() {
        if (instance == null) {
            instance = new EncryptionService();
        }
        return instance;
    }
    
    /**
     * Encripta un texto usando AES-256-GCM
     * @param plaintext Texto a encriptar
     * @return Texto encriptado en Base64 (incluye salt + IV + texto cifrado + tag)
     */
    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);
        
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // Combinar IV + texto cifrado
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);
        
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }
    
    /**
     * Desencripta un texto encriptado con AES-256-GCM
     * @param ciphertext Texto encriptado en Base64
     * @return Texto original desencriptado
     */
    public String decrypt(String ciphertext) throws Exception {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        
        // Si el texto no parece estar encriptado (para compatibilidad con datos antiguos)
        if (!isEncrypted(ciphertext)) {
            return ciphertext;
        }
        
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
        
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        
        byte[] encrypted = new byte[byteBuffer.remaining()];
        byteBuffer.get(encrypted);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);
        
        byte[] plaintext = cipher.doFinal(encrypted);
        
        return new String(plaintext, StandardCharsets.UTF_8);
    }
    
    /**
     * Verifica si un texto parece estar encriptado
     */
    private boolean isEncrypted(String text) {
        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            // Un texto encriptado debe tener al menos: IV (12) + tag (16) + 1 byte de datos
            return decoded.length >= (GCM_IV_LENGTH + 17);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Deriva una clave secreta desde una contraseña usando PBKDF2
     */
    private SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    
    /**
     * Genera un salt determinístico basado en datos del sistema
     * Esto permite regenerar la misma clave en cada ejecución
     */
    private byte[] generateDeterministicSalt(String data) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(data.getBytes(StandardCharsets.UTF_8));
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(hash, 0, salt, 0, SALT_LENGTH);
            return salt;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar salt", e);
        }
    }
    
    /**
     * Limpia la clave maestra de memoria (llamar al cerrar la aplicación)
     */
    public void clearKeys() {
        if (masterKey != null) {
            try {
                // Sobrescribir bytes de la clave
                byte[] keyBytes = masterKey.getEncoded();
                for (int i = 0; i < keyBytes.length; i++) {
                    keyBytes[i] = 0;
                }
            } catch (Exception e) {
                System.err.println("Error al limpiar claves: " + e.getMessage());
            }
        }
    }
}
