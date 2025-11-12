package com.gestorcorreo.ui;

import com.gestorcorreo.model.Tag;
import com.gestorcorreo.service.TagService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Diálogo para gestionar etiquetas
 */
public class TagManagerDialog extends JDialog {
    private JTable tagTable;
    private javax.swing.table.DefaultTableModel tableModel;
    private TagService tagService;
    
    public TagManagerDialog(Frame parent) {
        super(parent, "Gestionar Etiquetas", true);
        this.tagService = TagService.getInstance();
        initComponents();
        loadTags();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(600, 400);
        setLocationRelativeTo(getParent());
        
        // Panel de tabla
        JPanel tablePanel = createTablePanel();
        
        // Panel de botones
        JPanel buttonPanel = createButtonPanel();
        
        add(tablePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Etiquetas"));
        
        // Crear tabla
        String[] columnNames = {"Nombre", "Color", "Descripción"};
        tableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tagTable = new JTable(tableModel);
        tagTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tagTable.setRowHeight(30);
        
        // Renderizador personalizado para la columna de color
        tagTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String colorHex = value.toString();
                    try {
                        Color color = Color.decode(colorHex);
                        label.setBackground(color);
                        label.setForeground(getContrastColor(color));
                        label.setOpaque(true);
                        label.setText(colorHex);
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                    } catch (NumberFormatException e) {
                        label.setText(colorHex);
                    }
                }
                return label;
            }
        });
        
        // Ajustar anchos
        tagTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        tagTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        tagTable.getColumnModel().getColumn(2).setPreferredWidth(300);
        
        JScrollPane scrollPane = new JScrollPane(tagTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton addButton = new JButton("Nueva Etiqueta");
        addButton.addActionListener(e -> addTag());
        
        JButton editButton = new JButton("Editar");
        editButton.addActionListener(e -> editTag());
        
        JButton deleteButton = new JButton("Eliminar");
        deleteButton.addActionListener(e -> deleteTag());
        
        JButton restoreButton = new JButton("Restaurar Predeterminadas");
        restoreButton.addActionListener(e -> restoreDefaultTags());
        
        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(e -> dispose());
        
        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(restoreButton);
        panel.add(closeButton);
        
        return panel;
    }
    
    private void loadTags() {
        tableModel.setRowCount(0);
        for (Tag tag : tagService.getAllTags()) {
            Object[] row = {
                tag.getName(),
                tag.getColor(),
                tag.getDescription()
            };
            tableModel.addRow(row);
        }
    }
    
    private void addTag() {
        TagEditorDialog dialog = new TagEditorDialog(this, null);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            Tag newTag = dialog.getTag();
            if (tagService.addTag(newTag)) {
                loadTags();
                JOptionPane.showMessageDialog(this,
                    "Etiqueta creada correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo crear la etiqueta. Puede que ya exista.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void editTag() {
        int selectedRow = tagTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Selecciona una etiqueta para editar",
                "Sin selección",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String tagName = (String) tableModel.getValueAt(selectedRow, 0);
        Tag tag = tagService.getTagByName(tagName);
        
        if (tag != null) {
            TagEditorDialog dialog = new TagEditorDialog(this, tag);
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                Tag updatedTag = dialog.getTag();
                if (tagService.updateTag(tagName, updatedTag)) {
                    loadTags();
                    JOptionPane.showMessageDialog(this,
                        "Etiqueta actualizada correctamente",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }
    
    private void deleteTag() {
        int selectedRow = tagTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Selecciona una etiqueta para eliminar",
                "Sin selección",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String tagName = (String) tableModel.getValueAt(selectedRow, 0);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Eliminar la etiqueta '" + tagName + "'?\n" +
            "Esta etiqueta se eliminará de todos los correos.",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (tagService.removeTag(tagName)) {
                loadTags();
                JOptionPane.showMessageDialog(this,
                    "Etiqueta eliminada correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private void restoreDefaultTags() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Restaurar las etiquetas predeterminadas?\n" +
            "Esto eliminará todas las etiquetas personalizadas.",
            "Confirmar restauración",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            tagService.restoreDefaultTags();
            loadTags();
            JOptionPane.showMessageDialog(this,
                "Etiquetas restauradas correctamente",
                "Éxito",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Calcula un color contrastante para el texto
     */
    private Color getContrastColor(Color backgroundColor) {
        int luminance = (int) (0.299 * backgroundColor.getRed() + 
                              0.587 * backgroundColor.getGreen() + 
                              0.114 * backgroundColor.getBlue());
        return luminance > 128 ? Color.BLACK : Color.WHITE;
    }
    
    public static void showDialog(Frame parent) {
        TagManagerDialog dialog = new TagManagerDialog(parent);
        dialog.setVisible(true);
    }
}
