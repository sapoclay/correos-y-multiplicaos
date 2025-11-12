package com.gestorcorreo.ui;

import com.gestorcorreo.model.Contact;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo para crear/editar un contacto
 */
public class ContactEditorDialog extends JDialog {
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField companyField;
    private JTextArea notesArea;
    private boolean confirmed = false;
    private Contact contact;
    
    public ContactEditorDialog(Dialog parent, Contact existingContact) {
        super(parent, existingContact == null ? "Nuevo Contacto" : "Editar Contacto", true);
        this.contact = existingContact;
        initComponents();
        if (existingContact != null) {
            loadContact(existingContact);
        }
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 400);
        setLocationRelativeTo(getParent());
        
        // Panel de campos
        JPanel fieldsPanel = createFieldsPanel();
        
        // Panel de botones
        JPanel buttonPanel = createButtonPanel();
        
        add(fieldsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Nombre
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Nombre:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nameField = new JTextField(25);
        panel.add(nameField, gbc);
        
        // Email (requerido)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel emailLabel = new JLabel("Email:*");
        emailLabel.setForeground(Color.RED);
        panel.add(emailLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        emailField = new JTextField(25);
        panel.add(emailField, gbc);
        
        // Teléfono
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Teléfono:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        phoneField = new JTextField(25);
        panel.add(phoneField, gbc);
        
        // Empresa
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Empresa:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        companyField = new JTextField(25);
        panel.add(companyField, gbc);
        
        // Notas
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Notas:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        notesArea = new JTextArea(4, 25);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(notesArea);
        panel.add(scrollPane, gbc);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton okButton = new JButton("Guardar");
        okButton.addActionListener(e -> {
            if (validateFields()) {
                confirmed = true;
                dispose();
            }
        });
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        
        panel.add(okButton);
        panel.add(cancelButton);
        
        return panel;
    }
    
    private void loadContact(Contact contact) {
        nameField.setText(contact.getName());
        emailField.setText(contact.getEmail());
        phoneField.setText(contact.getPhone());
        companyField.setText(contact.getCompany());
        notesArea.setText(contact.getNote());
    }
    
    private boolean validateFields() {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El email es obligatorio",
                "Error de validación",
                JOptionPane.ERROR_MESSAGE);
            emailField.requestFocus();
            return false;
        }
        
        // Validación básica de email
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            JOptionPane.showMessageDialog(this,
                "El email no tiene un formato válido",
                "Error de validación",
                JOptionPane.ERROR_MESSAGE);
            emailField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public Contact getContact() {
        Contact contact = new Contact();
        contact.setName(nameField.getText().trim());
        contact.setEmail(emailField.getText().trim());
        contact.setPhone(phoneField.getText().trim());
        contact.setCompany(companyField.getText().trim());
        contact.setNotes(notesArea.getText().trim());
        
        // Preservar frecuencia si es edición
        if (this.contact != null) {
            contact.setFrequency(this.contact.getFrequency());
        }
        
        return contact;
    }
}
