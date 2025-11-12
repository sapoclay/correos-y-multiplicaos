package com.gestorcorreo.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import com.gestorcorreo.service.ConfigService;
import com.gestorcorreo.service.EmailCheckService;
import com.gestorcorreo.service.EmailReceiveService;
import com.gestorcorreo.service.EmailStorageService;
import com.gestorcorreo.model.EmailConfig;
import com.gestorcorreo.model.EmailMessage;
import com.gestorcorreo.model.Tag;
import com.gestorcorreo.service.TagService;
import java.util.List;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;

/**
 * Ventana principal del gestor de correos "Correos Y Multiplicaos"
 */
public class MainWindow extends JFrame {
    private JPanel toolbarPanel;
    private JPanel statusPanel;
    private SystemTrayManager trayManager;
    private JComboBox<String> accountCombo;
    private JLabel statusLabel;
    private JTree foldersTree;
    private JPanel foldersPanel;
    private JTable emailTable;
    private javax.swing.table.DefaultTableModel emailTableModel;
    private HtmlViewerPanel htmlViewerPanel;
    private JPanel emailHeaderPanel;
    private JPanel contentWarningPanel;
    private List<EmailMessage> currentEmails;
    private EmailMessage currentDisplayedEmail;
    private boolean allowExternalContent = false;
    private java.util.Set<String> emailsWithExternalContentLoaded = new java.util.HashSet<>(); // IDs de emails con contenido externo cargado
    private String currentAccountEmail; // Email de la cuenta actual
    private String currentFolderName = "INBOX"; // Carpeta actual (por defecto INBOX)
    private JPanel tagFilterPanel; // Panel de filtro de etiquetas
    private String currentTagFilter = null; // Etiqueta actualmente filtrada
    // Claves de correos a resaltar temporalmente
    private java.util.Set<String> highlightNewKeys = new java.util.HashSet<>();
    
    public MainWindow() {
        currentEmails = new ArrayList<>();
        initComponents();
        initSystemTray();
        initEmailCheckService();
        initAppointmentReminderService();
        
        // Cargar correos guardados de la cuenta seleccionada al iniciar
        SwingUtilities.invokeLater(() -> {
            loadStoredEmailsForSelectedAccount();
        });
    }
    
    private void initComponents() {
        setTitle("Correos Y Multiplicaos - Gestor de Correo Electrónico");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // Ocultar en lugar de cerrar
        setSize(1000, 700);
        setLocationRelativeTo(null); // Centrar en la pantalla
        
        // Agregar listener para limpiar claves al cerrar la ventana
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                com.gestorcorreo.security.SecureLogger.info("Ventana ocultándose...");
            }
        });
        
        // Iniciar auto-lock tras inactividad
        com.gestorcorreo.security.AutoLockService.getInstance().start(this, () -> {
            com.gestorcorreo.security.SecureLogger.security("Aplicación bloqueada automáticamente");
        });
        
        // Crear el panel principal con BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior - Barra de herramientas
        JPanel toolbarPanel = createToolbarPanel();
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);
        
        // Panel central - Split pane con lista de correos y visor
        JSplitPane splitPane = createCentralSplitPane();
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // Panel inferior - Barra de estado
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
        
        // Crear la barra de menú
        setJMenuBar(createMenuBar());
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Menú Archivo
        JMenu fileMenu = new JMenu("Archivo");
        
        JMenuItem newAccountItem = new JMenuItem("Nueva cuenta...");
        newAccountItem.addActionListener(e -> NewAccountDialog.showDialog(this));
        
        JMenuItem manageAccountsItem = new JMenuItem("Administrar cuentas...");
        manageAccountsItem.addActionListener(e -> AccountManagerDialog.showDialog(this));
        
        JMenuItem configItem = new JMenuItem("Configuración...");
        configItem.addActionListener(e -> ConfigDialog.showDialog(this));
        
        JMenuItem lockItem = new JMenuItem("🔒 Bloquear aplicación");
        lockItem.addActionListener(e -> {
            com.gestorcorreo.security.AutoLockService.getInstance().lockManually();
        });
        
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> exitApplication());
        
        fileMenu.add(newAccountItem);
        fileMenu.add(manageAccountsItem);
        fileMenu.addSeparator();
        fileMenu.add(configItem);
        fileMenu.add(lockItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        
        // Menú Correo
        JMenu mailMenu = new JMenu("Correo");
        
        JMenuItem newMailItem = new JMenuItem("Nuevo mensaje");
        newMailItem.addActionListener(e -> {
            EmailConfig defaultAccount = ConfigService.getInstance().getDefaultAccount();
            if (defaultAccount == null) {
                JOptionPane.showMessageDialog(this,
                    "No hay ninguna cuenta configurada.\nPor favor, añada una cuenta en Archivo > Nueva cuenta",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Abrir ventana nativa JavaFX (sin Electron)
            JavaFXComposeWindow.open(defaultAccount);
        });
        
        JMenuItem replyItem = new JMenuItem("Responder");
        replyItem.addActionListener(e -> {
            int selectedRow = emailTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < currentEmails.size()) {
                EmailMessage selectedEmail = currentEmails.get(selectedRow);
                replyToEmail(selectedEmail);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Selecciona un mensaje para responder",
                    "Responder",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        JMenuItem forwardItem = new JMenuItem("Reenviar");
        forwardItem.addActionListener(e -> {
            int selectedRow = emailTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < currentEmails.size()) {
                EmailMessage selectedEmail = currentEmails.get(selectedRow);
                forwardEmail(selectedEmail);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Selecciona un mensaje para reenviar",
                    "Reenviar",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        JMenuItem deleteItem = new JMenuItem("Eliminar");
        deleteItem.addActionListener(e -> {
            int selectedRow = emailTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < currentEmails.size()) {
                deleteEmail(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Selecciona un mensaje para eliminar",
                    "Eliminar",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        JMenuItem emptyTrashItem = new JMenuItem("Vaciar Papelera");
        emptyTrashItem.addActionListener(e -> emptyTrash());
        
        mailMenu.add(newMailItem);
        mailMenu.add(replyItem);
        mailMenu.add(forwardItem);
        mailMenu.addSeparator();
        mailMenu.add(deleteItem);
        mailMenu.add(emptyTrashItem);
        menuBar.add(mailMenu);
        
        // Menú Buscar
        JMenu searchMenu = new JMenu("Buscar");
        
        JMenuItem advancedSearchItem = new JMenuItem("Búsqueda avanzada...");
        advancedSearchItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, 
                                         java.awt.event.InputEvent.CTRL_DOWN_MASK));
        advancedSearchItem.addActionListener(e -> showAdvancedSearch());
        
        searchMenu.add(advancedSearchItem);
        menuBar.add(searchMenu);
        
        // Menú Etiquetas
        JMenu tagsMenu = new JMenu("Etiquetas");
        
        JMenuItem manageTagsItem = new JMenuItem("Gestionar etiquetas...");
        manageTagsItem.addActionListener(e -> TagManagerDialog.showDialog(this));
        
        tagsMenu.add(manageTagsItem);
        menuBar.add(tagsMenu);
        
        // Menú Contactos
        JMenu contactsMenu = new JMenu("Contactos");
        
        JMenuItem addressBookItem = new JMenuItem("Libreta de direcciones...");
        addressBookItem.addActionListener(e -> AddressBookDialog.showDialog(this));
        
        contactsMenu.add(addressBookItem);
        menuBar.add(contactsMenu);
        
        // Menú Ver
        JMenu viewMenu = new JMenu("Ver");
        
        JCheckBoxMenuItem toolbarCheck = new JCheckBoxMenuItem("Barra de herramientas", true);
        toolbarCheck.addActionListener(e -> {
            toolbarPanel.setVisible(toolbarCheck.isSelected());
        });
        
        JCheckBoxMenuItem statusCheck = new JCheckBoxMenuItem("Barra de estado", true);
        statusCheck.addActionListener(e -> {
            statusPanel.setVisible(statusCheck.isSelected());
        });
        
        viewMenu.add(toolbarCheck);
        viewMenu.add(statusCheck);
        menuBar.add(viewMenu);

        // Menú Calendario
        JMenu calendarMenu = new JMenu("Calendario");
        JMenuItem openCalendarItem = new JMenuItem("Abrir calendario...");
        openCalendarItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C,
                java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        openCalendarItem.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                com.gestorcorreo.model.AppointmentService srv = new com.gestorcorreo.model.AppointmentService();
                com.gestorcorreo.model.CalendarDialog dlg = new com.gestorcorreo.model.CalendarDialog(this, srv);
                dlg.setVisible(true);
            });
        });
        calendarMenu.add(openCalendarItem);
        menuBar.add(calendarMenu);
        
        // Menú Ayuda
        JMenu helpMenu = new JMenu("Ayuda");
        
        JMenuItem aboutItem = new JMenuItem("Acerca de...");
        aboutItem.addActionListener(e -> AboutDialog.showDialog(this));
        
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        
        return menuBar;
    }
    
    private JPanel createToolbarPanel() {
        toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolbarPanel.setBorder(BorderFactory.createEtchedBorder());
        
        // Selector de cuenta
        JLabel accountLabel = new JLabel("Cuenta: ");
        toolbarPanel.add(accountLabel);
        
        accountCombo = new JComboBox<>();
        accountCombo.setPreferredSize(new Dimension(250, 25));
        loadAccountsToCombo();
        accountCombo.addActionListener(e -> {
            String selectedAccount = (String) accountCombo.getSelectedItem();
            if (selectedAccount != null && !selectedAccount.equals("Sin cuentas configuradas")) {
                updateStatusLabel("Cuenta activa: " + selectedAccount);
                // Cargar correos guardados de esta cuenta
                loadStoredEmailsForSelectedAccount();
            }
        });
        toolbarPanel.add(accountCombo);
        
        toolbarPanel.add(new JSeparator(SwingConstants.VERTICAL));
        
        JButton newMailBtn = new JButton("Nuevo");
        newMailBtn.addActionListener(e -> {
            EmailConfig defaultAccount = ConfigService.getInstance().getDefaultAccount();
            if (defaultAccount == null) {
                JOptionPane.showMessageDialog(this,
                    "No hay ninguna cuenta configurada.\nPor favor, añada una cuenta en Archivo > Nueva cuenta",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            JavaFXComposeWindow.open(defaultAccount);
        });
        
        JButton replyBtn = new JButton("Responder");
        replyBtn.addActionListener(e -> {
            int selectedRow = emailTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < currentEmails.size()) {
                EmailMessage selectedEmail = currentEmails.get(selectedRow);
                replyToEmail(selectedEmail);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Selecciona un mensaje para responder",
                    "Responder",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        JButton forwardBtn = new JButton("Reenviar");
        forwardBtn.addActionListener(e -> {
            int selectedRow = emailTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < currentEmails.size()) {
                EmailMessage selectedEmail = currentEmails.get(selectedRow);
                forwardEmail(selectedEmail);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Selecciona un mensaje para reenviar",
                    "Reenviar",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        JButton deleteBtn = new JButton("Eliminar");
        deleteBtn.addActionListener(e -> {
            int selectedRow = emailTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < currentEmails.size()) {
                deleteEmail(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Selecciona un mensaje para eliminar",
                    "Eliminar",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        JButton refreshBtn = new JButton("Descargar correos");
        refreshBtn.addActionListener(e -> downloadEmails());
        
        toolbarPanel.add(newMailBtn);
        toolbarPanel.add(replyBtn);
        toolbarPanel.add(forwardBtn);
        toolbarPanel.add(new JSeparator(SwingConstants.VERTICAL));
        toolbarPanel.add(deleteBtn);
        toolbarPanel.add(new JSeparator(SwingConstants.VERTICAL));
        toolbarPanel.add(refreshBtn);
        
        return toolbarPanel;
    }
    
    /**
     * Carga las cuentas disponibles en el combo
     */
    private void loadAccountsToCombo() {
        accountCombo.removeAllItems();
        List<EmailConfig> accounts = ConfigService.getInstance().getAllAccounts();
        
        if (accounts.isEmpty()) {
            accountCombo.addItem("Sin cuentas configuradas");
        } else {
            for (EmailConfig account : accounts) {
                String displayName = account.getEmail();
                if (account.isDefaultAccount()) {
                    displayName += " (predeterminada)";
                }
                accountCombo.addItem(displayName);
            }
        }
    }
    
    /**
     * Actualiza el combo de cuentas y el árbol de carpetas (llamar después de añadir/eliminar cuentas)
     */
    public void refreshAccountCombo() {
        loadAccountsToCombo();
        refreshFoldersTree();
    }
    
    /**
     * Actualiza el texto de la barra de estado
     */
    private void updateStatusLabel(String text) {
        if (statusLabel != null) {
            statusLabel.setText(" " + text);
        }
    }
    
    private JSplitPane createCentralSplitPane() {
        // Panel izquierdo - Carpetas
        JPanel foldersPanel = createFoldersPanel();
        
        // Panel derecho - Split vertical con lista de correos y visor de mensaje
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setTopComponent(createEmailListPanel());
        rightSplitPane.setBottomComponent(createEmailViewerPanel());
        rightSplitPane.setDividerLocation(300);
        
        // Split principal horizontal
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(foldersPanel);
        mainSplitPane.setRightComponent(rightSplitPane);
        mainSplitPane.setDividerLocation(200);
        
        return mainSplitPane;
    }
    
    private JPanel createFoldersPanel() {
        foldersPanel = new JPanel(new BorderLayout());
        foldersPanel.setBorder(BorderFactory.createTitledBorder("Carpetas"));
        
        // Crear el árbol con las cuentas actuales
        updateFoldersTree();
        
        return foldersPanel;
    }
    
    /**
     * Actualiza el árbol de carpetas con las cuentas configuradas
     */
    private void updateFoldersTree() {
        // Crear el nodo raíz
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Cuentas");
        
        // Obtener todas las cuentas configuradas
        List<EmailConfig> accounts = ConfigService.getInstance().getAllAccounts();
        
        if (accounts.isEmpty()) {
            // Si no hay cuentas, mostrar un mensaje
            DefaultMutableTreeNode noAccounts = new DefaultMutableTreeNode("Sin cuentas configuradas");
            root.add(noAccounts);
        } else {
            // Añadir cada cuenta con sus carpetas
            for (EmailConfig account : accounts) {
                DefaultMutableTreeNode accountNode = new DefaultMutableTreeNode(account.getEmail());
                accountNode.add(new DefaultMutableTreeNode("Bandeja de entrada"));
                accountNode.add(new DefaultMutableTreeNode("Enviados"));
                accountNode.add(new DefaultMutableTreeNode("Borradores"));
                accountNode.add(new DefaultMutableTreeNode("Papelera"));
                accountNode.add(new DefaultMutableTreeNode("Spam"));
                root.add(accountNode);
            }
        }
        
        // Crear el árbol
        foldersTree = new JTree(root);
        // Renderer para mostrar badge (N) en "Bandeja de entrada"
        foldersTree.setCellRenderer(new javax.swing.tree.DefaultTreeCellRenderer() {
            @Override
            public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                java.awt.Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    Object user = node.getUserObject();
                    if (user != null) {
                        String text = user.toString();
                        javax.swing.tree.TreeNode parent = node.getParent();
                        if (parent instanceof DefaultMutableTreeNode) {
                            DefaultMutableTreeNode p = (DefaultMutableTreeNode) parent;
                            Object pu = p.getUserObject();
                            if (pu != null && !"Cuentas".equals(pu.toString())) {
                                String accountEmail = pu.toString();
                                if ("Bandeja de entrada".equals(text)) {
                                    int badge = com.gestorcorreo.service.NewEmailHighlightService.getInstance().getBadge(accountEmail);
                                    if (badge > 0) {
                                        setText(text + " (" + badge + ")");
                                    } else {
                                        setText(text);
                                    }
                                } else {
                                    setText(text);
                                }
                            }
                        }
                    }
                }
                return comp;
            }
        });
        foldersTree.setRootVisible(true);
        
        // Añadir listener para clics en el árbol
        foldersTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showFolderTreeContextMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showFolderTreeContextMenu(e);
                }
            }
            
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 1 && !e.isPopupTrigger()) {
                    // Clic simple (no menú contextual)
                    MainWindow.this.handleFolderTreeClick();
                }
            }
        });
        
        // Expandir todos los nodos
        for (int i = 0; i < foldersTree.getRowCount(); i++) {
            foldersTree.expandRow(i);
        }
        
        // Actualizar el panel
        foldersPanel.removeAll();
        JScrollPane scrollPane = new JScrollPane(foldersTree);
        foldersPanel.add(scrollPane, BorderLayout.CENTER);
        foldersPanel.revalidate();
        foldersPanel.repaint();
    }
    
    /**
     * Muestra el menú contextual del árbol de carpetas
     */
    private void showFolderTreeContextMenu(java.awt.event.MouseEvent e) {
        // Seleccionar el nodo donde se hizo clic
        int row = foldersTree.getRowForLocation(e.getX(), e.getY());
        if (row == -1) {
            return; // No hay nodo en esa posición
        }
        
        foldersTree.setSelectionRow(row);
        javax.swing.tree.TreePath path = foldersTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }
        
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        String nodeText = selectedNode.getUserObject().toString();
        
        // Determinar si es una cuenta o una carpeta
        boolean isAccountNode = selectedNode.getParent() != null && 
                                selectedNode.getParent().toString().equals("Cuentas") &&
                                !nodeText.equals("Sin cuentas configuradas");
        
        if (isAccountNode) {
            // Menú para nodos de cuenta
            JPopupMenu accountMenu = createAccountContextMenu(nodeText);
            accountMenu.show(foldersTree, e.getX(), e.getY());
        } else {
            // Menú para carpetas (si es necesario)
            JPopupMenu folderMenu = createFolderContextMenu(nodeText);
            if (folderMenu != null) {
                folderMenu.show(foldersTree, e.getX(), e.getY());
            }
        }
    }
    
    /**
     * Crea el menú contextual para un nodo de cuenta
     */
    private JPopupMenu createAccountContextMenu(String accountEmail) {
        JPopupMenu menu = new JPopupMenu();
        
        // Opción: Descargar correos
        JMenuItem downloadItem = new JMenuItem("📥 Descargar correos");
        downloadItem.addActionListener(e -> {
            // Seleccionar esta cuenta en el combo
            accountCombo.setSelectedItem(accountEmail);
            downloadEmails();
        });
        menu.add(downloadItem);
        
        menu.addSeparator();
        
        // Opción: Nuevo mensaje
        JMenuItem newMessageItem = new JMenuItem("✉️ Nuevo mensaje");
        newMessageItem.addActionListener(e -> {
            accountCombo.setSelectedItem(accountEmail);
            EmailConfig config = ConfigService.getInstance().getAllAccounts().stream()
                .filter(acc -> acc.getEmail().equals(accountEmail))
                .findFirst()
                .orElse(null);
            
                if (config != null) {
                    JavaFXComposeWindow.open(config);
                } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo cargar la configuración de la cuenta",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        menu.add(newMessageItem);
        
        menu.addSeparator();
        
        // Opción: Propiedades de la cuenta
        JMenuItem propertiesItem = new JMenuItem("⚙️ Propiedades de la cuenta");
        propertiesItem.addActionListener(e -> {
            // Buscar la configuración de la cuenta
            EmailConfig config = ConfigService.getInstance().getAllAccounts().stream()
                .filter(acc -> acc.getEmail().equals(accountEmail))
                .findFirst()
                .orElse(null);
            
            if (config != null) {
                EditAccountDialog.showDialog(this, config);
            }
        });
        menu.add(propertiesItem);
        
        // Opción: Editar firma
        JMenuItem signatureItem = new JMenuItem("✍️ Editar firma");
        signatureItem.addActionListener(e -> {
            EmailConfig config = ConfigService.getInstance().getAllAccounts().stream()
                .filter(acc -> acc.getEmail().equals(accountEmail))
                .findFirst()
                .orElse(null);
            
            if (config != null) {
                SignatureEditorDialog.showDialog(this, config);
            }
        });
        menu.add(signatureItem);
        
        menu.addSeparator();
        
        // Opción: Establecer como predeterminada
        JMenuItem setDefaultItem = new JMenuItem("⭐ Establecer como predeterminada");
        setDefaultItem.addActionListener(e -> {
            ConfigService.getInstance().setDefaultAccount(accountEmail);
            refreshAccountCombo();
            refreshFoldersTree();
            updateStatusLabel("Cuenta predeterminada: " + accountEmail);
        });
        menu.add(setDefaultItem);
        
        menu.addSeparator();
        
        // Opción: Ver carpetas disponibles
        JMenuItem listFoldersItem = new JMenuItem("📁 Ver carpetas del servidor");
        listFoldersItem.addActionListener(e -> {
            showAvailableFolders(accountEmail);
        });
        menu.add(listFoldersItem);
        
        menu.addSeparator();
        
        // Opción: Eliminar cuenta
        JMenuItem deleteItem = new JMenuItem("🗑️ Eliminar cuenta");
        deleteItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de que desea eliminar la cuenta?\n\n" +
                "Email: " + accountEmail + "\n\n" +
                "Esta acción también eliminará todos los correos descargados.",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                ConfigService.getInstance().removeAccount(accountEmail);
                EmailStorageService.getInstance().deleteAccountEmails(accountEmail);
                refreshAccountCombo();
                refreshFoldersTree();
                currentEmails.clear();
                updateEmailTable();
                updateStatusLabel("Cuenta eliminada: " + accountEmail);
            }
        });
        menu.add(deleteItem);
        
        return menu;
    }
    
    /**
     * Crea el menú contextual para un nodo de carpeta
     */
    private JPopupMenu createFolderContextMenu(String folderName) {
        // Por ahora, solo para carpetas específicas
        if (folderName.equals("Bandeja de entrada")) {
            JPopupMenu menu = new JPopupMenu();
            
            JMenuItem refreshItem = new JMenuItem("🔄 Actualizar");
            refreshItem.addActionListener(e -> downloadEmails());
            menu.add(refreshItem);
            
            return menu;
        }
        
        return null; // Sin menú para otras carpetas por ahora
    }
    
    /**
     * Maneja el clic sobre una carpeta del árbol para mostrar sus correos
     */
    private void handleFolderTreeClick() {
        TreePath selectedPath = foldersTree.getSelectionPath();
        if (selectedPath == null || selectedPath.getPathCount() < 3) {
            return; // No hay suficiente profundidad (necesitamos: Root -> Cuenta -> Carpeta)
        }
        
        // Obtener el nodo de la cuenta (nivel 1) y el nodo de la carpeta (nivel 2)
        DefaultMutableTreeNode accountNode = (DefaultMutableTreeNode) selectedPath.getPathComponent(1);
        DefaultMutableTreeNode folderNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        
        String accountEmail = accountNode.getUserObject().toString();
        String folderDisplayName = folderNode.getUserObject().toString();
        
        // Mapear nombres de carpetas en español a nombres IMAP estándar
        String imapFolderName;
        switch (folderDisplayName) {
            case "Bandeja de entrada":
                imapFolderName = "INBOX";
                break;
            case "Enviados":
                imapFolderName = "Sent";
                break;
            case "Borradores":
                imapFolderName = "Drafts";
                break;
            case "Papelera":
                imapFolderName = "Trash";
                break;
            case "Spam":
                imapFolderName = "Junk";
                break;
            default:
                imapFolderName = "INBOX";
        }
        
        // Cargar correos de esta carpeta específica
        downloadEmailsFromFolder(accountEmail, imapFolderName);
    }
    
    /**
     * Crea el menú contextual para un correo en la tabla
     */
    private JPopupMenu createEmailContextMenu(int row) {
        JPopupMenu popup = new JPopupMenu();
        
        if (row < 0 || row >= currentEmails.size()) {
            return popup;
        }
        
        EmailMessage email = currentEmails.get(row);
        
        // Opción: Responder
        JMenuItem replyItem = new JMenuItem("↩ Responder");
        replyItem.addActionListener(e -> replyToEmail(email));
        popup.add(replyItem);
        
        // Opción: Reenviar
        JMenuItem forwardItem = new JMenuItem("➡ Reenviar");
        forwardItem.addActionListener(e -> forwardEmail(email));
        popup.add(forwardItem);
        
        popup.addSeparator();
        
        // Opción: Marcar como no leído
        if (email.isRead()) {
            JMenuItem markUnreadItem = new JMenuItem("✉ Marcar como no leído");
            markUnreadItem.addActionListener(e -> {
                email.setRead(false);
                emailTableModel.setValueAt("✉", row, 0);
                
                // Guardar cambios
                String selectedAccount = (String) accountCombo.getSelectedItem();
                if (selectedAccount != null && !selectedAccount.equals("Sin cuentas configuradas")) {
                    String emailAddress = selectedAccount.replace(" (predeterminada)", "");
                    EmailStorageService.getInstance().saveEmails(emailAddress, "INBOX", currentEmails);
                }
            });
            popup.add(markUnreadItem);
        }
        
        popup.addSeparator();
        
        // Opción: Etiquetas
        JMenu tagsSubmenu = new JMenu("🏷️ Etiquetas");
        
        // Listar etiquetas disponibles
        java.util.List<com.gestorcorreo.model.Tag> availableTags = 
            com.gestorcorreo.service.TagService.getInstance().getAllTags();
        
        if (availableTags.isEmpty()) {
            JMenuItem noTagsItem = new JMenuItem("(Sin etiquetas configuradas)");
            noTagsItem.setEnabled(false);
            tagsSubmenu.add(noTagsItem);
        } else {
            // Añadir cada etiqueta como opción
            for (com.gestorcorreo.model.Tag tag : availableTags) {
                JCheckBoxMenuItem tagItem = new JCheckBoxMenuItem(tag.getName());
                
                // Marcar si el correo ya tiene esta etiqueta
                if (email.getTags() != null && email.getTags().contains(tag.getName())) {
                    tagItem.setSelected(true);
                }
                
                // Colorear el fondo según el color de la etiqueta
                try {
                    Color tagColor = Color.decode(tag.getColor());
                    tagItem.setBackground(tagColor);
                    // Calcular color de texto contrastante
                    int luminance = (int) (0.299 * tagColor.getRed() + 
                                          0.587 * tagColor.getGreen() + 
                                          0.114 * tagColor.getBlue());
                    tagItem.setForeground(luminance > 128 ? Color.BLACK : Color.WHITE);
                    tagItem.setOpaque(true);
                } catch (NumberFormatException ex) {
                    // Color inválido, usar defaults
                }
                
                final String tagName = tag.getName();
                tagItem.addActionListener(e -> {
                    if (tagItem.isSelected()) {
                        email.addTag(tagName);
                    } else {
                        email.removeTag(tagName);
                    }
                    saveCurrentEmails();
                    updateEmailTable(); // Refrescar tabla
                });
                
                tagsSubmenu.add(tagItem);
            }
            
            // Agregar separador y opción para eliminar todas las etiquetas
            if (email.getTags() != null && !email.getTags().isEmpty()) {
                tagsSubmenu.addSeparator();
                
                JMenuItem removeAllTagsItem = new JMenuItem("❌ Eliminar todas las etiquetas");
                removeAllTagsItem.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "¿Estás seguro de que deseas eliminar todas las etiquetas de este correo?",
                        "Confirmar eliminación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    );
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        email.getTags().clear();
                        saveCurrentEmails();
                        updateEmailTable();
                        JOptionPane.showMessageDialog(
                            this,
                            "Todas las etiquetas han sido eliminadas del correo.",
                            "Etiquetas eliminadas",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                });
                
                tagsSubmenu.add(removeAllTagsItem);
            }
        }
        
        popup.add(tagsSubmenu);
        
        popup.addSeparator();
        
        // Opción: Eliminar
        JMenuItem deleteItem = new JMenuItem("🗑️ Eliminar");
        deleteItem.addActionListener(e -> deleteEmail(row));
        popup.add(deleteItem);

        // Opción: Restaurar (solo si estamos en Papelera)
        if ("Trash".equalsIgnoreCase(currentFolderName)) {
            JMenuItem restoreItem = new JMenuItem("↩ Restaurar a Bandeja de entrada");
            restoreItem.addActionListener(e -> restoreEmail(row));
            popup.addSeparator();
            popup.add(restoreItem);
        }
        
        return popup;
    }
    
    /**
     * Responde a un correo electrónico
     */
    private void replyToEmail(EmailMessage originalEmail) {
        String selectedAccount = (String) accountCombo.getSelectedItem();
        if (selectedAccount == null || selectedAccount.equals("Sin cuentas configuradas")) {
            JOptionPane.showMessageDialog(this,
                "No hay cuentas configuradas",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String emailAddress = selectedAccount.replace(" (predeterminada)", "");
        EmailConfig config = null;
        for (EmailConfig cfg : ConfigService.getInstance().getAllAccounts()) {
            if (cfg.getEmail().equals(emailAddress)) {
                config = cfg;
                break;
            }
        }
        
        if (config != null) {
            // Abrir nueva ventana JavaFX de respuesta
            JavaFXComposeWindow.openReply(config, originalEmail);
        }
    }
    
    /**
     * Reenvía un correo electrónico
     */
    private void forwardEmail(EmailMessage originalEmail) {
        String selectedAccount = (String) accountCombo.getSelectedItem();
        if (selectedAccount == null || selectedAccount.equals("Sin cuentas configuradas")) {
            JOptionPane.showMessageDialog(this,
                "No hay cuentas configuradas",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String emailAddress = selectedAccount.replace(" (predeterminada)", "");
        EmailConfig config = null;
        for (EmailConfig cfg : ConfigService.getInstance().getAllAccounts()) {
            if (cfg.getEmail().equals(emailAddress)) {
                config = cfg;
                break;
            }
        }
        
        if (config != null) {
            // Abrir nueva ventana JavaFX de reenvío
            JavaFXComposeWindow.openForward(config, originalEmail);
        }
    }
    
    /**
     * Elimina un correo de la lista
     */
    private void deleteEmail(int row) {
        if (row < 0 || row >= currentEmails.size()) {
            return;
        }

        // Determinar si es eliminación permanente (en Papelera) o mover a Papelera
        boolean inTrash = "Trash".equalsIgnoreCase(currentFolderName);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            inTrash ? "¿Eliminar permanentemente este correo de la Papelera?" : "¿Mover este correo a la Papelera?",
            inTrash ? "Eliminar definitivamente" : "Mover a Papelera",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Obtener cuenta seleccionada
        String selectedAccount = (String) accountCombo.getSelectedItem();
        if (selectedAccount == null || selectedAccount.equals("Sin cuentas configuradas")) {
            return;
        }
    String emailAddress = selectedAccount.replace(" (predeterminada)", "");

        // Tomar el correo a eliminar/mover
        EmailMessage removed = currentEmails.get(row);

        // Persistir antes de mutar la lista para minimizar riesgo de condición de carrera con descargas
        if (!inTrash) {
            // Mover a Trash primero
            List<EmailMessage> trashEmails = EmailStorageService.getInstance().loadEmails(emailAddress, "Trash");
            List<EmailMessage> mergedTrash = EmailStorageService.getInstance().mergeEmails(trashEmails, java.util.Arrays.asList(removed));
            EmailStorageService.getInstance().saveEmails(emailAddress, "Trash", mergedTrash);
        }

        // Remover de la lista visible
        currentEmails.remove(row);

        // (Ya movido previamente si no estaba en Trash)

        // Guardar la carpeta actual (refrescar persistencia de donde se eliminó)
        if (currentFolderName != null && !currentFolderName.isEmpty()) {
            EmailStorageService.getInstance().saveEmails(emailAddress, currentFolderName, currentEmails);
        }

        // Actualizar tabla
        updateEmailTable();

        // Limpiar vista previa
        htmlViewerPanel.loadHtmlContent("");
        emailHeaderPanel.removeAll();
        emailHeaderPanel.add(new JLabel("Selecciona un mensaje para ver su contenido"));
        emailHeaderPanel.revalidate();
        emailHeaderPanel.repaint();
        currentDisplayedEmail = null;

        updateStatusLabel(inTrash ? "Correo eliminado permanentemente" : "Correo movido a Papelera (local)");

        // Mover también en el servidor IMAP en segundo plano (mejora de sincronización)
        if (!inTrash) {
            final String srcFolder = currentFolderName != null ? currentFolderName : "INBOX";
            // Obtener configuración de la cuenta
            EmailConfig foundConfig = null;
            for (EmailConfig cfg : ConfigService.getInstance().getAllAccounts()) {
                if (cfg.getEmail().equals(emailAddress)) { foundConfig = cfg; break; }
            }
            if (foundConfig != null) {
                final EmailConfig configFinal = foundConfig;
                EmailMessage refCopy = removed; // referencia
                SwingWorker<Boolean, Void> moveWorker = new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        return EmailReceiveService.moveToTrash(configFinal, srcFolder, refCopy);
                    }
                    @Override
                    protected void done() {
                        try {
                            boolean ok = get();
                            if (ok) {
                                updateStatusLabel("Correo movido a Papelera en el servidor");
                            } else {
                                updateStatusLabel("Correo movido localmente. No se pudo mover en servidor");
                            }
                        } catch (Exception ex) {
                            updateStatusLabel("Correo movido localmente. Error al mover en servidor");
                        }
                    }
                };
                moveWorker.execute();
            }
        }
    }

    /**
     * Vacía por completo la Papelera de la cuenta seleccionada tras confirmación
     */
    private void emptyTrash() {
        String selectedAccount = (String) accountCombo.getSelectedItem();
        if (selectedAccount == null || selectedAccount.equals("Sin cuentas configuradas")) {
            JOptionPane.showMessageDialog(this,
                "No hay cuentas configuradas",
                "Vaciar Papelera",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String emailAddress = selectedAccount.replace(" (predeterminada)", "");

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "¿Eliminar definitivamente todos los correos de la Papelera? Esta acción no se puede deshacer.",
            "Vaciar Papelera",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Guardar lista vacía en Trash
        EmailStorageService.getInstance().saveEmails(emailAddress, "Trash", new java.util.ArrayList<>());

        // Si la carpeta visible actualmente es Trash, limpiar también la vista
        if ("Trash".equalsIgnoreCase(currentFolderName)) {
            currentEmails.clear();
            updateEmailTable();
            htmlViewerPanel.loadHtmlContent("");
            emailHeaderPanel.removeAll();
            emailHeaderPanel.add(new JLabel("Selecciona un mensaje para ver su contenido"));
            emailHeaderPanel.revalidate();
            emailHeaderPanel.repaint();
            currentDisplayedEmail = null;
        }

        updateStatusLabel("Papelera vaciada");
    }

    /**
     * Restaura un correo desde la Papelera a la Bandeja de entrada
     */
    private void restoreEmail(int row) {
        if (!"Trash".equalsIgnoreCase(currentFolderName)) {
            return; // Solo válido cuando estamos en la Papelera
        }
        if (row < 0 || row >= currentEmails.size()) {
            return;
        }

        String selectedAccount = (String) accountCombo.getSelectedItem();
        if (selectedAccount == null || selectedAccount.equals("Sin cuentas configuradas")) {
            return;
        }
        String emailAddress = selectedAccount.replace(" (predeterminada)", "");

        EmailMessage toRestore = currentEmails.remove(row);

        // Guardar nueva Papelera (sin el correo restaurado)
        EmailStorageService.getInstance().saveEmails(emailAddress, "Trash", currentEmails);

        // Cargar INBOX, añadir el correo restaurado si no existe y guardar
        List<EmailMessage> inboxEmails = EmailStorageService.getInstance().loadEmails(emailAddress, "INBOX");
        List<EmailMessage> mergedInbox = EmailStorageService.getInstance().mergeEmails(inboxEmails, java.util.Arrays.asList(toRestore));
        EmailStorageService.getInstance().saveEmails(emailAddress, "INBOX", mergedInbox);

        // Actualizar vista (seguimos en Papelera, así que solo reflejar eliminación allí)
        updateEmailTable();
        htmlViewerPanel.loadHtmlContent("");
        emailHeaderPanel.removeAll();
        emailHeaderPanel.add(new JLabel("Selecciona un mensaje para ver su contenido"));
        emailHeaderPanel.revalidate();
        emailHeaderPanel.repaint();
        currentDisplayedEmail = null;

        updateStatusLabel("Correo restaurado a la Bandeja de entrada (local)");

        // Restaurar también en el servidor IMAP en segundo plano
        final String srcFolder = currentFolderName != null ? currentFolderName : "Trash";
        EmailConfig foundConfig = null;
        for (EmailConfig cfg : ConfigService.getInstance().getAllAccounts()) {
            if (cfg.getEmail().equals(emailAddress)) { foundConfig = cfg; break; }
        }
        if (foundConfig != null) {
            final EmailConfig configFinal = foundConfig;
            final EmailMessage refCopy = toRestore;
            SwingWorker<Boolean, Void> restoreWorker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return EmailReceiveService.moveMessageToFolder(configFinal, srcFolder, "INBOX", refCopy);
                }
                @Override
                protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            updateStatusLabel("Correo restaurado en el servidor a INBOX");
                        } else {
                            updateStatusLabel("Correo restaurado localmente. No se pudo restaurar en servidor");
                        }
                    } catch (Exception ex) {
                        updateStatusLabel("Correo restaurado localmente. Error al restaurar en servidor");
                    }
                }
            };
            restoreWorker.execute();
        }
    }
    
    /**
     * Muestra un diálogo con las carpetas disponibles en el servidor
     */
    private void showAvailableFolders(String accountEmail) {
        // Obtener la cuenta
        EmailConfig account = null;
        for (EmailConfig config : ConfigService.getInstance().getAllAccounts()) {
            if (config.getEmail().equals(accountEmail)) {
                account = config;
                break;
            }
        }
        
        if (account == null) {
            return;
        }
        
        final EmailConfig finalAccount = account;
        
        // Crear diálogo de progreso
        JDialog progressDialog = new JDialog(this, "Consultando carpetas", true);
        progressDialog.setLayout(new BorderLayout(10, 10));
        progressDialog.setSize(300, 100);
        progressDialog.setLocationRelativeTo(this);
        
        JLabel statusLabel = new JLabel("Conectando al servidor...", SwingConstants.CENTER);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.add(statusLabel, BorderLayout.NORTH);
        contentPanel.add(progressBar, BorderLayout.CENTER);
        progressDialog.add(contentPanel);
        
        // Consultar carpetas en segundo plano
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return EmailReceiveService.listFolders(finalAccount);
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    List<String> folders = get();
                    
                    // Crear mensaje con la lista de carpetas
                    StringBuilder message = new StringBuilder();
                    message.append("Carpetas disponibles en el servidor:\n\n");
                    for (String folder : folders) {
                        message.append("• ").append(folder).append("\n");
                    }
                    
                    // Mostrar en un área de texto con scroll
                    JTextArea textArea = new JTextArea(message.toString());
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(500, 400));
                    
                    JOptionPane.showMessageDialog(MainWindow.this,
                        scrollPane,
                        "Carpetas del servidor - " + accountEmail,
                        JOptionPane.INFORMATION_MESSAGE);
                        
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainWindow.this,
                        "Error al consultar las carpetas: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    /**
     * Obtiene nombres alternativos para carpetas IMAP según diferentes proveedores
     */
    private String[] getAlternativeFolderNames(String folderName) {
        switch (folderName) {
            case "Sent":
                return new String[]{
                    "[Gmail]/Enviados",         // Gmail español
                    "[Gmail]/Sent Mail",        // Gmail inglés
                    "Sent Items",               // Outlook
                    "Sent Mail",                // Otros
                    "Enviados",                 // Genérico español
                    "[Google Mail]/Enviados"    // Google Mail español (algunos países)
                };
            case "Drafts":
                return new String[]{
                    "[Gmail]/Borradores",       // Gmail español
                    "[Gmail]/Drafts",           // Gmail inglés
                    "Draft",                    // Otros
                    "Drafts",                   // Variante plural
                    "Borradores",               // Genérico español
                    "[Google Mail]/Borradores"  // Google Mail español
                };
            case "Trash":
                return new String[]{
                    "[Gmail]/Papelera",         // Gmail español
                    "[Gmail]/Trash",            // Gmail inglés
                    "Deleted",                  // Otros
                    "Deleted Items",            // Outlook
                    "Deleted Messages",         // Apple Mail / IMAP genérico
                    "Papelera",                 // Genérico español
                    "INBOX.Trash",              // Dovecot/cyrus notación
                    "INBOX/Trash",              // Notación alternativa
                    "[Google Mail]/Papelera",   // Google Mail español
                    "[Google Mail]/Trash"       // Google Mail inglés
                };
            case "Junk":
                return new String[]{
                    "[Gmail]/Spam",             // Gmail (mismo en español e inglés)
                    "Spam",                     // Genérico
                    "Correo no deseado",        // Español
                    "Junk",                     // Variante
                    "Junk Mail",                // Otros
                    "[Google Mail]/Spam"        // Google Mail
                };
            default:
                return new String[]{};
        }
    }
    
    /**
     * Descarga correos de una carpeta específica de una cuenta
     */
    private void downloadEmailsFromFolder(String emailAddress, String folderName) {
        // Obtener la cuenta
        EmailConfig account = null;
        for (EmailConfig config : ConfigService.getInstance().getAllAccounts()) {
            if (config.getEmail().equals(emailAddress)) {
                account = config;
                break;
            }
        }
        
        if (account == null) {
            JOptionPane.showMessageDialog(this,
                "No se pudo encontrar la configuración de la cuenta",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        final EmailConfig finalAccount = account;
        final String finalFolderName = folderName;
        
        // Establecer cuenta y carpeta actual
        this.currentAccountEmail = emailAddress;
        this.currentFolderName = folderName;
        
        // Primero cargar correos guardados localmente
        List<EmailMessage> storedEmails = EmailStorageService.getInstance()
            .loadEmails(emailAddress, folderName);
        currentEmails.clear();
        currentEmails.addAll(storedEmails);
        updateEmailTable();
        
        // Actualizar barra de estado
        updateStatusLabel("Descargando mensajes de " + folderName + "...");
        
        // Descargar correos en segundo plano
        SwingWorker<List<EmailMessage>, Void> worker = new SwingWorker<List<EmailMessage>, Void>() {
            @Override
            protected List<EmailMessage> doInBackground() throws Exception {
                try {
                    return EmailReceiveService.fetchEmails(finalAccount, finalFolderName, 50);
                } catch (jakarta.mail.FolderNotFoundException e) {
                    // Intentar con nombres alternativos comunes
                    String[] alternativeNames = getAlternativeFolderNames(finalFolderName);
                    for (String altName : alternativeNames) {
                        try {
                            System.out.println("Intentando carpeta alternativa: " + altName);
                            return EmailReceiveService.fetchEmails(finalAccount, altName, 50);
                        } catch (jakarta.mail.FolderNotFoundException ex) {
                            // Continuar con el siguiente nombre alternativo
                        }
                    }
                    // Si ninguna alternativa funciona, lanzar la excepción original
                    throw e;
                }
            }
            
            @Override
            protected void done() {
                try {
                    List<EmailMessage> newEmails = get();

                    // Evitar reaparición de correos movidos a Papelera: excluir los que ya están en Trash
                    List<EmailMessage> trashEmails = EmailStorageService.getInstance().loadEmails(emailAddress, "Trash");
                    if (trashEmails != null && !trashEmails.isEmpty()) {
                        List<EmailMessage> filtered = new ArrayList<>();
                        for (EmailMessage ne : newEmails) {
                            if (!existsInList(trashEmails, ne)) {
                                filtered.add(ne);
                            }
                        }
                        newEmails = filtered;
                    }
                    
                    // IMPORTANT: recargar correos locales actualizados para evitar sobrescribir eliminaciones hechas mientras se descargaba
                    List<EmailMessage> freshLocal = EmailStorageService.getInstance().loadEmails(emailAddress, finalFolderName);
                    if (freshLocal == null) {
                        freshLocal = new ArrayList<>();
                    }
                    // Combinar con correos guardados (freshLocal ya refleja eliminaciones/movimientos realizados tras iniciar la descarga)
                    List<EmailMessage> mergedEmails = EmailStorageService.getInstance()
                        .mergeEmails(freshLocal, newEmails);
                    
                    currentEmails.clear();
                    currentEmails.addAll(mergedEmails);
                    updateEmailTable();
                    
                    // Guardar correos actualizados
                    EmailStorageService.getInstance().saveEmails(emailAddress, finalFolderName, mergedEmails);
                    
                    // Actualizar barra de estado
                    int newCount = newEmails.size();
                    int totalCount = mergedEmails.size();
                    updateStatusLabel("Descargados " + newCount + " nuevos mensajes de " + finalFolderName + ". Total: " + totalCount);
                        
                } catch (Exception ex) {
                    // Verificar si es un error de carpeta no encontrada
                    Throwable cause = ex.getCause();
                    if (cause instanceof jakarta.mail.FolderNotFoundException) {
                        updateStatusLabel("Carpeta '" + finalFolderName + "' no encontrada en servidor. Mostrando Papelera local.");
                        // No tocar currentEmails (ya muestra lo local); evitar bloquear con diálogo intrusivo
                        // Opcional: mostrar aviso suave
                        JOptionPane.showMessageDialog(MainWindow.this,
                            "La carpeta '" + finalFolderName + "' no existe en el servidor o tiene otro nombre.\n" +
                            "Se mostrará el contenido local de '" + finalFolderName + "'.",
                            "Carpeta no disponible",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        updateStatusLabel("Error al descargar correos");
                        JOptionPane.showMessageDialog(MainWindow.this,
                            "Error al descargar correos: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Refresca el árbol de carpetas (llamar después de añadir/eliminar cuentas)
     */
    public void refreshFoldersTree() {
        updateFoldersTree();
    }
    
    private JPanel createEmailListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Mensajes"));
        
        // Panel de filtros de etiquetas en la parte superior
        tagFilterPanel = createTagFilterPanel();
        panel.add(tagFilterPanel, BorderLayout.NORTH);
        
        // Crear modelo de tabla
        String[] columnNames = {"Estado", "De", "Asunto", "Fecha", "Etiquetas"};
        emailTableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // No editable
            }
        };
        
        // Crear tabla
        emailTable = new JTable(emailTableModel);
        // Renderer para resaltar filas nuevas y mejorar legibilidad sin forzar fondo blanco
        emailTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    // Respetar selección del sistema
                    if (!isSelected) {
                        // Restablecer colores a los del Look&Feel
                        java.awt.Color defaultBg = UIManager.getColor("Table.background");
                        java.awt.Color defaultFg = UIManager.getColor("Table.foreground");
                        if (defaultBg != null) comp.setBackground(defaultBg);
                        if (defaultFg != null) comp.setForeground(defaultFg);
                    }

                    // Fuente por defecto
                    comp.setFont(table.getFont());

                    if (row >= 0 && row < currentEmails.size()) {
                        EmailMessage em = currentEmails.get(row);

                        // Negrita en la columna Asunto si NO leído
                        if (!em.isRead() && column == 2) { // columna 2 = Asunto
                            comp.setFont(table.getFont().deriveFont(java.awt.Font.BOLD));
                        }

                        // Resaltar nuevos (sólo si no está seleccionado)
                        if (!isSelected) {
                            String key = com.gestorcorreo.service.NewEmailHighlightService.keyOf(em);
                            if (key != null && highlightNewKeys.contains(key)) {
                                comp.setBackground(new java.awt.Color(255, 249, 196)); // amarillo suave
                            }
                        }
                    }
                } catch (Exception ignore) {}
                return comp;
            }
        });
        emailTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        emailTable.setRowHeight(25);
        emailTable.getColumnModel().getColumn(0).setMaxWidth(60); // Columna de estado
        emailTable.getColumnModel().getColumn(4).setPreferredWidth(120); // Columna de etiquetas
        
        // Agregar menú contextual a la tabla de correos
        emailTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showEmailTableContextMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showEmailTableContextMenu(e);
                }
            }
            
            private void showEmailTableContextMenu(java.awt.event.MouseEvent e) {
                int row = emailTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < emailTable.getRowCount()) {
                    emailTable.setRowSelectionInterval(row, row);
                    JPopupMenu popup = createEmailContextMenu(row);
                    popup.show(emailTable, e.getX(), e.getY());
                }
            }
        });
        
        // Listener para mostrar el correo seleccionado
        emailTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = emailTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < currentEmails.size()) {
                    EmailMessage email = currentEmails.get(selectedRow);
                    
                    // Marcar como leído si no lo está
                    if (!email.isRead()) {
                        email.setRead(true);
                        // Actualizar el icono en la tabla
                        emailTableModel.setValueAt("✓", selectedRow, 0);
                        
                        // Guardar los cambios en el almacenamiento
                        String selectedAccount = (String) accountCombo.getSelectedItem();
                        if (selectedAccount != null && !selectedAccount.equals("Sin cuentas configuradas")) {
                            String emailAddress = selectedAccount.replace(" (predeterminada)", "");
                            EmailStorageService.getInstance().saveEmails(emailAddress, "INBOX", currentEmails);
                        }
                    }
                    
                    displayEmailContent(email);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(emailTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createEmailViewerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Vista previa"));
        
        // Panel superior con cabeceras y advertencia de contenido
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Panel de cabeceras del correo
        emailHeaderPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        emailHeaderPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        emailHeaderPanel.add(new JLabel("Selecciona un mensaje para ver su contenido"));
        topPanel.add(emailHeaderPanel, BorderLayout.NORTH);
        
        // Panel de advertencia de contenido bloqueado (inicialmente oculto)
        contentWarningPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        contentWarningPanel.setBackground(new Color(255, 250, 205)); // Amarillo claro
        contentWarningPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(200, 180, 100)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        JLabel warningLabel = new JLabel("⚠ Imágenes y contenido externo bloqueados por seguridad");
        warningLabel.setFont(warningLabel.getFont().deriveFont(Font.BOLD));
        
        JButton loadContentButton = new JButton("Cargar contenido externo");
        loadContentButton.addActionListener(e -> loadExternalContent());
        
        contentWarningPanel.add(warningLabel);
        contentWarningPanel.add(loadContentButton);
        contentWarningPanel.setVisible(false);
        
        topPanel.add(contentWarningPanel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);
        
        // Usar JavaFX WebView para renderizado HTML completo con CSS
        htmlViewerPanel = new HtmlViewerPanel();
        panel.add(htmlViewerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Carga el contenido externo del correo actual
     */
    private void loadExternalContent() {
        allowExternalContent = true;
        htmlViewerPanel.setAllowExternalContent(true);
        contentWarningPanel.setVisible(false);
        
        if (currentDisplayedEmail != null) {
            // Recordar que este email tiene el contenido externo cargado
            String emailId = currentDisplayedEmail.getMessageId();
            if (emailId != null && !emailId.isEmpty()) {
                emailsWithExternalContentLoaded.add(emailId);
            }
            displayEmailContent(currentDisplayedEmail);
        }
    }
    
    /**
     * Descarga los correos de la cuenta seleccionada
     */
    private void downloadEmails() {
        String selectedAccount = (String) accountCombo.getSelectedItem();
        
        if (selectedAccount == null || selectedAccount.equals("Sin cuentas configuradas")) {
            JOptionPane.showMessageDialog(this,
                "No hay cuentas configuradas.\nAñade una cuenta primero desde Archivo → Nueva cuenta",
                "Sin cuentas",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Obtener la cuenta seleccionada
        String emailAddress = selectedAccount.replace(" (predeterminada)", "");
        EmailConfig account = null;
        for (EmailConfig config : ConfigService.getInstance().getAllAccounts()) {
            if (config.getEmail().equals(emailAddress)) {
                account = config;
                break;
            }
        }
        
        if (account == null) {
            return;
        }
        
        final EmailConfig finalAccount = account;
        
        // Actualizar barra de estado
        updateStatusLabel("Descargando mensajes...");
        
        // Descargar correos en segundo plano
        SwingWorker<List<EmailMessage>, Void> worker = new SwingWorker<List<EmailMessage>, Void>() {
            @Override
            protected List<EmailMessage> doInBackground() throws Exception {
                return EmailReceiveService.fetchEmails(finalAccount, "INBOX", 50);
            }
            
            @Override
            protected void done() {
                try {
                    List<EmailMessage> newEmails = get();
                    
                    // Cargar correos guardados existentes
                    List<EmailMessage> storedEmails = EmailStorageService.getInstance()
                            .loadEmails(finalAccount.getEmail(), "INBOX");
                    
                    // Fusionar correos (evitar duplicados)
                    List<EmailMessage> mergedEmails = EmailStorageService.getInstance()
                            .mergeEmails(storedEmails, newEmails);
                    
                    // Actualizar la lista actual
                    currentEmails.clear();
                    currentEmails.addAll(mergedEmails);
                    updateEmailTable();
                    
                    // Guardar todos los correos (incluyendo los nuevos)
                    EmailStorageService.getInstance().saveEmails(
                            finalAccount.getEmail(), "INBOX", mergedEmails);
                    
                    int newCount = newEmails.size();
                    int totalCount = mergedEmails.size();
                    updateStatusLabel("Descargados " + newCount + " nuevos mensajes. Total: " + totalCount);
                    
                } catch (Exception e) {
                    updateStatusLabel("Error al descargar correos");
                    JOptionPane.showMessageDialog(MainWindow.this,
                        "Error al descargar correos:\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Carga los correos guardados localmente para la cuenta seleccionada
     */
    private void loadStoredEmailsForSelectedAccount() {
        String selectedAccount = (String) accountCombo.getSelectedItem();
        if (selectedAccount == null || selectedAccount.equals("Sin cuentas configuradas")) {
            return;
        }
        
        // Obtener la dirección de email sin el texto "(predeterminada)"
        String emailAddress = selectedAccount.replace(" (predeterminada)", "");
        
        // Establecer cuenta y carpeta actual
        this.currentAccountEmail = emailAddress;
        this.currentFolderName = "INBOX";
        
        // Cargar correos guardados
        List<EmailMessage> storedEmails = EmailStorageService.getInstance()
                .loadEmails(emailAddress, "INBOX");
        
        if (!storedEmails.isEmpty()) {
            currentEmails.clear();
            currentEmails.addAll(storedEmails);
            updateEmailTable();
            updateStatusLabel("Cargados " + storedEmails.size() + " correos de " + emailAddress);
        } else {
            currentEmails.clear();
            updateEmailTable();
            updateStatusLabel("No hay correos guardados para " + emailAddress);
        }
    }
    
    /**
     * Actualiza la tabla con los correos actuales
     */
    private void updateEmailTable() {
        emailTableModel.setRowCount(0); // Limpiar tabla
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        for (EmailMessage email : currentEmails) {
            // Aplicar filtro de etiquetas si está activo
            if (currentTagFilter != null && !email.hasTag(currentTagFilter)) {
                continue; // Saltar este correo si no tiene la etiqueta filtrada
            }
            
            Object[] row = new Object[5];
            row[0] = email.isRead() ? "✓" : "●"; // Leído / No leído
            row[1] = email.getFrom();
            row[2] = email.getSubject();
            row[3] = email.getReceivedDate() != null ? email.getReceivedDate().format(formatter) : "";
            
            // Mostrar etiquetas
            if (email.getTags() != null && !email.getTags().isEmpty()) {
                row[4] = String.join(", ", email.getTags());
            } else {
                row[4] = "";
            }
            
            emailTableModel.addRow(row);
        }
    }

    /**
     * Comprueba si un correo ya existe en una lista (misma lógica que EmailStorageService)
     */
    private boolean existsInList(List<EmailMessage> list, EmailMessage target) {
        if (list == null || target == null) return false;
        for (EmailMessage email : list) {
            if (email.getFrom() != null && email.getFrom().equals(target.getFrom()) &&
                email.getSubject() != null && email.getSubject().equals(target.getSubject()) &&
                email.getSentDate() != null && email.getSentDate().equals(target.getSentDate())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Muestra el contenido de un correo en el visor
     */
    private void displayEmailContent(EmailMessage email) {
        // Verificar si este email ya tiene el contenido externo cargado previamente
        String emailId = email.getMessageId();
        boolean hadExternalContentLoaded = emailId != null && emailsWithExternalContentLoaded.contains(emailId);
        
        // Resetear el flag de contenido externo al cambiar de correo
        // EXCEPTO si ya se había cargado el contenido externo de este email
        if (currentDisplayedEmail != email) {
            if (hadExternalContentLoaded) {
                // Este email ya tenía el contenido externo cargado, mantener el estado
                allowExternalContent = true;
                htmlViewerPanel.setAllowExternalContent(true);
            } else {
                // Email nuevo o sin contenido externo cargado previamente
                allowExternalContent = false;
                htmlViewerPanel.setAllowExternalContent(false);
            }
        }
        
        // Guardar el correo actual para recarga si se permite contenido externo
        currentDisplayedEmail = email;
        
        // Actualizar panel de cabeceras
        emailHeaderPanel.removeAll();
        emailHeaderPanel.setLayout(new GridLayout(5, 1, 5, 5));
        
        emailHeaderPanel.add(new JLabel("De: " + email.getFrom()));
        emailHeaderPanel.add(new JLabel("Para: " + String.join(", ", email.getTo())));
        emailHeaderPanel.add(new JLabel("Asunto: " + email.getSubject()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String dateStr = email.getReceivedDate() != null ? email.getReceivedDate().format(formatter) : "Desconocida";
        emailHeaderPanel.add(new JLabel("Fecha: " + dateStr));
        
        String attachInfo = email.getAttachments().isEmpty() ? "Sin adjuntos" : email.getAttachments().size() + " adjunto(s)";
        emailHeaderPanel.add(new JLabel("Adjuntos: " + attachInfo));
        
        emailHeaderPanel.revalidate();
        emailHeaderPanel.repaint();
        
        // Actualizar contenido
        String content = email.getBody() != null ? email.getBody() : "(Sin contenido)";
        
        if (email.isHtml()) {
            // Detectar si hay contenido externo (imágenes, CSS externos, etc.)
            boolean hasExternalContent = HtmlViewerPanel.hasExternalContent(content);
            
            // Mostrar advertencia solo si hay contenido externo y no se ha permitido cargarlo
            if (hasExternalContent && !allowExternalContent) {
                contentWarningPanel.setVisible(true);
            } else {
                contentWarningPanel.setVisible(false);
            }
            
            // Asegurar que el HTML tenga estructura básica si no la tiene
            String htmlContent = content.trim();
            if (!htmlContent.toLowerCase().startsWith("<html")) {
                htmlContent = "<html><head><meta charset=\"UTF-8\"></head><body>" + htmlContent + "</body></html>";
            }
            
            // Cargar contenido HTML en el WebView
            htmlViewerPanel.loadHtmlContent(htmlContent);
            
        } else {
            // Contenido de texto plano, no hay contenido externo
            contentWarningPanel.setVisible(false);
            
            // Convertir texto plano a HTML para visualización
            String htmlContent = "<html><head><meta charset=\"UTF-8\"></head>" +
                                "<body style='font-family: Arial, sans-serif; padding: 20px;'>" +
                                "<pre style='white-space: pre-wrap; word-wrap: break-word; font-family: inherit;'>" +
                                escapeHtml(content) +
                                "</pre></body></html>";
            
            htmlViewerPanel.loadHtmlContent(htmlContent);
        }
    }
    

    /**
     * Escapa caracteres HTML especiales
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
    
    private JPanel createStatusPanel() {
        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        
        statusLabel = new JLabel(" Listo");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        return statusPanel;
    }
    
    /**
     * Inicializa el icono en la bandeja del sistema
     */
    private void initSystemTray() {
        trayManager = new SystemTrayManager(this);
        trayManager.initialize();
    }

    /**
     * Inicializa recordatorios de citas (día siguiente)
     */
    private void initAppointmentReminderService() {
        try {
            com.gestorcorreo.service.AppointmentReminderService.getInstance().start(trayManager);
        } catch (Exception ignored) {}
    }
    
    /**
     * Inicializa el servicio de comprobación automática de correos
     */
    private void initEmailCheckService() {
        // Configurar el listener para notificaciones
        EmailCheckService.getInstance().setEmailCheckListener(new EmailCheckService.EmailCheckListener() {
            @Override
            public void onNewEmailsDetected(String accountEmail, int count, String message) {
                // Mostrar notificación nativa del sistema operativo
                com.gestorcorreo.util.NotificationHelper.showNotification(
                    "Correos Y Multiplicaos", 
                    message, 
                    com.gestorcorreo.util.NotificationHelper.NotificationType.INFO
                );
                
                // También mostrar en la bandeja del sistema (fallback)
                if (trayManager != null) {
                    trayManager.showNotification("Correos Y Multiplicaos", message, TrayIcon.MessageType.INFO);
                }
                
                // Actualizar la barra de estado
                updateStatusLabel(message);

                // Si estamos viendo la misma cuenta y la carpeta actual es INBOX, refrescar lista desde almacenamiento
                String selectedAccount = (String) accountCombo.getSelectedItem();
                if (selectedAccount != null) {
                    String selectedEmail = selectedAccount.replace(" (predeterminada)", "");
                    if (selectedEmail.equals(accountEmail) && "INBOX".equalsIgnoreCase(currentFolderName)) {
                        // Preparar resaltado con nuevas claves
                        highlightNewKeys.clear();
                        highlightNewKeys.addAll(com.gestorcorreo.service.NewEmailHighlightService.getInstance().getNewKeys(accountEmail));
                        List<EmailMessage> stored = com.gestorcorreo.service.EmailStorageService.getInstance().loadEmails(accountEmail, "INBOX");
                        currentEmails.clear();
                        currentEmails.addAll(stored);
                        updateEmailTable();
                        emailTable.repaint();
                        // Limpiar resaltado y badge tras unos segundos
                        new javax.swing.Timer(6000, ev -> {
                            highlightNewKeys.clear();
                            com.gestorcorreo.service.NewEmailHighlightService.getInstance().clearNewKeys(accountEmail);
                            com.gestorcorreo.service.NewEmailHighlightService.getInstance().resetBadge(accountEmail);
                            foldersTree.repaint();
                            emailTable.repaint();
                        }) {{ setRepeats(false); }}.start();
                    }
                }
                // Refrescar badge del árbol siempre
                foldersTree.repaint();
            }
        });
        
        // Iniciar el servicio de comprobación automática
        EmailCheckService.getInstance().startAutoCheck();
    }
    
    /**
     * Cierra la aplicación con confirmación
     */
    private void exitApplication() {
        int response = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro de que desea salir?",
            "Confirmar salida",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (response == JOptionPane.YES_OPTION) {
            // Detener auto-lock
            com.gestorcorreo.security.AutoLockService.getInstance().stop();
            
            // Limpiar claves de encriptación de memoria antes de salir
            try {
                com.gestorcorreo.security.EncryptionService.getInstance().clearKeys();
                com.gestorcorreo.security.SecureLogger.info("Claves de encriptación limpiadas de memoria");
            } catch (Exception e) {
                com.gestorcorreo.security.SecureLogger.error("Error al limpiar claves", e);
            }
            
            System.exit(0);
        }
    }
    
    /**
     * Muestra el diálogo de búsqueda avanzada
     */
    private void showAdvancedSearch() {
        AdvancedSearchDialog searchDialog = new AdvancedSearchDialog(this);
        searchDialog.setVisible(true);
    }
    
    /**
     * Guarda los correos actuales en el almacenamiento
     */
    private void saveCurrentEmails() {
        if (currentAccountEmail != null && currentFolderName != null) {
            EmailStorageService.getInstance().saveEmails(
                currentAccountEmail, currentFolderName, currentEmails);
        }
    }
    
    /**
     * Crea el panel de filtrado rápido por etiquetas
     */
    private JPanel createTagFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel filterLabel = new JLabel("Filtrar por etiqueta:");
        panel.add(filterLabel);
        
        // Botón "Todas" para quitar el filtro
        JButton allButton = new JButton("Todas");
        allButton.setToolTipText("Mostrar todos los correos");
        allButton.setOpaque(true);
        allButton.setBackground(new Color(60, 63, 65));
        allButton.setForeground(Color.WHITE);
        Color allBorderColor = new Color(66, 133, 244);
        allButton.setBorder(new LineBorder(allBorderColor, 1, true));
        allButton.putClientProperty("borderColor", allBorderColor);
        allButton.setFocusPainted(false);
        allButton.addActionListener(e -> {
            currentTagFilter = null;
            updateEmailTable();
            highlightSelectedFilterButton(allButton);
        });
        panel.add(allButton);
        
        // Crear botones para cada etiqueta
        for (Tag tag : TagService.getInstance().getAllTags()) {
            JButton tagButton = new JButton(tag.getName());
            tagButton.setToolTipText("Filtrar por: " + tag.getName());
            
            // Aplicar color de la etiqueta
            try {
                Color tagColor = Color.decode(tag.getColor());
                tagButton.setBackground(tagColor);
                tagButton.setForeground(Color.WHITE);
                tagButton.setBorder(new LineBorder(tagColor, 1, true));
                tagButton.putClientProperty("borderColor", tagColor);
            } catch (NumberFormatException ex) {
                tagButton.setBackground(Color.LIGHT_GRAY);
                tagButton.setForeground(Color.WHITE);
                tagButton.setBorder(new LineBorder(Color.LIGHT_GRAY, 1, true));
                tagButton.putClientProperty("borderColor", Color.LIGHT_GRAY);
            }
            
            tagButton.setOpaque(true);
            tagButton.setBorderPainted(true);
            tagButton.setFocusPainted(false);
            
            tagButton.addActionListener(e -> {
                currentTagFilter = tag.getName();
                updateEmailTable();
                highlightSelectedFilterButton(tagButton);
            });
            
            panel.add(tagButton);
        }

        this.tagFilterPanel = panel;
        highlightSelectedFilterButton(allButton);
        return panel;
    }
    
    /**
     * Resalta el botón de filtro seleccionado
     */
    private void highlightSelectedFilterButton(JButton selectedButton) {
        if (tagFilterPanel == null) {
            return; // Protección contra null
        }
        
        // Restaurar todos los botones del panel
        for (Component comp : tagFilterPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                Color borderColor = (Color) btn.getClientProperty("borderColor");
                if (borderColor == null) {
                    borderColor = Color.GRAY;
                }
                btn.setBorder(new LineBorder(borderColor, 1, true));
            }
        }
		
        // Resaltar el botón seleccionado
        if (selectedButton != null) {
            Color borderColor = (Color) selectedButton.getClientProperty("borderColor");
            if (borderColor == null) {
                borderColor = Color.BLUE;
            }
            selectedButton.setBorder(new LineBorder(borderColor, 3, true));
        }
    }
    
    /**
     * Muestra la ventana principal
     */
    public void showWindow() {
        setVisible(true);
    }
    
    /**
     * Obtiene el gestor de la bandeja del sistema
     */
    public SystemTrayManager getTrayManager() {
        return trayManager;
    }
}
