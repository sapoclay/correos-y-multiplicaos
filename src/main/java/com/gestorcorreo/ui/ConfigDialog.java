package com.gestorcorreo.ui;

import com.gestorcorreo.service.PreferencesService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Diálogo de configuración general de la aplicación
 * Para configurar cuentas específicas, usar "Administrar cuentas"
 */
public class ConfigDialog extends JDialog {
    
    // Componentes de la interfaz - General
    private JCheckBox autoCheckCheckBox;
    private JSpinner checkIntervalSpinner;
    private JCheckBox soundCheckBox;
    private JCheckBox notificationCheckBox;
    private JCheckBox markReadCheckBox;
    private JSpinner reminderHourSpinner;
    private JCheckBox reminderOnlyOnChangesCheckBox;
    
    // Componentes de la interfaz - Apariencia
    private JComboBox<String> themeCombo;
    private JComboBox<String> fontSizeCombo;
    private JCheckBox trayCheckBox;
    private JCheckBox minimizeToTrayCheckBox;
    
    public ConfigDialog(JFrame parent) {
        super(parent, "Configuración", true);
        initComponents();
        loadPreferences();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 450);
        setLocationRelativeTo(getParent());
        
        // Panel principal con pestañas
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Pestaña de General (opciones de la aplicación)
        tabbedPane.addTab("General", createGeneralPanel());
        
        // Pestaña de Apariencia
        tabbedPane.addTab("Apariencia", createAppearancePanel());
        
        // Pestaña de Seguridad
        tabbedPane.addTab("Seguridad", createSecurityPanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton saveButton = new JButton("Guardar");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfiguration();
            }
        });
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createAppearancePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Título
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Opciones de Apariencia");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        
        // Tema
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Tema de la interfaz:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String[] themes = {"Sistema", "Claro", "Oscuro"};
        themeCombo = new JComboBox<>(themes);
        panel.add(themeCombo, gbc);
        
        // Tamaño de fuente
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("Tamaño de fuente:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String[] fontSizes = {"Pequeña", "Normal", "Grande"};
        fontSizeCombo = new JComboBox<>(fontSizes);
        panel.add(fontSizeCombo, gbc);
        
        // Mostrar bandeja del sistema
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        trayCheckBox = new JCheckBox("Mostrar icono en la bandeja del sistema", true);
        panel.add(trayCheckBox, gbc);
        
        // Minimizar a bandeja
        gbc.gridx = 0;
        gbc.gridy = 4;
        minimizeToTrayCheckBox = new JCheckBox("Minimizar a la bandeja al cerrar", true);
        panel.add(minimizeToTrayCheckBox, gbc);
        
        // Espacio en blanco
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weighty = 1.0;
        panel.add(new JLabel(), gbc);
        
        return panel;
    }
    
    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Título
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
    JLabel titleLabel = new JLabel("Comprobación y Recordatorios");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(titleLabel, gbc);
        
        // Comprobación automática
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        autoCheckCheckBox = new JCheckBox("Comprobar correo automáticamente", true);
        autoCheckCheckBox.addActionListener(e -> {
            checkIntervalSpinner.setEnabled(autoCheckCheckBox.isSelected());
        });
        panel.add(autoCheckCheckBox, gbc);
        
        // Intervalo de comprobación
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JLabel intervalLabel = new JLabel("    Intervalo de comprobación:");
        panel.add(intervalLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(5, 1, 60, 1);
        checkIntervalSpinner = new JSpinner(spinnerModel);
        checkIntervalSpinner.setPreferredSize(new Dimension(60, 25));
        intervalPanel.add(checkIntervalSpinner);
        intervalPanel.add(new JLabel("minutos"));
        panel.add(intervalPanel, gbc);

    // Hora de recordatorio de citas (día siguiente)
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 1;
    gbc.weightx = 0;
    JLabel reminderLabel = new JLabel("    Recordatorio diario (hora):");
    panel.add(reminderLabel, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    JPanel reminderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    SpinnerNumberModel reminderModel = new SpinnerNumberModel(18, 0, 23, 1);
    reminderHourSpinner = new JSpinner(reminderModel);
    reminderHourSpinner.setPreferredSize(new Dimension(60, 25));
    reminderPanel.add(reminderHourSpinner);
    reminderPanel.add(new JLabel(":00"));
    panel.add(reminderPanel, gbc);

        reminderOnlyOnChangesCheckBox = new JCheckBox("Notificar solo si cambian las citas (día siguiente)");
        reminderOnlyOnChangesCheckBox.setSelected(PreferencesService.getInstance().getPreferences().isReminderOnlyOnChanges());
        reminderPanel.add(reminderOnlyOnChangesCheckBox);
        
        // Separador
        gbc.gridx = 0;
    gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(10), gbc);
        
        // Título Notificaciones
        gbc.gridx = 0;
    gbc.gridy = 5;
        gbc.gridwidth = 2;
        JLabel notifTitleLabel = new JLabel("Notificaciones");
        notifTitleLabel.setFont(notifTitleLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(notifTitleLabel, gbc);
        
        // Sonido de notificación
        gbc.gridx = 0;
    gbc.gridy = 6;
        gbc.gridwidth = 2;
        soundCheckBox = new JCheckBox("Reproducir sonido al recibir correo nuevo", true);
        panel.add(soundCheckBox, gbc);
        
        // Notificaciones del sistema
        gbc.gridx = 0;
    gbc.gridy = 7;
        gbc.gridwidth = 1;
        notificationCheckBox = new JCheckBox("Mostrar notificaciones del sistema", true);
        panel.add(notificationCheckBox, gbc);
        
        // Botón de prueba de notificación
    gbc.gridx = 1;
        gbc.weightx = 0;
        JButton testNotificationButton = new JButton("Probar");
        testNotificationButton.setToolTipText("Probar notificación de escritorio");
        testNotificationButton.addActionListener(e -> {
            if (notificationCheckBox.isSelected()) {
                com.gestorcorreo.service.EmailCheckService.getInstance().simulateNewEmails(3);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Las notificaciones están deshabilitadas.\nActiva la opción primero.",
                    "Notificaciones deshabilitadas",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        panel.add(testNotificationButton, gbc);
        
        // Separador
        gbc.gridx = 0;
    gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(Box.createVerticalStrut(10), gbc);
        
        // Título Lectura
        gbc.gridx = 0;
    gbc.gridy = 9;
        gbc.gridwidth = 2;
        JLabel readTitleLabel = new JLabel("Lectura de Mensajes");
        readTitleLabel.setFont(readTitleLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(readTitleLabel, gbc);
        
        // Marcar como leído
        gbc.gridx = 0;
    gbc.gridy = 10;
        markReadCheckBox = new JCheckBox("Marcar como leído automáticamente al abrir", false);
        panel.add(markReadCheckBox, gbc);
        
        // Espacio en blanco
        gbc.gridx = 0;
    gbc.gridy = 11;
        gbc.weighty = 1.0;
        panel.add(new JLabel(), gbc);
        
        return panel;
    }
    
    /**
     * Carga las preferencias actuales en los componentes
     */
    private void loadPreferences() {
        PreferencesService.Preferences prefs = PreferencesService.getInstance().getPreferences();
        
        // General
        autoCheckCheckBox.setSelected(prefs.isAutoCheckEmail());
        checkIntervalSpinner.setValue(prefs.getCheckIntervalMinutes());
        soundCheckBox.setSelected(prefs.isPlaySoundOnNewEmail());
        notificationCheckBox.setSelected(prefs.isShowSystemNotifications());
        markReadCheckBox.setSelected(prefs.isMarkAsReadOnOpen());
        reminderHourSpinner.setValue(prefs.getReminderHour());
        reminderOnlyOnChangesCheckBox.setSelected(prefs.isReminderOnlyOnChanges());
        
        // Apariencia
        themeCombo.setSelectedItem(prefs.getTheme());
        fontSizeCombo.setSelectedItem(prefs.getFontSize());
        trayCheckBox.setSelected(prefs.isShowTrayIcon());
        minimizeToTrayCheckBox.setSelected(prefs.isMinimizeToTray());
    }
    
    /**
     * Guarda la configuración y aplica los cambios
     */
    private void saveConfiguration() {
        PreferencesService.Preferences prefs = new PreferencesService.Preferences();
        
        // General
        prefs.setAutoCheckEmail(autoCheckCheckBox.isSelected());
        prefs.setCheckIntervalMinutes((Integer) checkIntervalSpinner.getValue());
        prefs.setPlaySoundOnNewEmail(soundCheckBox.isSelected());
        prefs.setShowSystemNotifications(notificationCheckBox.isSelected());
        prefs.setMarkAsReadOnOpen(markReadCheckBox.isSelected());
        prefs.setReminderHour((Integer) reminderHourSpinner.getValue());
        prefs.setReminderOnlyOnChanges(reminderOnlyOnChangesCheckBox.isSelected());
        
        // Apariencia
        prefs.setTheme((String) themeCombo.getSelectedItem());
        prefs.setFontSize((String) fontSizeCombo.getSelectedItem());
        prefs.setShowTrayIcon(trayCheckBox.isSelected());
        prefs.setMinimizeToTray(minimizeToTrayCheckBox.isSelected());
        
        // Guardar preferencias
        PreferencesService.getInstance().updatePreferences(prefs);
        
        // Aplicar cambios de apariencia
        applyAppearanceChanges(prefs);
        
        // Reiniciar el servicio de comprobación de correos con la nueva configuración
        com.gestorcorreo.service.EmailCheckService.getInstance().restart();
        // Reiniciar recordatorios con la nueva hora
        com.gestorcorreo.service.AppointmentReminderService.getInstance().start(
            (getOwner() instanceof MainWindow) ? ((MainWindow) getOwner()).getTrayManager() : null);
        
        JOptionPane.showMessageDialog(this,
            "Configuración guardada correctamente.\nLa comprobación automática de correos se ha actualizado.",
            "Éxito",
            JOptionPane.INFORMATION_MESSAGE);
        
        dispose();
    }
    
    /**
     * Aplica los cambios de apariencia inmediatamente
     */
    private void applyAppearanceChanges(PreferencesService.Preferences prefs) {
        try {
            // Aplicar tema
            String theme = prefs.getTheme();
            switch (theme) {
                case "Claro":
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    break;
                case "Oscuro":
                    // Para tema oscuro, intentar usar FlatLaf si está disponible
                    // Por ahora usamos el look and feel del sistema
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "Sistema":
                default:
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
            }
            
            // Aplicar tamaño de fuente
            String fontSize = prefs.getFontSize();
            float fontMultiplier = 1.0f;
            switch (fontSize) {
                case "Pequeña":
                    fontMultiplier = 0.9f;
                    break;
                case "Grande":
                    fontMultiplier = 1.2f;
                    break;
                case "Normal":
                default:
                    fontMultiplier = 1.0f;
                    break;
            }
            
            // Actualizar todas las fuentes del UI
            if (fontMultiplier != 1.0f) {
                java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    Object value = UIManager.get(key);
                    if (value instanceof javax.swing.plaf.FontUIResource) {
                        javax.swing.plaf.FontUIResource font = (javax.swing.plaf.FontUIResource) value;
                        UIManager.put(key, new javax.swing.plaf.FontUIResource(
                            font.deriveFont(font.getSize() * fontMultiplier)));
                    }
                }
            }
            
            // Actualizar todas las ventanas abiertas
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
            
            // Actualizar el menú de la bandeja del sistema
            if (getOwner() instanceof MainWindow) {
                MainWindow mainWindow = (MainWindow) getOwner();
                SystemTrayManager trayManager = mainWindow.getTrayManager();
                if (trayManager != null) {
                    trayManager.updateMenuLookAndFeel();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error al aplicar cambios de apariencia: " + e.getMessage());
        }
    }
    
    private JPanel createSecurityPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Título
        JLabel titleLabel = new JLabel("Configuración de Seguridad");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        // Espacio
        gbc.gridy++;
        panel.add(Box.createVerticalStrut(10), gbc);
        
        // Checkbox para requerir contraseña
        gbc.gridy++;
        JCheckBox requirePasswordCheck = new JCheckBox("Requerir contraseña para desbloquear la aplicación");
        requirePasswordCheck.setSelected(PreferencesService.getInstance().getPreferences().isRequireUnlockPassword());
        panel.add(requirePasswordCheck, gbc);
        
        // Panel para configurar contraseña (inicialmente oculto)
        gbc.gridy++;
        gbc.gridwidth = 1;
        JPanel passwordPanel = new JPanel(new GridBagLayout());
        GridBagConstraints pwGbc = new GridBagConstraints();
        pwGbc.insets = new Insets(5, 5, 5, 5);
        pwGbc.anchor = GridBagConstraints.WEST;
        pwGbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Campo nueva contraseña
        pwGbc.gridx = 0;
        pwGbc.gridy = 0;
        pwGbc.weightx = 0.0;
        passwordPanel.add(new JLabel("Nueva contraseña:"), pwGbc);
        
        pwGbc.gridx = 1;
        pwGbc.weightx = 1.0;
        JPasswordField newPasswordField = new JPasswordField(20);
        passwordPanel.add(newPasswordField, pwGbc);
        
        // Campo confirmar contraseña
        pwGbc.gridx = 0;
        pwGbc.gridy = 1;
        pwGbc.weightx = 0.0;
        passwordPanel.add(new JLabel("Confirmar contraseña:"), pwGbc);
        
        pwGbc.gridx = 1;
        pwGbc.weightx = 1.0;
        JPasswordField confirmPasswordField = new JPasswordField(20);
        passwordPanel.add(confirmPasswordField, pwGbc);
        
        // Etiqueta de error/éxito
        pwGbc.gridx = 0;
        pwGbc.gridy = 2;
        pwGbc.gridwidth = 2;
        JLabel messageLabel = new JLabel(" ");
        messageLabel.setForeground(Color.RED);
        passwordPanel.add(messageLabel, pwGbc);
        
        // Botones
        pwGbc.gridy = 3;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton setPasswordButton = new JButton("Establecer contraseña");
        JButton clearPasswordButton = new JButton("Eliminar contraseña");
        
        buttonPanel.add(setPasswordButton);
        buttonPanel.add(clearPasswordButton);
        passwordPanel.add(buttonPanel, pwGbc);
        
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(passwordPanel, gbc);
        
        // Visibilidad inicial del panel de contraseña
        passwordPanel.setVisible(requirePasswordCheck.isSelected());
        
        // Listener del checkbox
        requirePasswordCheck.addActionListener(e -> {
            boolean enabled = requirePasswordCheck.isSelected();
            passwordPanel.setVisible(enabled);
            
            if (!enabled) {
                // Si se desmarca, eliminar la contraseña
                PreferencesService.getInstance().getPreferences().setRequireUnlockPassword(false);
                PreferencesService.getInstance().getPreferences().setUnlockPasswordHash(null);
                PreferencesService.getInstance().savePreferences();
                messageLabel.setText(" ");
            }
        });
        
        // Acción del botón "Establecer contraseña"
        setPasswordButton.addActionListener(e -> {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Por favor, complete ambos campos");
                return;
            }
            
            if (newPassword.length() < 4) {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("La contraseña debe tener al menos 4 caracteres");
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Las contraseñas no coinciden");
                return;
            }
            
            // Hash de la contraseña con SHA-256
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                
                // Guardar en preferencias
                PreferencesService.getInstance().getPreferences().setRequireUnlockPassword(true);
                PreferencesService.getInstance().getPreferences().setUnlockPasswordHash(hexString.toString());
                PreferencesService.getInstance().savePreferences();
                
                messageLabel.setForeground(new Color(0, 128, 0));
                messageLabel.setText("✓ Contraseña establecida correctamente");
                
                // Limpiar campos
                newPasswordField.setText("");
                confirmPasswordField.setText("");
                
            } catch (Exception ex) {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Error al establecer la contraseña");
                System.err.println("Error al hashear contraseña: " + ex.getMessage());
            }
        });
        
        // Acción del botón "Eliminar contraseña"
        clearPasswordButton.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de que desea eliminar la contraseña de desbloqueo?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (option == JOptionPane.YES_OPTION) {
                PreferencesService.getInstance().getPreferences().setRequireUnlockPassword(false);
                PreferencesService.getInstance().getPreferences().setUnlockPasswordHash(null);
                PreferencesService.getInstance().savePreferences();
                
                requirePasswordCheck.setSelected(false);
                passwordPanel.setVisible(false);
                newPasswordField.setText("");
                confirmPasswordField.setText("");
                
                messageLabel.setForeground(new Color(0, 128, 0));
                messageLabel.setText("✓ Contraseña eliminada");
            }
        });
        
        // Información adicional
        gbc.gridy++;
        gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("<html><i>Nota: La contraseña se almacena de forma segura usando hash SHA-256.<br>" +
                "Si olvida su contraseña, deberá eliminar el archivo de preferencias.</i></html>");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 11f));
        infoLabel.setForeground(Color.GRAY);
        panel.add(infoLabel, gbc);
        
        // Espacio al final
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    public static void showDialog(JFrame parent) {
        ConfigDialog dialog = new ConfigDialog(parent);
        dialog.setVisible(true);
    }
}
