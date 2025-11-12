package com.gestorcorreo.ui;

import com.gestorcorreo.model.EmailConfig;
import com.gestorcorreo.service.ConfigService;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo para editar una cuenta de correo existente
 */
public class EditAccountDialog extends JDialog {
    
    private EmailConfig account;
    private JTextField accountNameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JTextField smtpServerField;
    private JTextField smtpPortField;
    private JTextField imapServerField;
    private JTextField imapPortField;
    private JCheckBox setAsDefaultCheckBox;
    private JCheckBox useSslCheckBox;
    private JCheckBox useTlsCheckBox;
    
    public EditAccountDialog(JFrame parent, EmailConfig account) {
        super(parent, "Editar cuenta - " + account.getAccountName(), true);
        this.account = account;
        initComponents();
        loadAccountData();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(550, 550);
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
        JLabel titleLabel = new JLabel("Editar configuración de la cuenta");
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
        
        // Correo electrónico
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Correo electrónico:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        emailField = new JTextField();
        emailField.setEditable(false); // No permitir cambiar el email (identificador único)
        emailField.setBackground(new Color(240, 240, 240));
        mainPanel.add(emailField, gbc);
        
        // Contraseña
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Contraseña:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField();
        mainPanel.add(passwordField, gbc);
        
        // Nota sobre contraseña
        gbc.gridx = 1;
        gbc.gridy = 4;
        JLabel passwordNote = new JLabel("<html><i>Deja en blanco para mantener la actual</i></html>");
        passwordNote.setForeground(Color.GRAY);
        passwordNote.setFont(passwordNote.getFont().deriveFont(10f));
        mainPanel.add(passwordNote, gbc);
        
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
        mainPanel.add(smtpServerField, gbc);
        
        // Puerto SMTP
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Puerto SMTP:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        smtpPortField = new JTextField();
        mainPanel.add(smtpPortField, gbc);
        
        // Servidor IMAP
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Servidor IMAP:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        imapServerField = new JTextField();
        mainPanel.add(imapServerField, gbc);
        
        // Puerto IMAP
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Puerto IMAP:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        imapPortField = new JTextField();
        mainPanel.add(imapPortField, gbc);
        
        // Separador
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);
        
        gbc.gridwidth = 1;
        
        // Opciones SSL/TLS
        gbc.gridx = 1;
        gbc.gridy = 11;
        useSslCheckBox = new JCheckBox("Usar SSL", true);
        mainPanel.add(useSslCheckBox, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 12;
        useTlsCheckBox = new JCheckBox("Usar TLS", true);
        mainPanel.add(useTlsCheckBox, gbc);
        
        // Establecer como predeterminada
        gbc.gridx = 1;
        gbc.gridy = 13;
        setAsDefaultCheckBox = new JCheckBox("Establecer como cuenta predeterminada");
        mainPanel.add(setAsDefaultCheckBox, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton saveButton = new JButton("Guardar cambios");
        saveButton.addActionListener(e -> saveChanges());
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Carga los datos de la cuenta en los campos
     */
    private void loadAccountData() {
        accountNameField.setText(account.getAccountName());
        emailField.setText(account.getEmail());
        smtpServerField.setText(account.getSmtpHost());
        smtpPortField.setText(String.valueOf(account.getSmtpPort()));
        imapServerField.setText(account.getImapHost());
        imapPortField.setText(String.valueOf(account.getImapPort()));
        setAsDefaultCheckBox.setSelected(account.isDefaultAccount());
        useSslCheckBox.setSelected(account.isUseSsl());
        useTlsCheckBox.setSelected(account.isUseTls());
        // No cargar la contraseña por seguridad
    }
    
    /**
     * Guarda los cambios en la cuenta
     */
    private void saveChanges() {
        // Validar campos
        if (accountNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Debe especificar un nombre para la cuenta",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (smtpServerField.getText().trim().isEmpty() || imapServerField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Debe especificar los servidores SMTP e IMAP",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int smtpPort = Integer.parseInt(smtpPortField.getText().trim());
            int imapPort = Integer.parseInt(imapPortField.getText().trim());
            
            // Actualizar la cuenta
            account.setAccountName(accountNameField.getText().trim());
            account.setDisplayName(accountNameField.getText().trim());
            account.setSmtpHost(smtpServerField.getText().trim());
            account.setSmtpPort(smtpPort);
            account.setImapHost(imapServerField.getText().trim());
            account.setImapPort(imapPort);
            account.setUseSsl(useSslCheckBox.isSelected());
            account.setUseTls(useTlsCheckBox.isSelected());
            
            // Actualizar contraseña solo si se ingresó una nueva
            char[] newPassword = passwordField.getPassword();
            if (newPassword.length > 0) {
                account.setPassword(new String(newPassword));
            }
            
            // Actualizar cuenta predeterminada
            if (setAsDefaultCheckBox.isSelected()) {
                ConfigService.getInstance().setDefaultAccount(account.getEmail());
            } else {
                account.setDefaultAccount(false);
            }
            
            // Guardar los cambios
            ConfigService.getInstance().updateAccount(account);
            
            // Actualizar el combo de cuentas en la ventana principal
            if (getOwner() instanceof MainWindow) {
                ((MainWindow) getOwner()).refreshAccountCombo();
            }
            
            JOptionPane.showMessageDialog(this,
                "Cambios guardados correctamente",
                "Éxito",
                JOptionPane.INFORMATION_MESSAGE);
            
            dispose();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Los puertos deben ser números válidos",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void showDialog(JFrame parent, EmailConfig account) {
        EditAccountDialog dialog = new EditAccountDialog(parent, account);
        dialog.setVisible(true);
    }
}
