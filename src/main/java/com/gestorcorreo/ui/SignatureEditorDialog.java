package com.gestorcorreo.ui;

import com.gestorcorreo.model.EmailConfig;
import com.gestorcorreo.service.ConfigService;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo para editar la firma de una cuenta de correo
 */
public class SignatureEditorDialog extends JDialog {
    private JTextArea signatureArea;
    private JCheckBox useSignatureCheck;
    private JLabel previewLabel;
    private EmailConfig account;
    private boolean confirmed = false;
    
    public SignatureEditorDialog(Frame parent, EmailConfig account) {
        super(parent, "Editor de Firma - " + account.getEmail(), true);
        this.account = account;
        initComponents();
        loadSignature();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(600, 500);
        setLocationRelativeTo(getParent());
        
        // Panel principal con pestañas
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Pestaña de edición
        JPanel editorPanel = createEditorPanel();
        tabbedPane.addTab("Editar Firma", editorPanel);
        
        // Pestaña de vista previa
        JPanel previewPanel = createPreviewPanel();
        tabbedPane.addTab("Vista Previa", previewPanel);
        
        // Panel de botones
        JPanel buttonPanel = createButtonPanel();
        
        add(tabbedPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Checkbox para activar/desactivar firma
        useSignatureCheck = new JCheckBox("Usar firma automáticamente al redactar correos");
        useSignatureCheck.addActionListener(e -> signatureArea.setEnabled(useSignatureCheck.isSelected()));
        panel.add(useSignatureCheck, BorderLayout.NORTH);
        
        // Área de texto para la firma
        signatureArea = new JTextArea(15, 50);
        signatureArea.setLineWrap(true);
        signatureArea.setWrapStyleWord(true);
        signatureArea.setFont(new Font("Arial", Font.PLAIN, 12));
        signatureArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
        });
        
        JScrollPane scrollPane = new JScrollPane(signatureArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Texto de la firma"));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de ayuda
        JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.setBorder(BorderFactory.createTitledBorder("Ayuda"));
        JTextArea helpText = new JTextArea(
            "Sugerencias para la firma:\n" +
            "• Incluye tu nombre completo\n" +
            "• Añade tu cargo o posición\n" +
            "• Información de contacto (teléfono, web)\n" +
            "• Mantén la firma breve (4-6 líneas)\n\n" +
            "Ejemplo:\n" +
            "Saludos cordiales,\n" +
            "Juan Pérez\n" +
            "Gerente de Ventas\n" +
            "Tel: +34 600 123 456\n" +
            "www.miempresa.com"
        );
        helpText.setEditable(false);
        helpText.setBackground(panel.getBackground());
        helpText.setFont(new Font("Arial", Font.PLAIN, 11));
        helpPanel.add(helpText, BorderLayout.CENTER);
        panel.add(helpPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Vista previa de cómo se verá la firma:");
        panel.add(titleLabel, BorderLayout.NORTH);
        
        previewLabel = new JLabel();
        previewLabel.setVerticalAlignment(SwingConstants.TOP);
        previewLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Vista Previa"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JScrollPane scrollPane = new JScrollPane(previewLabel);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton insertTemplateBtn = new JButton("Plantilla...");
        insertTemplateBtn.addActionListener(e -> insertTemplate());
        
        JButton clearBtn = new JButton("Limpiar");
        clearBtn.addActionListener(e -> {
            signatureArea.setText("");
            updatePreview();
        });
        
        JButton okButton = new JButton("Guardar");
        okButton.addActionListener(e -> {
            saveSignature();
            confirmed = true;
            dispose();
        });
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        
        panel.add(insertTemplateBtn);
        panel.add(clearBtn);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(okButton);
        panel.add(cancelButton);
        
        return panel;
    }
    
    private void loadSignature() {
        if (account.getSignature() != null) {
            signatureArea.setText(account.getSignature());
        }
        useSignatureCheck.setSelected(account.isUseSignature());
        signatureArea.setEnabled(account.isUseSignature());
        updatePreview();
    }
    
    private void saveSignature() {
        account.setSignature(signatureArea.getText());
        account.setUseSignature(useSignatureCheck.isSelected());
        
        // Guardar en el servicio de configuración
        ConfigService.getInstance().updateAccount(account);
        
        JOptionPane.showMessageDialog(this,
            "Firma guardada correctamente",
            "Éxito",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void updatePreview() {
        String signature = signatureArea.getText();
        if (signature.isEmpty()) {
            previewLabel.setText("<html><i style='color: gray;'>Sin firma</i></html>");
        } else {
            // Convertir saltos de línea a HTML
            String htmlSignature = signature.replace("\n", "<br>");
            previewLabel.setText("<html><div style='font-family: Arial; font-size: 12px;'>" +
                "<hr style='border: 1px solid #ccc; margin: 10px 0;'>" +
                htmlSignature +
                "</div></html>");
        }
    }
    
    private void insertTemplate() {
        String[] templates = {
            "Profesional",
            "Informal",
            "Corporativo",
            "Minimalista"
        };
        
        String choice = (String) JOptionPane.showInputDialog(
            this,
            "Selecciona una plantilla de firma:",
            "Plantillas de Firma",
            JOptionPane.QUESTION_MESSAGE,
            null,
            templates,
            templates[0]
        );
        
        if (choice != null) {
            String template = "";
            String displayName = account.getDisplayName() != null ? account.getDisplayName() : "Tu Nombre";
            
            switch (choice) {
                case "Profesional":
                    template = String.format(
                        "Atentamente,\n\n" +
                        "%s\n" +
                        "Cargo/Posición\n" +
                        "Empresa/Organización\n" +
                        "Tel: +34 XXX XXX XXX\n" +
                        "Email: %s",
                        displayName, account.getEmail()
                    );
                    break;
                case "Informal":
                    template = String.format(
                        "Saludos,\n" +
                        "%s\n\n" +
                        "📧 %s",
                        displayName, account.getEmail()
                    );
                    break;
                case "Corporativo":
                    template = String.format(
                        "Cordialmente,\n\n" +
                        "%s\n" +
                        "Cargo | Departamento\n" +
                        "NOMBRE DE LA EMPRESA\n" +
                        "Tel: +34 XXX XXX XXX | Móvil: +34 XXX XXX XXX\n" +
                        "Email: %s | Web: www.empresa.com\n\n" +
                        "Este mensaje y sus archivos adjuntos van dirigidos exclusivamente a su destinatario.",
                        displayName, account.getEmail()
                    );
                    break;
                case "Minimalista":
                    template = String.format(
                        "%s\n" +
                        "%s",
                        displayName, account.getEmail()
                    );
                    break;
            }
            
            signatureArea.setText(template);
            updatePreview();
        }
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public static void showDialog(Frame parent, EmailConfig account) {
        SignatureEditorDialog dialog = new SignatureEditorDialog(parent, account);
        dialog.setVisible(true);
    }
}
