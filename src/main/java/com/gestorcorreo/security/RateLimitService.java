package com.gestorcorreo.security;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de Rate Limiting para prevenir ataques de fuerza bruta
 * Implementa espera exponencial (exponential backoff)
 */
public class RateLimitService {
    private static RateLimitService instance;
    
    // Configuración
    private static final int MAX_ATTEMPTS = 3;
    private static final int BASE_DELAY_SECONDS = 5;
    private static final int MAX_DELAY_SECONDS = 300; // 5 minutos
    
    // Almacena intentos fallidos por recurso (email/host)
    private final Map<String, AttemptRecord> attemptRecords = new ConcurrentHashMap<>();
    
    private RateLimitService() {}
    
    public static RateLimitService getInstance() {
        if (instance == null) {
            instance = new RateLimitService();
        }
        return instance;
    }
    
    /**
     * Registra un intento de conexión exitoso
     */
    public void recordSuccess(String resource) {
        attemptRecords.remove(resource);
        SecureLogger.debug("Rate limit: Intento exitoso para " + SecureLogger.maskPartial(resource, 3));
    }
    
    /**
     * Registra un intento de conexión fallido
     */
    public void recordFailure(String resource) {
        AttemptRecord record = attemptRecords.computeIfAbsent(resource, k -> new AttemptRecord());
        record.incrementAttempts();
        
        SecureLogger.warn("Rate limit: Intento fallido #" + record.attempts + " para " + 
                         SecureLogger.maskPartial(resource, 3));
        
        if (record.attempts >= MAX_ATTEMPTS) {
            SecureLogger.security("ALERTA: Múltiples intentos fallidos detectados para " + 
                                 SecureLogger.maskPartial(resource, 3));
        }
    }
    
    /**
     * Verifica si un recurso está bloqueado temporalmente
     * @return true si debe esperar, false si puede continuar
     */
    public boolean isBlocked(String resource) {
        AttemptRecord record = attemptRecords.get(resource);
        
        if (record == null || record.attempts < MAX_ATTEMPTS) {
            return false;
        }
        
        long delaySeconds = calculateDelay(record.attempts);
        Instant unblockTime = record.lastAttempt.plusSeconds(delaySeconds);
        
        if (Instant.now().isBefore(unblockTime)) {
            long remainingSeconds = Duration.between(Instant.now(), unblockTime).getSeconds();
            SecureLogger.warn("Recurso bloqueado. Tiempo restante: " + remainingSeconds + " segundos");
            return true;
        }
        
        return false;
    }
    
    /**
     * Obtiene el tiempo de espera necesario antes de reintentar
     * @return segundos de espera, o 0 si no hay bloqueo
     */
    public long getWaitTimeSeconds(String resource) {
        AttemptRecord record = attemptRecords.get(resource);
        
        if (record == null || record.attempts < MAX_ATTEMPTS) {
            return 0;
        }
        
        long delaySeconds = calculateDelay(record.attempts);
        Instant unblockTime = record.lastAttempt.plusSeconds(delaySeconds);
        
        if (Instant.now().isBefore(unblockTime)) {
            return Duration.between(Instant.now(), unblockTime).getSeconds();
        }
        
        return 0;
    }
    
    /**
     * Calcula el delay usando backoff exponencial
     */
    private long calculateDelay(int attempts) {
        if (attempts < MAX_ATTEMPTS) {
            return 0;
        }
        
        // Backoff exponencial: base * 2^(intentos - max)
        long delay = BASE_DELAY_SECONDS * (long) Math.pow(2, attempts - MAX_ATTEMPTS);
        return Math.min(delay, MAX_DELAY_SECONDS);
    }
    
    /**
     * Limpia intentos antiguos (llamar periódicamente)
     */
    public void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(MAX_DELAY_SECONDS * 2);
        attemptRecords.entrySet().removeIf(entry -> entry.getValue().lastAttempt.isBefore(cutoff));
        SecureLogger.debug("Rate limit: Limpieza de registros antiguos completada");
    }
    
    /**
     * Reinicia el contador para un recurso específico (uso administrativo)
     */
    public void reset(String resource) {
        attemptRecords.remove(resource);
        SecureLogger.info("Rate limit: Contador reiniciado para " + SecureLogger.maskPartial(resource, 3));
    }
    
    /**
     * Obtiene información de un recurso (para debugging)
     */
    public String getStatus(String resource) {
        AttemptRecord record = attemptRecords.get(resource);
        if (record == null) {
            return "Sin intentos registrados";
        }
        
        long waitTime = getWaitTimeSeconds(resource);
        return String.format("Intentos: %d, Último: %s, Espera: %d seg", 
                           record.attempts, record.lastAttempt, waitTime);
    }
    
    /**
     * Registro interno de intentos
     */
    private static class AttemptRecord {
        private int attempts = 0;
        private Instant lastAttempt = Instant.now();
        
        void incrementAttempts() {
            attempts++;
            lastAttempt = Instant.now();
        }
    }
}
