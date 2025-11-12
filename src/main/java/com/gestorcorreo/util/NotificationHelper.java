package com.gestorcorreo.util;

import java.io.IOException;

/**
 * Clase auxiliar para gestionar notificaciones del sistema
 * Compatible con Windows y Linux
 */
public class NotificationHelper {
    
    /**
     * Muestra una notificación del sistema usando diferentes métodos según el SO
     * @param title Título de la notificación
     * @param message Mensaje de la notificación
     * @param type Tipo de notificación (INFO, WARNING, ERROR)
     */
    public static void showNotification(String title, String message, NotificationType type) {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("linux")) {
            showLinuxNotification(title, message, type);
        } else if (os.contains("windows")) {
            showWindowsNotification(title, message, type);
        } else {
            // Fallback para otros sistemas
            showDefaultNotification(title, message);
        }
    }
    
    /**
     * Muestra notificación en Linux usando notify-send
     */
    private static void showLinuxNotification(String title, String message, NotificationType type) {
        try {
            String urgency = "normal";
            String icon = "mail-unread";
            
            switch (type) {
                case INFO:
                    urgency = "normal";
                    icon = "mail-unread";
                    break;
                case WARNING:
                    urgency = "normal";
                    icon = "dialog-warning";
                    break;
                case ERROR:
                    urgency = "critical";
                    icon = "dialog-error";
                    break;
            }
            
            ProcessBuilder pb = new ProcessBuilder(
                "notify-send",
                "-u", urgency,
                "-i", icon,
                "-a", "Correos Y Multiplicaos",
                title,
                message
            );
            pb.start();
        } catch (IOException e) {
            System.err.println("Error al mostrar notificación en Linux: " + e.getMessage());
            showDefaultNotification(title, message);
        }
    }
    
    /**
     * Muestra notificación en Windows (solo disponible con SystemTray)
     */
    private static void showWindowsNotification(String title, String message, NotificationType type) {
        // En Windows, las notificaciones se manejan mejor a través del SystemTray
        // Este método es un fallback
        showDefaultNotification(title, message);
    }
    
    /**
     * Método fallback usando solo el TrayIcon del sistema
     */
    private static void showDefaultNotification(String title, String message) {
        System.out.println("Notificación: " + title + " - " + message);
    }
    
    /**
     * Tipos de notificación
     */
    public enum NotificationType {
        INFO,
        WARNING,
        ERROR
    }
}
