package com.gestorcorreo.ui;

import com.gestorcorreo.model.EmailConfig;
import com.gestorcorreo.service.ConfigService;
import com.gestorcorreo.service.EmailStorageService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Diálogo para gestionar las cuentas de correo guardadas
 */
public class AccountManagerDialog extends JDialog {
    
    private JTable accountsTable;
    private DefaultTableModel tableModel;
    private JButton editButton;
    private JButton deleteButton;
    private JButton setDefaultButton;
    
    public AccountManagerDialog(JFrame parent) {
        super(parent, "Administrar cuentas", true);
        initComponents();
        loadAccounts();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(700, 450);
        setLocationRelativeTo(getParent());
        
        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Título
        JLabel titleLabel = new JLabel("Cuentas de correo configuradas");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Tabla de cuentas
        String[] columnNames = {"Nombre", "Email", "Tipo", "SMTP", "IMAP", "Predeterminada"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // No editable directamente
            }
        };
        
        accountsTable = new JTable(tableModel);
        accountsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountsTable.setRowHeight(25);
        accountsTable.getTableHeader().setReorderingAllowed(false);
        
        // Ajustar anchos de columnas
        accountsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        accountsTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        accountsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        accountsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        accountsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        accountsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        
        // Menú contextual
        JPopupMenu popupMenu = createPopupMenu();
        accountsTable.setComponentPopupMenu(popupMenu);
        
        // Doble clic para editar
        accountsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedAccount();
                }
            }
        });
        
        // Listener para habilitar/deshabilitar botones
        accountsTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = accountsTable.getSelectedRow() != -1;
            editButton.setEnabled(hasSelection);
            deleteButton.setEnabled(hasSelection);
            setDefaultButton.setEnabled(hasSelection);
        });
        
        JScrollPane scrollPane = new JScrollPane(accountsTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Panel de botones a la derecha
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));
        
        JButton newButton = new JButton("Nueva cuenta");
        newButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        newButton.setMaximumSize(new Dimension(150, 30));
        newButton.addActionListener(e -> addNewAccount());
        
        editButton = new JButton("Editar");
        editButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        editButton.setMaximumSize(new Dimension(150, 30));
        editButton.setEnabled(false);
        editButton.addActionListener(e -> editSelectedAccount());
        
        deleteButton = new JButton("Eliminar");
        deleteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        deleteButton.setMaximumSize(new Dimension(150, 30));
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedAccount());
        
        setDefaultButton = new JButton("Predeterminada");
        setDefaultButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        setDefaultButton.setMaximumSize(new Dimension(150, 30));
        setDefaultButton.setEnabled(false);
        setDefaultButton.addActionListener(e -> setAsDefault());
        
        JButton refreshButton = new JButton("Actualizar");
        refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshButton.setMaximumSize(new Dimension(150, 30));
        refreshButton.addActionListener(e -> loadAccounts());
        
        JButton closeButton = new JButton("Cerrar");
        closeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        closeButton.setMaximumSize(new Dimension(150, 30));
        closeButton.addActionListener(e -> dispose());
        
        rightPanel.add(newButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(editButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(deleteButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(setDefaultButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(refreshButton);
        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(closeButton);
        
        add(rightPanel, BorderLayout.EAST);
    }
    
    /**
     * Crea el menú contextual para la tabla
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenuItem editItem = new JMenuItem("Editar cuenta");
        editItem.addActionListener(e -> editSelectedAccount());
        popupMenu.add(editItem);
        
        JMenuItem deleteItem = new JMenuItem("Eliminar cuenta");
        deleteItem.addActionListener(e -> deleteSelectedAccount());
        popupMenu.add(deleteItem);
        
        popupMenu.addSeparator();
        
        JMenuItem setDefaultItem = new JMenuItem("Establecer como predeterminada");
        setDefaultItem.addActionListener(e -> setAsDefault());
        popupMenu.add(setDefaultItem);
        
        popupMenu.addSeparator();
        
        JMenuItem testItem = new JMenuItem("Probar conexión");
        testItem.addActionListener(e -> testConnection());
        popupMenu.add(testItem);
        
        JMenuItem viewDetailsItem = new JMenuItem("Ver detalles");
        viewDetailsItem.addActionListener(e -> viewDetails());
        popupMenu.add(viewDetailsItem);
        
        popupMenu.addSeparator();
        
        JMenuItem refreshItem = new JMenuItem("Actualizar lista");
        refreshItem.addActionListener(e -> loadAccounts());
        popupMenu.add(refreshItem);
        
        return popupMenu;
    }
    
    /**
     * Carga las cuentas en la tabla
     */
    private void loadAccounts() {
        tableModel.setRowCount(0); // Limpiar tabla
        
        List<EmailConfig> accounts = ConfigService.getInstance().getAllAccounts();
        
        if (accounts.isEmpty()) {
            // Mostrar mensaje si no hay cuentas
            JLabel noAccountsLabel = new JLabel("No hay cuentas configuradas");
            noAccountsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noAccountsLabel.setForeground(Color.GRAY);
        } else {
            for (EmailConfig account : accounts) {
                Object[] row = {
                    account.getAccountName() != null ? account.getAccountName() : "Sin nombre",
                    account.getEmail() != null ? account.getEmail() : "Sin email",
                    account.getAccountType() != null ? account.getAccountType() : "Personalizado",
                    account.getSmtpHost() + ":" + account.getSmtpPort(),
                    account.getImapHost() + ":" + account.getImapPort(),
                    account.isDefaultAccount() ? "Sí" : "No"
                };
                tableModel.addRow(row);
            }
        }
    }
    
    /**
     * Añade una nueva cuenta
     */
    private void addNewAccount() {
        NewAccountDialog.showDialog((JFrame) getParent());
        loadAccounts(); // Recargar después de añadir
    }
    
    /**
     * Edita la cuenta seleccionada
     */
    private void editSelectedAccount() {
        int selectedRow = accountsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una cuenta para editar",
                "Información",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String accountName = (String) tableModel.getValueAt(selectedRow, 0);
        String email = (String) tableModel.getValueAt(selectedRow, 1);
        
        // Buscar la cuenta
        EmailConfig account = findAccount(email);
        if (account != null) {
            EditAccountDialog.showDialog((JFrame) getParent(), account);
            loadAccounts(); // Recargar después de editar
        }
    }
    
    /**
     * Elimina la cuenta seleccionada
     */
    private void deleteSelectedAccount() {
        int selectedRow = accountsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una cuenta para eliminar",
                "Información",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String accountName = (String) tableModel.getValueAt(selectedRow, 0);
        String email = (String) tableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea eliminar la cuenta?\n\n" +
            "Nombre: " + accountName + "\n" +
            "Email: " + email,
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            ConfigService.getInstance().removeAccount(email);
            
            // Eliminar correos guardados de esta cuenta
            EmailStorageService.getInstance().deleteAccountEmails(email);
            
            loadAccounts(); // Recargar después de eliminar
            
            // Actualizar el combo de cuentas en la ventana principal
            if (getOwner() instanceof MainWindow) {
                ((MainWindow) getOwner()).refreshAccountCombo();
            }
            
            JOptionPane.showMessageDialog(this,
                "Cuenta eliminada correctamente",
                "Éxito",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Establece la cuenta seleccionada como predeterminada
     */
    private void setAsDefault() {
        int selectedRow = accountsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una cuenta",
                "Información",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String email = (String) tableModel.getValueAt(selectedRow, 1);
        ConfigService.getInstance().setDefaultAccount(email);
        loadAccounts(); // Recargar para mostrar el cambio
        
        // Actualizar el combo de cuentas en la ventana principal
        if (getOwner() instanceof MainWindow) {
            ((MainWindow) getOwner()).refreshAccountCombo();
        }
        
        JOptionPane.showMessageDialog(this,
            "Cuenta establecida como predeterminada",
            "Éxito",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Prueba la conexión de la cuenta seleccionada
     */
    private void testConnection() {
        int selectedRow = accountsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una cuenta para probar",
                "Información",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String smtp = (String) tableModel.getValueAt(selectedRow, 3);
        String imap = (String) tableModel.getValueAt(selectedRow, 4);
        
        JOptionPane.showMessageDialog(this,
            "Probando conexión...\n\n" +
            "SMTP: " + smtp + "\n" +
            "IMAP: " + imap + "\n\n" +
            "(Esta funcionalidad está en desarrollo)",
            "Probar conexión",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Muestra los detalles de la cuenta seleccionada
     */
    private void viewDetails() {
        int selectedRow = accountsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una cuenta para ver detalles",
                "Información",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String email = (String) tableModel.getValueAt(selectedRow, 1);
        EmailConfig account = findAccount(email);
        
        if (account != null) {
            String details = "DETALLES DE LA CUENTA\n\n" +
                "Nombre: " + account.getAccountName() + "\n" +
                "Email: " + account.getEmail() + "\n" +
                "Tipo: " + account.getAccountType() + "\n" +
                "Nombre para mostrar: " + account.getDisplayName() + "\n\n" +
                "SERVIDOR SMTP (Salida)\n" +
                "Host: " + account.getSmtpHost() + "\n" +
                "Puerto: " + account.getSmtpPort() + "\n\n" +
                "SERVIDOR IMAP (Entrada)\n" +
                "Host: " + account.getImapHost() + "\n" +
                "Puerto: " + account.getImapPort() + "\n\n" +
                "OPCIONES\n" +
                "SSL: " + (account.isUseSsl() ? "Activado" : "Desactivado") + "\n" +
                "TLS: " + (account.isUseTls() ? "Activado" : "Desactivado") + "\n" +
                "Autenticación: " + (account.isUseAuth() ? "Activada" : "Desactivada") + "\n" +
                "Cuenta predeterminada: " + (account.isDefaultAccount() ? "Sí" : "No");
            
            JTextArea textArea = new JTextArea(details);
            textArea.setEditable(false);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(450, 350));
            
            JOptionPane.showMessageDialog(this,
                scrollPane,
                "Detalles de la cuenta",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Busca una cuenta por email
     */
    private EmailConfig findAccount(String email) {
        List<EmailConfig> accounts = ConfigService.getInstance().getAllAccounts();
        for (EmailConfig account : accounts) {
            if (account.getEmail().equals(email)) {
                return account;
            }
        }
        return null;
    }
    
    public static void showDialog(JFrame parent) {
        AccountManagerDialog dialog = new AccountManagerDialog(parent);
        dialog.setVisible(true);
    }
}
