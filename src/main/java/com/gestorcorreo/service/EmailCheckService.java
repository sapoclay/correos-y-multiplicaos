package com.gestorcorreo.service;

import com.gestorcorreo.model.EmailConfig;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;

/**
 * Servicio para la comprobación automática de correos nuevos
 */
public class EmailCheckService {
    private static EmailCheckService instance;
    private Timer timer;
    private boolean isRunning = false;
    private EmailCheckListener listener;
    
    private EmailCheckService() {
        // Constructor privado para singleton
    }
    
    public static EmailCheckService getInstance() {
        if (instance == null) {
            instance = new EmailCheckService();
        }
        return instance;
    }
    
    /**
     * Inicia la comprobación automática de correos
     */
    public void startAutoCheck() {
        PreferencesService.Preferences prefs = PreferencesService.getInstance().getPreferences();
        
        if (!prefs.isAutoCheckEmail()) {
            stopAutoCheck();
            return;
        }
        
        if (isRunning) {
            stopAutoCheck();
        }
        
        int intervalMinutes = prefs.getCheckIntervalMinutes();
        long intervalMillis = intervalMinutes * 60 * 1000L;
        
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForNewEmails();
            }
        }, intervalMillis, intervalMillis);
        
        isRunning = true;
        System.out.println("Comprobación automática iniciada con intervalo de " + intervalMinutes + " minutos");
    }
    
    /**
     * Detiene la comprobación automática de correos
     */
    public void stopAutoCheck() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isRunning = false;
        System.out.println("Comprobación automática detenida");
    }
    
    /**
     * Reinicia el servicio con la nueva configuración
     */
    public void restart() {
        stopAutoCheck();
        startAutoCheck();
    }
    
    /**
     * Comprueba si hay correos nuevos en todas las cuentas configuradas
     */
    private void checkForNewEmails() {
        // Ejecutar en el hilo del Timer (background). Solo interacciones de UI irán con invokeLater
        try {
            List<EmailConfig> accounts = ConfigService.getInstance().getAllAccounts();
            if (accounts.isEmpty()) {
                return;
            }

            for (EmailConfig account : accounts) {
                try {
                    // Descargar últimos mensajes del servidor (INBOX)
                    List<com.gestorcorreo.model.EmailMessage> remote = EmailReceiveService.fetchEmails(account, "INBOX", 50);
                    // Cargar almacenados localmente
                    List<com.gestorcorreo.model.EmailMessage> local = EmailStorageService.getInstance().loadEmails(account.getEmail(), "INBOX");
                    // Evitar reintroducir correos movidos a Papelera local
                    List<com.gestorcorreo.model.EmailMessage> localTrash = EmailStorageService.getInstance().loadEmails(account.getEmail(), "Trash");
                    if (localTrash != null && !localTrash.isEmpty()) {
                        java.util.List<com.gestorcorreo.model.EmailMessage> filtered = new java.util.ArrayList<>();
                        for (com.gestorcorreo.model.EmailMessage m : remote) {
                            if (!contains(localTrash, m)) {
                                filtered.add(m);
                            }
                        }
                        remote = filtered;
                    }

                    // Calcular nuevos: criterios from + subject + sentDate
                    int newCount = 0;
                    java.util.List<com.gestorcorreo.model.EmailMessage> newOnes = new java.util.ArrayList<>();
                    for (com.gestorcorreo.model.EmailMessage msg : remote) {
                        if (!contains(local, msg)) {
                            newCount++;
                            newOnes.add(msg);
                        }
                    }

                    if (newCount > 0) {
                        // Fusionar y guardar
                        List<com.gestorcorreo.model.EmailMessage> merged = EmailStorageService.getInstance().mergeEmails(local, remote);
                        EmailStorageService.getInstance().saveEmails(account.getEmail(), "INBOX", merged);

                        // Registrar para resaltado/badge
                        NewEmailHighlightService.getInstance().addNewEmails(account.getEmail(), newOnes);

                        final int finalNewCount = newCount;
                        SwingUtilities.invokeLater(() -> notifyNewEmails(account.getEmail(), finalNewCount));
                    }
                } catch (Exception exAcc) {
                    System.err.println("Error comprobando nuevos correos para cuenta " + account.getEmail() + ": " + exAcc.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error general al comprobar correos nuevos: " + e.getMessage());
        }
    }
    
    /**
     * Notifica al usuario sobre correos nuevos
     */
    private void notifyNewEmails(String accountEmail, int count) {
        PreferencesService.Preferences prefs = PreferencesService.getInstance().getPreferences();
        
        // Mostrar notificación del sistema si está habilitado
        if (prefs.isShowSystemNotifications() && listener != null) {
            String message = count == 1 
                ? "Tienes 1 correo nuevo" 
                : "Tienes " + count + " correos nuevos";
            listener.onNewEmailsDetected(accountEmail, count, message);
        }
        
        // Reproducir sonido si está habilitado
        if (prefs.isPlaySoundOnNewEmail()) {
            playNotificationSound();
        }
    }
    
    /**
     * Reproduce un sonido de notificación
     */
    private void playNotificationSound() {
        try {
            // Sonido del sistema
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            System.err.println("Error al reproducir sonido: " + e.getMessage());
        }
    }
    
    /**
     * Establece el listener para eventos de correos nuevos
     */
    public void setEmailCheckListener(EmailCheckListener listener) {
        this.listener = listener;
    }
    
    /**
     * Interfaz para notificar eventos de correos nuevos
     */
    public interface EmailCheckListener {
        void onNewEmailsDetected(String accountEmail, int count, String message);
    }
    
    /**
     * Comprueba si el servicio está en ejecución
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Simula la llegada de correos nuevos (para pruebas)
     * @param count Número de correos nuevos simulados
     */
    public void simulateNewEmails(int count) {
        // Usar la cuenta predeterminada (si existe) o la primera
        List<EmailConfig> accounts = ConfigService.getInstance().getAllAccounts();
        if (accounts.isEmpty()) return;
        EmailConfig target = accounts.stream().filter(EmailConfig::isDefaultAccount).findFirst().orElse(accounts.get(0));
        notifyNewEmails(target.getEmail(), count);
    }

    /** Comprueba si la lista contiene un mensaje (from+subject+sentDate) */
    private boolean contains(List<com.gestorcorreo.model.EmailMessage> list, com.gestorcorreo.model.EmailMessage target) {
        if (list == null || target == null) return false;
        for (com.gestorcorreo.model.EmailMessage e : list) {
            if (safeEquals(e.getFrom(), target.getFrom()) &&
                safeEquals(e.getSubject(), target.getSubject()) &&
                e.getSentDate() != null && target.getSentDate() != null && e.getSentDate().equals(target.getSentDate())) {
                return true;
            }
        }
        return false;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
