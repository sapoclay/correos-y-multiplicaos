package com.gestorcorreo.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Gestor del icono en la bandeja del sistema
 */
public class SystemTrayManager {
    
    private TrayIcon trayIcon;
    private SystemTray systemTray;
    private MainWindow mainWindow;
    private JDialog popupDialog;
    private JPopupMenu popupMenu;
    
    public SystemTrayManager(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }
    
    /**
     * Inicializa el icono en la bandeja del sistema
     */
    public void initialize() {
        // Verificar si el sistema soporta la bandeja
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray no está soportado en este sistema");
            return;
        }
        
        systemTray = SystemTray.getSystemTray();
        
        // Cargar el logo
        Image image = loadTrayIcon();
        
        // Crear el menú contextual Swing
        createSwingPopupMenu();
        
        // Crear el icono de la bandeja sin menú AWT (usaremos JPopupMenu)
        trayIcon = new TrayIcon(image, "Correos Y Multiplicaos");
        trayIcon.setImageAutoSize(true);
        
        // Listener para mostrar el menú con clic derecho y alternar ventana con doble clic
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { // Clic derecho
                    showSwingPopupMenu(e);
                }
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) { // Doble clic izquierdo
                    toggleMainWindow();
                }
            }
        });
        
        // Agregar el icono a la bandeja
        try {
            systemTray.add(trayIcon);
            System.out.println("Icono agregado a la bandeja del sistema");
        } catch (AWTException e) {
            System.err.println("Error al agregar el icono a la bandeja del sistema: " + e.getMessage());
        }
    }
    
    /**
     * Crea el menú contextual usando Swing (respeta el Look and Feel)
     */
    private void createSwingPopupMenu() {
        popupMenu = new JPopupMenu();
        
        // Opción: Desbloquear (solo visible cuando está bloqueado)
        JMenuItem unlockItem = new JMenuItem("🔓 Desbloquear aplicación");
        unlockItem.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
        unlockItem.addActionListener(e -> {
            com.gestorcorreo.security.AutoLockService.getInstance().unlockApplication();
        });
        popupMenu.add(unlockItem);
        
        popupMenu.addSeparator();
        
        // Opción: Mostrar/Ocultar ventana principal
        JMenuItem showHideItem = new JMenuItem("Mostrar ventana");
        showHideItem.addActionListener(e -> toggleMainWindow());
        popupMenu.add(showHideItem);
        
        popupMenu.addSeparator();
        
        // Opción: Nuevo mensaje
        JMenuItem newMessageItem = new JMenuItem("Nuevo mensaje");
        newMessageItem.addActionListener(e -> {
            if (!mainWindow.isVisible()) {
                mainWindow.setVisible(true);
                mainWindow.setState(Frame.NORMAL);
            }
            SwingUtilities.invokeLater(() -> {
                com.gestorcorreo.model.EmailConfig defaultAccount = 
                    com.gestorcorreo.service.ConfigService.getInstance().getDefaultAccount();
                if (defaultAccount == null) {
                    JOptionPane.showMessageDialog(mainWindow,
                        "No hay ninguna cuenta configurada.\nPor favor, añada una cuenta en Archivo > Nueva cuenta",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Abrir el nuevo compositor JavaFX
                JavaFXComposeWindow.open(defaultAccount);
            });
        });
        popupMenu.add(newMessageItem);

        // Opción: Calendario
        JMenuItem calendarItem = new JMenuItem("Calendario");
        calendarItem.addActionListener(e -> {
            if (!mainWindow.isVisible()) {
                mainWindow.setVisible(true);
                mainWindow.setState(Frame.NORMAL);
            }
            SwingUtilities.invokeLater(() -> {
                com.gestorcorreo.model.AppointmentService srv = new com.gestorcorreo.model.AppointmentService();
                com.gestorcorreo.model.CalendarDialog dlg = new com.gestorcorreo.model.CalendarDialog(mainWindow, srv);
                dlg.setVisible(true);
            });
        });
        popupMenu.add(calendarItem);
        
        // Opción: Nueva cuenta
        JMenuItem newAccountItem = new JMenuItem("Nueva cuenta");
        newAccountItem.addActionListener(e -> {
            if (!mainWindow.isVisible()) {
                mainWindow.setVisible(true);
                mainWindow.setState(Frame.NORMAL);
            }
            SwingUtilities.invokeLater(() -> NewAccountDialog.showDialog(mainWindow));
        });
        popupMenu.add(newAccountItem);
        
        // Opción: Administrar cuentas
        JMenuItem manageAccountsItem = new JMenuItem("Administrar cuentas");
        manageAccountsItem.addActionListener(e -> {
            if (!mainWindow.isVisible()) {
                mainWindow.setVisible(true);
                mainWindow.setState(Frame.NORMAL);
            }
            SwingUtilities.invokeLater(() -> AccountManagerDialog.showDialog(mainWindow));
        });
        popupMenu.add(manageAccountsItem);
        
        // Opción: Configuración
        JMenuItem configItem = new JMenuItem("Configuración");
        configItem.addActionListener(e -> {
            if (!mainWindow.isVisible()) {
                mainWindow.setVisible(true);
                mainWindow.setState(Frame.NORMAL);
            }
            SwingUtilities.invokeLater(() -> ConfigDialog.showDialog(mainWindow));
        });
        popupMenu.add(configItem);
        
        popupMenu.addSeparator();
        
        // Opción: Acerca de
        JMenuItem aboutItem = new JMenuItem("Acerca de");
        aboutItem.addActionListener(e -> 
            SwingUtilities.invokeLater(() -> AboutDialog.showDialog(mainWindow))
        );
        popupMenu.add(aboutItem);
        
        popupMenu.addSeparator();
        
        // Opción: Salir
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> exitApplication());
        popupMenu.add(exitItem);
        
        // Crear un diálogo invisible para el menú popup
        popupDialog = new JDialog();
        popupDialog.setUndecorated(true);
        popupDialog.setSize(0, 0);
        popupDialog.setAlwaysOnTop(true);
    }
    
    /**
     * Muestra el menú Swing en la posición del cursor
     */
    private void showSwingPopupMenu(MouseEvent e) {
        SwingUtilities.invokeLater(() -> {
            // Actualizar visibilidad de la opción Desbloquear según el estado
            boolean isLocked = com.gestorcorreo.security.AutoLockService.getInstance().isLocked();
            JMenuItem unlockItem = (JMenuItem) popupMenu.getComponent(0);
            unlockItem.setVisible(isLocked);
            
            // Si está bloqueado, ocultar/mostrar el separador
            if (popupMenu.getComponent(1) instanceof JPopupMenu.Separator) {
                popupMenu.getComponent(1).setVisible(isLocked);
            }
            
            // Actualizar el Look and Feel del menú según las preferencias actuales
            applyLookAndFeelToPopup();
            
            Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
            popupDialog.setLocation(mouseLocation);
            popupDialog.setVisible(true);
            popupMenu.show(popupDialog, 0, 0);
            
            // Ocultar el diálogo cuando se cierre el menú
            popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                    popupDialog.setVisible(false);
                }
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
                @Override
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                    popupDialog.setVisible(false);
                }
            });
        });
    }
    
    /**
     * Aplica el Look and Feel actual al menú popup
     */
    private void applyLookAndFeelToPopup() {
        try {
            SwingUtilities.updateComponentTreeUI(popupMenu);
            SwingUtilities.updateComponentTreeUI(popupDialog);
        } catch (Exception e) {
            System.err.println("Error al actualizar el Look and Feel del menú: " + e.getMessage());
        }
    }
    
    /**
     * Carga el icono para la bandeja del sistema
     */
    private Image loadTrayIcon() {
        try {
            // Cargar el logo desde los recursos
            java.net.URL imageURL = getClass().getResource("/images/logo.png");
            if (imageURL != null) {
                ImageIcon icon = new ImageIcon(imageURL);
                // Redimensionar para la bandeja (16x16 o 32x32 dependiendo del sistema)
                int size = (int) systemTray.getTrayIconSize().getWidth();
                Image scaledImage = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return scaledImage;
            } else {
                System.out.println("No se pudo cargar el logo, usando icono por defecto");
            }
        } catch (Exception e) {
            System.err.println("Error al cargar el icono: " + e.getMessage());
        }
        
        // Crear un icono por defecto si no se puede cargar el logo
        return createDefaultIcon();
    }
    
    /**
     * Crea un icono por defecto si no se puede cargar el logo
     */
    private Image createDefaultIcon() {
        int size = 32;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Dibujar un sobre simple
        g2d.setColor(new Color(70, 130, 180));
        g2d.fillRect(2, 8, 28, 20);
        g2d.setColor(Color.WHITE);
        g2d.drawLine(4, 10, 16, 18);
        g2d.drawLine(16, 18, 28, 10);
        
        g2d.dispose();
        return image;
    }
    

    
    /**
     * Muestra u oculta la ventana principal
     */
    private void toggleMainWindow() {
        if (mainWindow.isVisible()) {
            mainWindow.setVisible(false);
        } else {
            mainWindow.setVisible(true);
            mainWindow.setState(Frame.NORMAL);
            mainWindow.toFront();
        }
    }
    
    /**
     * Cierra la aplicación completamente
     */
    private void exitApplication() {
        int response = JOptionPane.showConfirmDialog(
            mainWindow,
            "¿Está seguro de que desea salir?",
            "Confirmar salida",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (response == JOptionPane.YES_OPTION) {
            // Remover el icono de la bandeja
            if (systemTray != null && trayIcon != null) {
                systemTray.remove(trayIcon);
            }
            // Cerrar la aplicación
            System.exit(0);
        }
    }
    
    /**
     * Muestra una notificación en la bandeja del sistema
     */
    public void showNotification(String caption, String text, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            trayIcon.displayMessage(caption, text, messageType);
        }
    }
    
    /**
     * Actualiza el Look and Feel del menú contextual
     * Llamar este método después de cambiar el tema en las preferencias
     */
    public void updateMenuLookAndFeel() {
        if (popupMenu != null && popupDialog != null) {
            SwingUtilities.invokeLater(() -> {
                applyLookAndFeelToPopup();
            });
        }
    }
}
