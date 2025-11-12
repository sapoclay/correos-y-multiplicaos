package com.gestorcorreo.ui;

import com.gestorcorreo.model.EmailMessage;
import com.gestorcorreo.model.Tag;
import com.gestorcorreo.service.EmailStorageService;
import com.gestorcorreo.service.ConfigService;
import com.gestorcorreo.service.TagService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Diálogo para búsqueda avanzada de correos electrónicos
 */
public class AdvancedSearchDialog extends JDialog {
    private JTextField fromField;
    private JTextField subjectField;
    private JTextField contentField;
    private JComboBox<String> dateRangeCombo;
    private JComboBox<String> folderCombo;
    private JComboBox<String> tagCombo;
    private JCheckBox allFoldersCheck;
    
    private List<EmailMessage> searchResults;
    
    public AdvancedSearchDialog(Frame parent) {
        super(parent, "Búsqueda Avanzada", true);
        searchResults = new ArrayList<>();
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(600, 400);
        setLocationRelativeTo(getParent());
        
        // Panel de criterios de búsqueda
        JPanel searchPanel = createSearchPanel();
        
        // Panel de resultados
        JPanel resultsPanel = createResultsPanel();
        
        // Panel de botones
        JPanel buttonPanel = createButtonPanel();
        
        add(searchPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Criterios de Búsqueda"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // De (From)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("De:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        fromField = new JTextField(20);
        fromField.setToolTipText("Buscar por remitente");
        panel.add(fromField, gbc);
        
        // Asunto (Subject)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Asunto:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        subjectField = new JTextField(20);
        subjectField.setToolTipText("Buscar en el asunto");
        panel.add(subjectField, gbc);
        
        // Contenido
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Contenido:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        contentField = new JTextField(20);
        contentField.setToolTipText("Buscar en el contenido del correo");
        panel.add(contentField, gbc);
        
        // Rango de fechas
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Fecha:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        String[] dateRanges = {
            "Cualquier fecha",
            "Hoy",
            "Última semana",
            "Último mes",
            "Últimos 3 meses",
            "Último año"
        };
        dateRangeCombo = new JComboBox<>(dateRanges);
        panel.add(dateRangeCombo, gbc);
        
        // Carpeta
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Carpeta:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel folderPanel = new JPanel(new BorderLayout(5, 0));
        String[] folders = {"INBOX", "Sent", "Drafts", "Trash", "Spam"};
        folderCombo = new JComboBox<>(folders);
        allFoldersCheck = new JCheckBox("Todas las carpetas", true);
        allFoldersCheck.addActionListener(e -> folderCombo.setEnabled(!allFoldersCheck.isSelected()));
        folderCombo.setEnabled(false);
        folderPanel.add(folderCombo, BorderLayout.CENTER);
        folderPanel.add(allFoldersCheck, BorderLayout.EAST);
        panel.add(folderPanel, gbc);
        
        // Etiqueta
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Etiqueta:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        List<String> tagNames = new ArrayList<>();
        tagNames.add("Todas las etiquetas");
        for (Tag tag : TagService.getInstance().getAllTags()) {
            tagNames.add(tag.getName());
        }
        tagCombo = new JComboBox<>(tagNames.toArray(new String[0]));
        tagCombo.setToolTipText("Filtrar por etiqueta");
        panel.add(tagCombo, gbc);
        
        return panel;
    }
    
    private JPanel resultsPanel;
    private JTable resultsTable;
    private javax.swing.table.DefaultTableModel resultsTableModel;
    
    private JPanel createResultsPanel() {
        resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Resultados"));
        
        // Crear tabla de resultados
        String[] columnNames = {"De", "Asunto", "Fecha", "Carpeta"};
        resultsTableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        resultsTable = new JTable(resultsTableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setRowHeight(25);
        
        // Ajustar anchos de columnas
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Etiqueta de contador
        JLabel countLabel = new JLabel("0 resultados encontrados");
        countLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        resultsPanel.add(countLabel, BorderLayout.SOUTH);
        
        return resultsPanel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton searchButton = new JButton("Buscar");
        searchButton.addActionListener(e -> performSearch());
        
        JButton clearButton = new JButton("Limpiar");
        clearButton.addActionListener(e -> clearSearch());
        
        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(e -> dispose());
        
        panel.add(searchButton);
        panel.add(clearButton);
        panel.add(closeButton);
        
        return panel;
    }
    
    private void performSearch() {
        // Obtener criterios de búsqueda
        String fromText = fromField.getText().trim().toLowerCase();
        String subjectText = subjectField.getText().trim().toLowerCase();
        String contentText = contentField.getText().trim().toLowerCase();
        String dateRange = (String) dateRangeCombo.getSelectedItem();
        boolean searchAllFolders = allFoldersCheck.isSelected();
        String selectedFolder = (String) folderCombo.getSelectedItem();
        String selectedTag = (String) tagCombo.getSelectedItem();
        boolean filterByTag = selectedTag != null && !selectedTag.equals("Todas las etiquetas");
        
        // Validar que al menos un criterio esté especificado
        if (fromText.isEmpty() && subjectText.isEmpty() && contentText.isEmpty() && !filterByTag) {
            JOptionPane.showMessageDialog(this,
                "Por favor, especifica al menos un criterio de búsqueda",
                "Criterios vacíos",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Calcular fecha límite según el rango seleccionado
        Date dateLimit = calculateDateLimit(dateRange);
        
        // Limpiar resultados anteriores
        searchResults.clear();
        resultsTableModel.setRowCount(0);
        
        // Buscar en todas las cuentas
        ConfigService configService = ConfigService.getInstance();
        EmailStorageService storageService = EmailStorageService.getInstance();
        
        configService.getAllAccounts().forEach(account -> {
            String[] foldersToSearch;
            
            if (searchAllFolders) {
                foldersToSearch = new String[]{"INBOX", "Sent", "Drafts", "Trash", "Spam"};
            } else {
                foldersToSearch = new String[]{selectedFolder};
            }
            
            for (String folder : foldersToSearch) {
                List<EmailMessage> emails = storageService.loadEmails(account.getEmail(), folder);
                
                for (EmailMessage email : emails) {
                    // Verificar filtro de etiquetas
                    if (filterByTag && !email.hasTag(selectedTag)) {
                        continue;
                    }
                    
                    if (matchesSearchCriteria(email, fromText, subjectText, contentText, dateLimit)) {
                        searchResults.add(email);
                        
                        // Añadir a la tabla
                        Object[] row = {
                            email.getFrom(),
                            email.getSubject(),
                            email.getSentDate(),
                            folder
                        };
                        resultsTableModel.addRow(row);
                    }
                }
            }
        });
        
        // Actualizar contador
        JLabel countLabel = (JLabel) resultsPanel.getComponent(1);
        countLabel.setText(searchResults.size() + " resultado(s) encontrado(s)");
        
        if (searchResults.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No se encontraron correos que coincidan con los criterios",
                "Sin resultados",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private boolean matchesSearchCriteria(EmailMessage email, String fromText, String subjectText, 
                                         String contentText, Date dateLimit) {
        // Verificar remitente
        if (!fromText.isEmpty()) {
            if (email.getFrom() == null || !email.getFrom().toLowerCase().contains(fromText)) {
                return false;
            }
        }
        
        // Verificar asunto
        if (!subjectText.isEmpty()) {
            if (email.getSubject() == null || !email.getSubject().toLowerCase().contains(subjectText)) {
                return false;
            }
        }
        
        // Verificar contenido
        if (!contentText.isEmpty()) {
            String body = email.getBody() != null ? email.getBody().toLowerCase() : "";
            if (!body.contains(contentText)) {
                return false;
            }
        }
        
        // Verificar fecha
        if (dateLimit != null && email.getSentDate() != null) {
            LocalDate emailDate = email.getSentDate().toLocalDate();
            LocalDate limitDate = dateLimit.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (emailDate.isBefore(limitDate)) {
                return false;
            }
        }
        
        return true;
    }
    
    private Date calculateDateLimit(String dateRange) {
        if (dateRange.equals("Cualquier fecha")) {
            return null;
        }
        
        LocalDate now = LocalDate.now();
        LocalDate limit;
        
        switch (dateRange) {
            case "Hoy":
                limit = now;
                break;
            case "Última semana":
                limit = now.minusWeeks(1);
                break;
            case "Último mes":
                limit = now.minusMonths(1);
                break;
            case "Últimos 3 meses":
                limit = now.minusMonths(3);
                break;
            case "Último año":
                limit = now.minusYears(1);
                break;
            default:
                return null;
        }
        
        return Date.from(limit.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    
    private void clearSearch() {
        fromField.setText("");
        subjectField.setText("");
        contentField.setText("");
        dateRangeCombo.setSelectedIndex(0);
        tagCombo.setSelectedIndex(0);
        allFoldersCheck.setSelected(true);
        folderCombo.setEnabled(false);
        resultsTableModel.setRowCount(0);
        searchResults.clear();
        
        JLabel countLabel = (JLabel) resultsPanel.getComponent(1);
        countLabel.setText("0 resultados encontrados");
    }
    
    public List<EmailMessage> getSearchResults() {
        return new ArrayList<>(searchResults);
    }
}
