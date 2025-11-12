package com.gestorcorreo.security;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Logger seguro que oculta información sensible como contraseñas, tokens, etc.
 */
public class SecureLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final boolean DEBUG_ENABLED =
        Boolean.parseBoolean(System.getProperty("gestor.debug", System.getenv().getOrDefault("GESTOR_DEBUG", "false")));
    
    // Patrones para detectar información sensible
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd|contraseña)[\"']?\\s*[:=]\\s*[\"']?([^\\s\"',}]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(?i)(token|auth|bearer|api[_-]?key)[\"']?\\s*[:=]\\s*[\"']?([^\\s\"',}]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EMAIL_PASSWORD_PATTERN = Pattern.compile(
        "(?i)connect.*password[=:]\\s*([^,\\s]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    public enum Level {
        DEBUG, INFO, WARN, ERROR, SECURITY
    }
    
    /**
     * Log con nivel INFO
     */
    public static void info(String message) {
        log(Level.INFO, message);
    }
    
    /**
     * Log con nivel DEBUG
     */
    public static void debug(String message) {
        if (DEBUG_ENABLED) {
            log(Level.DEBUG, message);
        }
    }
    
    /**
     * Log con nivel WARN
     */
    public static void warn(String message) {
        log(Level.WARN, message);
    }
    
    /**
     * Log con nivel ERROR
     */
    public static void error(String message) {
        log(Level.ERROR, message);
    }
    
    /**
     * Log con nivel ERROR y excepción
     */
    public static void error(String message, Throwable t) {
        log(Level.ERROR, message + " - " + t.getMessage());
    }
    
    /**
     * Log de eventos de seguridad (siempre visible)
     */
    public static void security(String message) {
        log(Level.SECURITY, message);
    }
    
    /**
     * Log principal con sanitización automática
     */
    private static void log(Level level, String message) {
        if (level == Level.DEBUG && !DEBUG_ENABLED) {
            return; // Silenciar DEBUG si no está habilitado
        }
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String sanitizedMessage = sanitize(message);
        String prefix = String.format("[%s] [%s]", timestamp, level);
        
        // Colorear según nivel (solo en consola)
        String coloredPrefix = colorizePrefix(prefix, level);
        
        System.out.println(coloredPrefix + " " + sanitizedMessage);
    }
    
    /**
     * Sanitiza el mensaje ocultando información sensible
     */
    private static String sanitize(String message) {
        if (message == null) {
            return "";
        }
        
        String sanitized = message;
        
        // Ocultar contraseñas
        sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1=***HIDDEN***");
        
        // Ocultar tokens
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("$1=***HIDDEN***");
        
        // Ocultar contraseñas en conexiones
        sanitized = EMAIL_PASSWORD_PATTERN.matcher(sanitized).replaceAll("connect...password=***HIDDEN***");
        
        // Ocultar parte de emails (mantener solo dominio)
        sanitized = sanitized.replaceAll("([a-zA-Z0-9._%+-]{2})[a-zA-Z0-9._%+-]*@", "$1***@");
        
        return sanitized;
    }
    
    /**
     * Coloriza el prefijo según el nivel
     */
    private static String colorizePrefix(String prefix, Level level) {
        switch (level) {
            case DEBUG:
                return "\u001B[36m" + prefix + "\u001B[0m"; // Cyan
            case INFO:
                return "\u001B[32m" + prefix + "\u001B[0m"; // Verde
            case WARN:
                return "\u001B[33m" + prefix + "\u001B[0m"; // Amarillo
            case ERROR:
                return "\u001B[31m" + prefix + "\u001B[0m"; // Rojo
            case SECURITY:
                return "\u001B[35m" + prefix + "\u001B[0m"; // Magenta
            default:
                return prefix;
        }
    }
    
    /**
     * Oculta completamente una cadena (para uso explícito)
     */
    public static String mask(String sensitive) {
        if (sensitive == null || sensitive.isEmpty()) {
            return "";
        }
        return "***HIDDEN***";
    }
    
    /**
     * Oculta parcialmente una cadena (muestra primeros y últimos caracteres)
     */
    public static String maskPartial(String sensitive, int visibleChars) {
        if (sensitive == null || sensitive.isEmpty()) {
            return "";
        }
        
        if (sensitive.length() <= visibleChars * 2) {
            return "***";
        }
        
        String start = sensitive.substring(0, visibleChars);
        String end = sensitive.substring(sensitive.length() - visibleChars);
        return start + "***" + end;
    }
}
