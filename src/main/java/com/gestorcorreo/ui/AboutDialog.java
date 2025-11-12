package com.gestorcorreo.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URL;

/**
 * Diálogo "Acerca de" para la aplicación Correos Y Multiplicaos
 */
public class AboutDialog extends JDialog {
    private static final String GITHUB_URL = "https://github.com/sapoclay/correos-y-multiplicaos";
    
    public AboutDialog(JFrame parent) {
        super(parent, "Acerca de Correos Y Multiplicaos", true);
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 400);
        setLocationRelativeTo(getParent());
        setResizable(false);
        
        // Panel superior con el logo
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(Color.WHITE);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        
        URL logoUrl = getClass().getClassLoader().getResource("images/logo.png");
        if (logoUrl != null) {
            ImageIcon originalLogo = new ImageIcon(logoUrl);
            Image scaledImage = originalLogo.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            ImageIcon logo = new ImageIcon(scaledImage);
            JLabel logoLabel = new JLabel(logo);
            logoPanel.add(logoLabel);
        }
        
        add(logoPanel, BorderLayout.NORTH);
        
        // Panel central con información
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        
        JLabel titleLabel = new JLabel("Correos Y Multiplicaos");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.BLACK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel versionLabel = new JLabel("Versión 1.0-SNAPSHOT");
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        versionLabel.setForeground(Color.BLACK);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextArea descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBackground(Color.WHITE);
        descriptionArea.setForeground(Color.BLACK);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        descriptionArea.setText(
            "Gestor de correo electrónico completo desarrollado en Java.\n\n" +
            "Esta aplicación permite gestionar múltiples cuentas de correo, " +
            "organizar mensajes en carpetas, enviar y recibir correos con " +
            "adjuntos, y mucho más.\n\n" +
            "Características principales:\n" +
            "• Soporte para múltiples cuentas\n" +
            "• Gestión de carpetas personalizadas\n" +
            "• Envío y recepción de correos\n" +
            "• Soporte para archivos adjuntos\n" +
            "• Interfaz intuitiva y fácil de usar"
        );
        
        JLabel creditsLabel = new JLabel("Creado por entreunosyceros.net");
        creditsLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        creditsLabel.setForeground(new Color(102, 102, 102));
        creditsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        infoPanel.add(titleLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(versionLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        infoPanel.add(descriptionArea);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        infoPanel.add(creditsLabel);
        
        add(infoPanel, BorderLayout.CENTER);
        
        // Panel inferior con botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton githubButton = new JButton("Ver en GitHub");
        githubButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openWebpage(GITHUB_URL);
            }
        });
        
        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        buttonPanel.add(githubButton);
        buttonPanel.add(closeButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Hacer el fondo blanco
        getContentPane().setBackground(Color.WHITE);
    }
    
    private void openWebpage(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime runtime = Runtime.getRuntime();
            
            if (os.contains("linux")) {
                // Intentar con xdg-open primero (estándar en Linux)
                runtime.exec(new String[]{"xdg-open", url});
            } else if (os.contains("mac")) {
                runtime.exec(new String[]{"open", url});
            } else if (os.contains("win")) {
                runtime.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else {
                // Fallback a Desktop API
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    JOptionPane.showMessageDialog(this,
                        "No se puede abrir el navegador automáticamente.\nVisita: " + url,
                        "Información",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al abrir el navegador: " + e.getMessage() + "\nVisita: " + url,
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void showDialog(JFrame parent) {
        AboutDialog dialog = new AboutDialog(parent);
        dialog.setVisible(true);
    }
}
