package com.gestorcorreo.security;

import javax.swing.*;
import java.awt.event.*;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Servicio de auto-lock que bloquea la aplicación tras un período de inactividad
 */
public class AutoLockService {
    private static AutoLockService instance;
    
    // Configuración
    private static final long INACTIVITY_TIMEOUT_MINUTES = 15; // 15 minutos
    private static final long CHECK_INTERVAL_SECONDS = 30; // Verificar cada 30 segundos
    
    private Instant lastActivity;
    private Timer timer;
    private boolean locked = false;
    private JFrame mainWindow;
    private Runnable lockCallback;
    
    private AutoLockService() {
        updateActivity();
    }
    
    public static AutoLockService getInstance() {
        if (instance == null) {
            instance = new AutoLockService();
        }
        return instance;
    }
    
    /**
     * Inicia el monitoreo de inactividad
     */
    public void start(JFrame window, Runnable onLock) {
        this.mainWindow = window;
        this.lockCallback = onLock;
        
        // Instalar listeners de actividad
        installActivityListeners(window);
        
        // Iniciar timer de verificación
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkInactivity();
            }
        }, CHECK_INTERVAL_SECONDS * 1000, CHECK_INTERVAL_SECONDS * 1000);
        
        SecureLogger.info("Auto-lock iniciado: timeout de " + INACTIVITY_TIMEOUT_MINUTES + " minutos");
    }
    
    /**
     * Detiene el monitoreo
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        SecureLogger.info("Auto-lock detenido");
    }
    
    /**
     * Actualiza el timestamp de última actividad
     */
    public void updateActivity() {
        lastActivity = Instant.now();
        
        // Si estaba bloqueado y hay actividad, mostrar diálogo de desbloqueo
        if (locked) {
            SecureLogger.debug("Actividad detectada mientras bloqueado");
        }
    }
    
    /**
     * Verifica si ha pasado el tiempo de inactividad
     */
    private void checkInactivity() {
        if (locked) {
            return; // Ya está bloqueado
        }
        
        long minutesInactive = (Instant.now().toEpochMilli() - lastActivity.toEpochMilli()) / 1000 / 60;
        
        if (minutesInactive >= INACTIVITY_TIMEOUT_MINUTES) {
            SecureLogger.security("ALERTA: Timeout de inactividad alcanzado (" + minutesInactive + " minutos)");
            lockApplication();
        }
    }
    
    /**
     * Bloquea la aplicación manualmente
     */
    public void lockManually() {
        if (!locked) {
            SecureLogger.security("Aplicación bloqueada manualmente por el usuario");
            lockApplication();
        }
    }
    
    /**
     * Bloquea la aplicación
     */
    private void lockApplication() {
        locked = true;
        
        SwingUtilities.invokeLater(() -> {
            if (mainWindow != null) {
                mainWindow.setVisible(false);
            }
            
            if (lockCallback != null) {
                lockCallback.run();
            }
            
            SecureLogger.security("Aplicación bloqueada");
            showUnlockDialog();
        });
    }
    
    /**
     * Muestra el diálogo de desbloqueo
     */
    private void showUnlockDialog() {
        // Verificar si se requiere contraseña
        boolean requirePassword = com.gestorcorreo.service.PreferencesService.getInstance()
            .getPreferences().isRequireUnlockPassword();
        
        JDialog unlockDialog = new JDialog((JFrame) null, "Aplicación Bloqueada", true);
        unlockDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        unlockDialog.setSize(500, requirePassword ? 400 : 350);
        unlockDialog.setLocationRelativeTo(null);
        unlockDialog.setResizable(false);
        unlockDialog.setAlwaysOnTop(true);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel lockIcon = new JLabel("🔒");
        lockIcon.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 48));
        lockIcon.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        
        JLabel message = new JLabel("La aplicación se ha bloqueado");
        message.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        message.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
        
        panel.add(lockIcon);
        panel.add(Box.createVerticalStrut(20));
        panel.add(message);
        panel.add(Box.createVerticalStrut(15));
        
        // Campo de contraseña si está requerido
        JPasswordField passwordField = null;
        JLabel errorLabel = null;
        
        if (requirePassword) {
            JLabel passwordLabel = new JLabel("Introduce la contraseña para desbloquear:");
            passwordLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            panel.add(passwordLabel);
            panel.add(Box.createVerticalStrut(10));
            
            passwordField = new JPasswordField(20);
            passwordField.setMaximumSize(new java.awt.Dimension(300, 30));
            passwordField.setAlignmentX(JPasswordField.CENTER_ALIGNMENT);
            panel.add(passwordField);
            panel.add(Box.createVerticalStrut(10));
            
            errorLabel = new JLabel(" ");
            errorLabel.setForeground(java.awt.Color.RED);
            errorLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            panel.add(errorLabel);
        } else {
            JLabel instruction1 = new JLabel("Haz clic en el botón Desbloquear para continuar");
            instruction1.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            panel.add(instruction1);
            panel.add(Box.createVerticalStrut(5));
            
            JLabel instruction2 = new JLabel("o presiona ENTER");
            instruction2.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            instruction2.setFont(new java.awt.Font("Dialog", java.awt.Font.ITALIC, 12));
            panel.add(instruction2);
        }
        
        panel.add(Box.createVerticalStrut(20));
        
        JButton unlockButton = new JButton("Desbloquear");
        unlockButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        unlockButton.setFocusPainted(true);
        unlockButton.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
        
        // Referencias finales para usar en lambdas
        final JPasswordField finalPasswordField = passwordField;
        final JLabel finalErrorLabel = errorLabel;
        
        // Acción de desbloqueo
        ActionListener unlockAction = e -> {
            if (requirePassword) {
                // Validar contraseña
                String enteredPassword = new String(finalPasswordField.getPassword());
                if (validateUnlockPassword(enteredPassword)) {
                    unlockApplication();
                    unlockDialog.dispose();
                } else {
                    finalErrorLabel.setText("❌ Contraseña incorrecta");
                    finalPasswordField.setText("");
                    finalPasswordField.requestFocus();
                }
            } else {
                unlockApplication();
                unlockDialog.dispose();
            }
        };
        
        unlockButton.addActionListener(unlockAction);
        
        // Permitir desbloquear con ENTER
        unlockDialog.getRootPane().setDefaultButton(unlockButton);
        
        panel.add(unlockButton);
        
        unlockDialog.add(panel);
        
        // Asegurar que el foco esté en el campo correcto
        SwingUtilities.invokeLater(() -> {
            if (requirePassword && finalPasswordField != null) {
                finalPasswordField.requestFocusInWindow();
            } else {
                unlockButton.requestFocusInWindow();
            }
        });
        
        unlockDialog.setVisible(true);
    }
    
    /**
     * Valida la contraseña de desbloqueo
     */
    private boolean validateUnlockPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        String storedHash = com.gestorcorreo.service.PreferencesService.getInstance()
            .getPreferences().getUnlockPasswordHash();
        
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        
        try {
            // Calcular hash SHA-256 de la contraseña ingresada
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().equals(storedHash);
        } catch (Exception e) {
            SecureLogger.error("Error al validar contraseña de desbloqueo: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Desbloquea la aplicación (puede ser llamado externamente)
     */
    public void unlockApplication() {
        if (locked) {
            locked = false;
            updateActivity();
            
            if (mainWindow != null) {
                mainWindow.setVisible(true);
                mainWindow.toFront();
                mainWindow.requestFocus();
            }
            
            SecureLogger.security("Aplicación desbloqueada");
        }
    }
    
    /**
     * Instala listeners para detectar actividad del usuario
     */
    private void installActivityListeners(JFrame window) {
        // Listener de teclado
        KeyAdapter keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                updateActivity();
            }
        };
        
        // Listener de ratón
        MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateActivity();
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                updateActivity();
            }
        };
        
        // Instalar listeners recursivamente en todos los componentes
        installListenersRecursively(window, keyListener, mouseListener);
        
        // Listener global de ventana
        window.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                updateActivity();
            }
        });
    }
    
    /**
     * Instala listeners en un componente y sus hijos recursivamente
     */
    private void installListenersRecursively(java.awt.Container container, 
                                            KeyAdapter keyListener, 
                                            MouseAdapter mouseListener) {
        container.addKeyListener(keyListener);
        container.addMouseListener(mouseListener);
        container.addMouseMotionListener(mouseListener);
        
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof java.awt.Container) {
                installListenersRecursively((java.awt.Container) child, keyListener, mouseListener);
            } else {
                child.addKeyListener(keyListener);
                child.addMouseListener(mouseListener);
                child.addMouseMotionListener(mouseListener);
            }
        }
    }
    
    /**
     * Obtiene el tiempo de inactividad en minutos
     */
    public long getInactivityMinutes() {
        return (Instant.now().toEpochMilli() - lastActivity.toEpochMilli()) / 1000 / 60;
    }
    
    /**
     * Verifica si la aplicación está bloqueada
     */
    public boolean isLocked() {
        return locked;
    }
}
