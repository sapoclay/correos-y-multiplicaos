package com.gestorcorreo.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Diálogo para añadir una nueva cuenta de correo
 */
public class NewAccountDialog extends JDialog {
    
    private JTextField accountNameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> accountTypeCombo;
    private JCheckBox setAsDefaultCheckBox;
    
    // Campos automáticos según el tipo de cuenta
    private JTextField smtpServerField;
    private JTextField imapServerField;
    private JTextArea noteArea;
    
    public NewAccountDialog(JFrame parent) {
        super(parent, "Nueva cuenta de correo", true);
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(550, 650);
        setLocationRelativeTo(getParent());
        
        // Panel principal
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);
        
        // Título
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Configurar nueva cuenta");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        mainPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        
        // Nombre de la cuenta
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Nombre de la cuenta:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        accountNameField = new JTextField();
        mainPanel.add(accountNameField, gbc);
        
        // Tipo de cuenta
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Tipo de cuenta:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String[] accountTypes = {"Gmail", "Outlook", "Yahoo", "Personalizado"};
        accountTypeCombo = new JComboBox<>(accountTypes);
        accountTypeCombo.addActionListener(e -> updateServerFields());
        mainPanel.add(accountTypeCombo, gbc);
        
        // Correo electrónico
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Correo electrónico:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        emailField = new JTextField();
        mainPanel.add(emailField, gbc);
        
        // Contraseña
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Contraseña:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField();
        mainPanel.add(passwordField, gbc);
        
        // Separador
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);
        
        gbc.gridwidth = 1;
        
        // Servidor SMTP
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Servidor SMTP:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        smtpServerField = new JTextField();
        smtpServerField.setEditable(false);
        mainPanel.add(smtpServerField, gbc);
        
        // Servidor IMAP
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Servidor IMAP:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        imapServerField = new JTextField();
        imapServerField.setEditable(false);
        mainPanel.add(imapServerField, gbc);
        
        // Establecer como predeterminada
        gbc.gridx = 1;
        gbc.gridy = 8;
        setAsDefaultCheckBox = new JCheckBox("Establecer como cuenta predeterminada", true);
        mainPanel.add(setAsDefaultCheckBox, gbc);
        
        // Nota informativa con scroll
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.3;
        gbc.ipady = 120;
        
        noteArea = new JTextArea();
        noteArea.setRows(8);
        noteArea.setColumns(40);
        noteArea.setEditable(false);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setForeground(Color.BLACK);
        noteArea.setFont(noteArea.getFont().deriveFont(11f));
        
        JScrollPane noteScrollPane = new JScrollPane(noteArea);
        noteScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        noteScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        noteScrollPane.setPreferredSize(new Dimension(450, 150));
        noteScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 0)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        mainPanel.add(noteScrollPane, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton testButton = new JButton("Probar conexión");
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testConnection();
            }
        });
        
        JButton addButton = new JButton("Añadir cuenta");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addAccount();
            }
        });
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        buttonPanel.add(testButton);
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Inicializar con Gmail por defecto
        updateServerFields();
    }
    
    private void updateServerFields() {
        String selectedType = (String) accountTypeCombo.getSelectedItem();
        Color panelColor = UIManager.getColor("Panel.background");
        Color textColor = UIManager.getColor("TextArea.foreground");
        if (textColor == null) {
            textColor = UIManager.getColor("Label.foreground");
        }
        
        switch (selectedType) {
            case "Gmail":
                smtpServerField.setText("smtp.gmail.com:587");
                imapServerField.setText("imap.gmail.com:993");
                smtpServerField.setEditable(false);
                imapServerField.setEditable(false);
                noteArea.setText(
                    "⚠️ IMPORTANTE - Configuración de Gmail:\n\n" +
                    "Google requiere que uses una CONTRASEÑA DE APLICACIÓN en lugar de tu contraseña normal.\n\n" +
                    "Pasos para obtener una contraseña de aplicación:\n" +
                    "1. Ve a: https://myaccount.google.com/\n" +
                    "2. Haz clic en 'Seguridad'\n" +
                    "3. Activa la 'Verificación en dos pasos' (si no está activada)\n" +
                    "4. Busca 'Contraseñas de aplicaciones'\n" +
                    "5. Genera una nueva contraseña para 'Correo'\n" +
                    "6. Copia la contraseña de 16 caracteres generada\n" +
                    "7. Úsala en el campo 'Contraseña' de arriba\n\n" +
                    "⚠️ NO uses tu contraseña normal de Gmail, no funcionará."
                );
                noteArea.setBackground(panelColor != null ? panelColor : Color.LIGHT_GRAY);
                noteArea.setForeground(textColor != null ? textColor : Color.BLACK);
                updateNoteScrollPaneBorder(new Color(200, 0, 0));
                break;
            case "Outlook":
                smtpServerField.setText("smtp-mail.outlook.com:587");
                imapServerField.setText("outlook.office365.com:993");
                smtpServerField.setEditable(false);
                imapServerField.setEditable(false);
                noteArea.setText(
                    "Nota sobre Outlook:\n\n" +
                    "• Usa tu correo completo como usuario (ejemplo@outlook.com o ejemplo@hotmail.com)\n" +
                    "• Usa tu contraseña normal de Outlook/Hotmail\n" +
                    "• Si tienes activada la verificación en dos pasos, necesitarás una contraseña de aplicación"
                );
                noteArea.setBackground(panelColor != null ? panelColor : Color.LIGHT_GRAY);
                noteArea.setForeground(textColor != null ? textColor : Color.BLACK);
                updateNoteScrollPaneBorder(new Color(0, 0, 200));
                break;
            case "Yahoo":
                smtpServerField.setText("smtp.mail.yahoo.com:587");
                imapServerField.setText("imap.mail.yahoo.com:993");
                smtpServerField.setEditable(false);
                imapServerField.setEditable(false);
                noteArea.setText(
                    "Nota sobre Yahoo:\n\n" +
                    "• Yahoo también requiere una contraseña de aplicación\n" +
                    "• Ve a: Configuración de cuenta > Seguridad de cuenta\n" +
                    "• Genera una contraseña de aplicación\n" +
                    "• Usa esa contraseña en lugar de tu contraseña normal"
                );
                noteArea.setBackground(panelColor != null ? panelColor : Color.LIGHT_GRAY);
                noteArea.setForeground(textColor != null ? textColor : Color.BLACK);
                updateNoteScrollPaneBorder(new Color(200, 100, 0));
                break;
            case "Personalizado":
                smtpServerField.setText("");
                imapServerField.setText("");
                smtpServerField.setEditable(true);
                imapServerField.setEditable(true);
                noteArea.setText(
                    "Configuración personalizada:\n\n" +
                    "• Ingresa los servidores SMTP e IMAP de tu proveedor\n" +
                    "• Formato: servidor.ejemplo.com:puerto\n" +
                    "• Ejemplo SMTP: smtp.ejemplo.com:587\n" +
                    "• Ejemplo IMAP: imap.ejemplo.com:993"
                );
                noteArea.setBackground(panelColor != null ? panelColor : Color.LIGHT_GRAY);
                noteArea.setForeground(textColor != null ? textColor : Color.BLACK);
                updateNoteScrollPaneBorder(new Color(0, 150, 0));
                break;
        }
    }
    
    private void updateNoteScrollPaneBorder(Color borderColor) {
        JScrollPane scrollPane = (JScrollPane) noteArea.getParent().getParent();
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }
    
    private void addAccount() {
        // Validar campos
        if (accountNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Debe especificar un nombre para la cuenta",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (emailField.getText().trim().isEmpty() || !emailField.getText().contains("@")) {
            JOptionPane.showMessageDialog(this,
                "Debe especificar una dirección de correo válida",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (passwordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this,
                "Debe especificar una contraseña",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Crear configuración de cuenta
        com.gestorcorreo.model.EmailConfig config = new com.gestorcorreo.model.EmailConfig();
        config.setAccountName(accountNameField.getText().trim());
        config.setEmail(emailField.getText().trim());
        config.setDisplayName(accountNameField.getText().trim());
        config.setUsername(emailField.getText().trim());
        config.setPassword(new String(passwordField.getPassword()));
        config.setAccountType((String) accountTypeCombo.getSelectedItem());
        config.setDefaultAccount(setAsDefaultCheckBox.isSelected());
        
        // Configurar servidores según el texto del campo
        String smtpText = smtpServerField.getText();
        String imapText = imapServerField.getText();
        
        if (smtpText.contains(":")) {
            String[] smtpParts = smtpText.split(":");
            config.setSmtpHost(smtpParts[0]);
            config.setSmtpPort(Integer.parseInt(smtpParts[1]));
        }
        
        if (imapText.contains(":")) {
            String[] imapParts = imapText.split(":");
            config.setImapHost(imapParts[0]);
            config.setImapPort(Integer.parseInt(imapParts[1]));
        }
        
        config.setUseSsl(true);
        config.setUseTls(true);
        config.setUseAuth(true);
        
        // Guardar la cuenta
        com.gestorcorreo.service.ConfigService.getInstance().addAccount(config);
        
        JOptionPane.showMessageDialog(this,
            "Cuenta añadida correctamente:\n\n" +
            "Nombre: " + accountNameField.getText() + "\n" +
            "Email: " + emailField.getText() + "\n" +
            "Tipo: " + accountTypeCombo.getSelectedItem(),
            "Éxito",
            JOptionPane.INFORMATION_MESSAGE);
        
        // Actualizar el combo de cuentas en la ventana principal
        if (getOwner() instanceof MainWindow) {
            ((MainWindow) getOwner()).refreshAccountCombo();
        }
        
        dispose();
    }
    
    /**
     * Prueba la conexión con los servidores SMTP e IMAP
     */
    private void testConnection() {
        // Validar que hay datos mínimos
        if (emailField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Ingresa una dirección de correo para probar la conexión",
                "Información",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        if (passwordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this,
                "Ingresa una contraseña para probar la conexión",
                "Información",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        if (smtpServerField.getText().trim().isEmpty() || imapServerField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Los servidores SMTP e IMAP deben estar configurados",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Crear diálogo de progreso
        JDialog progressDialog = new JDialog(this, "Probando conexión...", true);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setSize(350, 120);
        progressDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel statusLabel = new JLabel("Conectando a los servidores...");
        panel.add(statusLabel, BorderLayout.NORTH);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.CENTER);
        
        progressDialog.add(panel);
        
        // Probar conexión en un hilo separado
        new Thread(() -> {
            try {
                String email = emailField.getText().trim();
                String password = new String(passwordField.getPassword());
                String smtpText = smtpServerField.getText();
                String imapText = imapServerField.getText();
                
                String smtpHost = "";
                int smtpPort = 587;
                String imapHost = "";
                int imapPort = 993;
                
                if (smtpText.contains(":")) {
                    String[] parts = smtpText.split(":");
                    smtpHost = parts[0];
                    smtpPort = Integer.parseInt(parts[1]);
                }
                
                if (imapText.contains(":")) {
                    String[] parts = imapText.split(":");
                    imapHost = parts[0];
                    imapPort = Integer.parseInt(parts[1]);
                }
                
                StringBuilder resultMessage = new StringBuilder("Resultado de la prueba:\n\n");
                boolean allSuccess = true;
                
                // Probar SMTP
                SwingUtilities.invokeLater(() -> statusLabel.setText("Probando servidor SMTP..."));
                try {
                    java.util.Properties props = new java.util.Properties();
                    props.put("mail.smtp.host", smtpHost);
                    props.put("mail.smtp.port", String.valueOf(smtpPort));
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.connectiontimeout", "5000");
                    props.put("mail.smtp.timeout", "5000");
                    
                    jakarta.mail.Session session = jakarta.mail.Session.getInstance(props,
                        new jakarta.mail.Authenticator() {
                            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                                return new jakarta.mail.PasswordAuthentication(email, password);
                            }
                        });
                    
                    jakarta.mail.Transport transport = session.getTransport("smtp");
                    transport.connect(smtpHost, smtpPort, email, password);
                    transport.close();
                    
                    resultMessage.append("✓ SMTP: Conexión exitosa\n");
                    resultMessage.append("  Servidor: ").append(smtpHost).append(":").append(smtpPort).append("\n\n");
                } catch (Exception e) {
                    allSuccess = false;
                    resultMessage.append("✗ SMTP: Error de conexión\n");
                    resultMessage.append("  ").append(e.getMessage()).append("\n\n");
                }
                
                // Probar IMAP
                SwingUtilities.invokeLater(() -> statusLabel.setText("Probando servidor IMAP..."));
                try {
                    java.util.Properties props = new java.util.Properties();
                    props.put("mail.store.protocol", "imaps");
                    props.put("mail.imaps.host", imapHost);
                    props.put("mail.imaps.port", String.valueOf(imapPort));
                    props.put("mail.imaps.connectiontimeout", "5000");
                    props.put("mail.imaps.timeout", "5000");
                    
                    jakarta.mail.Session session = jakarta.mail.Session.getInstance(props);
                    jakarta.mail.Store store = session.getStore("imaps");
                    store.connect(imapHost, imapPort, email, password);
                    store.close();
                    
                    resultMessage.append("✓ IMAP: Conexión exitosa\n");
                    resultMessage.append("  Servidor: ").append(imapHost).append(":").append(imapPort).append("\n");
                } catch (Exception e) {
                    allSuccess = false;
                    resultMessage.append("✗ IMAP: Error de conexión\n");
                    resultMessage.append("  ").append(e.getMessage()).append("\n");
                }
                
                final boolean success = allSuccess;
                final String message = resultMessage.toString();
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(this,
                        message,
                        success ? "Prueba exitosa" : "Prueba con errores",
                        success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
                });
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(this,
                        "Error al probar la conexión:\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
        
        progressDialog.setVisible(true);
    }
    
    public static void showDialog(JFrame parent) {
        NewAccountDialog dialog = new NewAccountDialog(parent);
        dialog.setVisible(true);
    }
}
