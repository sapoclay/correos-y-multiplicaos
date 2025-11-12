package com.gestorcorreo.ui;

import com.gestorcorreo.model.Tag;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo para crear/editar una etiqueta
 */
public class TagEditorDialog extends JDialog {
    private JTextField nameField;
    private JTextField colorField;
    private JTextArea descriptionArea;
    private JPanel colorPreview;
    private boolean confirmed = false;
    private Tag tag;
    
    public TagEditorDialog(Dialog parent, Tag existingTag) {
        super(parent, existingTag == null ? "Nueva Etiqueta" : "Editar Etiqueta", true);
        this.tag = existingTag;
        initComponents();
        if (existingTag != null) {
            loadTag(existingTag);
        }
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(450, 350);
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
        nameField = new JTextField(20);
        panel.add(nameField, gbc);
        
        // Color
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Color (hex):"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel colorPanel = new JPanel(new BorderLayout(5, 0));
        colorField = new JTextField("#FF0000", 10);
        colorField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateColorPreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateColorPreview(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateColorPreview(); }
        });
        
        colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(40, 25));
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        colorPreview.setBackground(Color.RED);
        
        JButton colorChooserBtn = new JButton("Elegir...");
        colorChooserBtn.addActionListener(e -> chooseColor());
        
        colorPanel.add(colorField, BorderLayout.CENTER);
        colorPanel.add(colorPreview, BorderLayout.WEST);
        colorPanel.add(colorChooserBtn, BorderLayout.EAST);
        panel.add(colorPanel, gbc);
        
        // Colores predefinidos
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Predefinidos:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel presetsPanel = createPresetsPanel();
        panel.add(presetsPanel, gbc);
        
        // Descripción
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Descripción:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        descriptionArea = new JTextArea(4, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        panel.add(scrollPane, gbc);
        
        return panel;
    }
    
    private JPanel createPresetsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        
        String[] presetColors = {
            "#FF0000", "#FFA500", "#FFFF00", "#00FF00", 
            "#0000FF", "#FF00FF", "#00FFFF", "#808080"
        };
        
        for (String colorHex : presetColors) {
            JButton colorBtn = new JButton();
            colorBtn.setPreferredSize(new Dimension(30, 25));
            colorBtn.setBackground(Color.decode(colorHex));
            colorBtn.setToolTipText(colorHex);
            colorBtn.addActionListener(e -> {
                colorField.setText(colorHex);
                updateColorPreview();
            });
            panel.add(colorBtn);
        }
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton okButton = new JButton("Aceptar");
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
    
    private void loadTag(Tag tag) {
        nameField.setText(tag.getName());
        colorField.setText(tag.getColor());
        descriptionArea.setText(tag.getDescription());
        updateColorPreview();
    }
    
    private boolean validateFields() {
        String name = nameField.getText().trim();
        String color = colorField.getText().trim();
        
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El nombre de la etiqueta no puede estar vacío",
                "Error de validación",
                JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return false;
        }
        
        if (!color.matches("#[0-9A-Fa-f]{6}")) {
            JOptionPane.showMessageDialog(this,
                "El color debe estar en formato hexadecimal (#RRGGBB)\n" +
                "Ejemplo: #FF0000 para rojo",
                "Error de validación",
                JOptionPane.ERROR_MESSAGE);
            colorField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void updateColorPreview() {
        String colorHex = colorField.getText().trim();
        try {
            Color color = Color.decode(colorHex);
            colorPreview.setBackground(color);
        } catch (NumberFormatException e) {
            // Color inválido, no actualizar
        }
    }
    
    private void chooseColor() {
        Color currentColor;
        try {
            currentColor = Color.decode(colorField.getText());
        } catch (NumberFormatException e) {
            currentColor = Color.RED;
        }
        
        Color newColor = JColorChooser.showDialog(this, "Elegir Color", currentColor);
        if (newColor != null) {
            String colorHex = String.format("#%02X%02X%02X", 
                newColor.getRed(), newColor.getGreen(), newColor.getBlue());
            colorField.setText(colorHex);
            updateColorPreview();
        }
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public Tag getTag() {
        return new Tag(
            nameField.getText().trim(),
            colorField.getText().trim(),
            descriptionArea.getText().trim()
        );
    }
}
