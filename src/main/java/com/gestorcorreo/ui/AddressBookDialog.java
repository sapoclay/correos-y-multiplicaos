package com.gestorcorreo.ui;

import com.gestorcorreo.model.Contact;
import com.gestorcorreo.service.ContactService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Diálogo para gestionar la libreta de direcciones
 */
public class AddressBookDialog extends JDialog {
    private JTable contactTable;
    private javax.swing.table.DefaultTableModel tableModel;
    private ContactService contactService;
    private JTextField searchField;
    
    public AddressBookDialog(Frame parent) {
        super(parent, "Libreta de Direcciones", true);
        this.contactService = ContactService.getInstance();
        initComponents();
        loadContacts();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(800, 500);
        setLocationRelativeTo(getParent());
        
        // Panel de búsqueda
        JPanel searchPanel = createSearchPanel();
        
        // Panel de tabla
        JPanel tablePanel = createTablePanel();
        
        // Panel de botones
        JPanel buttonPanel = createButtonPanel();
        
        add(searchPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        panel.add(new JLabel("Buscar:"));
        
        searchField = new JTextField(30);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterContacts(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterContacts(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterContacts(); }
        });
        panel.add(searchField);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Crear tabla
        String[] columnNames = {"Nombre", "Email", "Teléfono", "Empresa", "Uso"};
        tableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        contactTable = new JTable(tableModel);
        contactTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactTable.setRowHeight(25);
        
        // Ajustar anchos
        contactTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        contactTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        contactTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        contactTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        contactTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        
        JScrollPane scrollPane = new JScrollPane(contactTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Label con contador
        JLabel countLabel = new JLabel();
        countLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(countLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton addButton = new JButton("Nuevo Contacto");
        addButton.addActionListener(e -> addContact());
        
        JButton editButton = new JButton("Editar");
        editButton.addActionListener(e -> editContact());
        
        JButton deleteButton = new JButton("Eliminar");
        deleteButton.addActionListener(e -> deleteContact());
        
        JButton importButton = new JButton("Importar CSV");
        importButton.addActionListener(e -> importContacts());
        
        JButton exportButton = new JButton("Exportar CSV");
        exportButton.addActionListener(e -> exportContacts());
        
        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(e -> dispose());
        
        panel.add(addButton);
        panel.add(editButton);
        panel.add(deleteButton);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(importButton);
        panel.add(exportButton);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(closeButton);
        
        return panel;
    }
    
    private void loadContacts() {
        loadContacts(null);
    }
    
    private void loadContacts(String filter) {
        tableModel.setRowCount(0);
        java.util.List<Contact> contacts = contactService.searchContacts(filter);
        
        for (Contact contact : contacts) {
            Object[] row = {
                contact.getName() != null ? contact.getName() : "",
                contact.getEmail(),
                contact.getPhone() != null ? contact.getPhone() : "",
                contact.getCompany() != null ? contact.getCompany() : "",
                contact.getFrequency()
            };
            tableModel.addRow(row);
        }
        
        // Actualizar contador
        JPanel tablePanel = (JPanel) ((JScrollPane) contactTable.getParent().getParent()).getParent();
        JLabel countLabel = (JLabel) tablePanel.getComponent(1);
        countLabel.setText(contacts.size() + " contacto(s)");
    }
    
    private void filterContacts() {
        String query = searchField.getText().trim();
        loadContacts(query.isEmpty() ? null : query);
    }
    
    private void addContact() {
        ContactEditorDialog dialog = new ContactEditorDialog(this, null);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            Contact newContact = dialog.getContact();
            if (contactService.addContact(newContact)) {
                loadContacts();
                JOptionPane.showMessageDialog(this,
                    "Contacto añadido correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo añadir el contacto. Puede que ya exista.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void editContact() {
        int selectedRow = contactTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Selecciona un contacto para editar",
                "Sin selección",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String email = (String) tableModel.getValueAt(selectedRow, 1);
        Contact contact = contactService.getContactByEmail(email);
        
        if (contact != null) {
            ContactEditorDialog dialog = new ContactEditorDialog(this, contact);
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                Contact updatedContact = dialog.getContact();
                if (contactService.updateContact(email, updatedContact)) {
                    loadContacts();
                    JOptionPane.showMessageDialog(this,
                        "Contacto actualizado correctamente",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }
    
    private void deleteContact() {
        int selectedRow = contactTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Selecciona un contacto para eliminar",
                "Sin selección",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String email = (String) tableModel.getValueAt(selectedRow, 1);
        String name = (String) tableModel.getValueAt(selectedRow, 0);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Eliminar el contacto '" + (name.isEmpty() ? email : name) + "'?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (contactService.removeContact(email)) {
                loadContacts();
                JOptionPane.showMessageDialog(this,
                    "Contacto eliminado correctamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    private void importContacts() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                int imported = contactService.importFromCSV(file.getAbsolutePath());
                loadContacts();
                JOptionPane.showMessageDialog(this,
                    "Se han importado " + imported + " contacto(s) correctamente",
                    "Importación completada",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error al importar contactos: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exportContacts() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));
        fileChooser.setSelectedFile(new File("contactos.csv"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String filePath = file.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".csv")) {
                filePath += ".csv";
            }
            
            try {
                contactService.exportToCSV(filePath);
                JOptionPane.showMessageDialog(this,
                    "Contactos exportados correctamente a:\n" + filePath,
                    "Exportación completada",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error al exportar contactos: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public static void showDialog(Frame parent) {
        AddressBookDialog dialog = new AddressBookDialog(parent);
        dialog.setVisible(true);
    }
}
