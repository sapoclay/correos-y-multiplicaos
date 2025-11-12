package com.gestorcorreo.ui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Pantalla de inicio (Splash Screen) para la aplicación Correos Y Multiplicaos
 */
public class SplashScreen extends JWindow {
    private static final int DISPLAY_TIME = 4000; // 4 segundos
    
    public SplashScreen() {
        initComponents();
    }
    
    private void initComponents() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        
        // Cargar el logo desde resources
        URL logoUrl = getClass().getClassLoader().getResource("images/logo.png");
        
        if (logoUrl != null) {
            ImageIcon originalLogo = new ImageIcon(logoUrl);
            // Redimensionar la imagen para que sea más alta que ancha (250x350 px)
            Image scaledImage = originalLogo.getImage().getScaledInstance(250, 350, Image.SCALE_SMOOTH);
            ImageIcon logo = new ImageIcon(scaledImage);
            JLabel imageLabel = new JLabel(logo);
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            imageLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
            content.add(imageLabel, BorderLayout.CENTER);
        } else {
            // Mensaje alternativo si no se encuentra el logo
            JLabel textLabel = new JLabel("Correos Y Multiplicaos", JLabel.CENTER);
            textLabel.setFont(new Font("Arial", Font.BOLD, 32));
            textLabel.setForeground(new Color(0, 102, 204));
            content.add(textLabel, BorderLayout.CENTER);
        }
        
        // Panel inferior con texto de carga y créditos
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        bottomPanel.setBackground(Color.WHITE);
        
        JLabel loadingLabel = new JLabel("Cargando aplicación...", JLabel.CENTER);
        loadingLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        loadingLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        JLabel creditsLabel = new JLabel("Creado por entreunosyceros.net", JLabel.CENTER);
        creditsLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        creditsLabel.setForeground(new Color(102, 102, 102));
        creditsLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        bottomPanel.add(loadingLabel);
        bottomPanel.add(creditsLabel);
        content.add(bottomPanel, BorderLayout.SOUTH);
        
        // Añadir borde
        content.setBorder(BorderFactory.createLineBorder(new Color(0, 102, 204), 2));
        
        setContentPane(content);
        pack();
        
        // Obtener la pantalla principal del sistema
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = defaultScreen.getDefaultConfiguration();
        Rectangle screenBounds = gc.getBounds();
        
        // Dimensiones de la ventana (más alta que ancha)
        int splashWidth = 300;
        int splashHeight = 480;
        setSize(new Dimension(splashWidth, splashHeight));
        
        // Centrar en la pantalla principal
        int x = screenBounds.x + (screenBounds.width - splashWidth) / 2;
        int y = screenBounds.y + (screenBounds.height - splashHeight) / 2;
        setLocation(x, y);
    }
    
    /**
     * Muestra el splash screen durante el tiempo especificado
     */
    public void showSplash() {
        setVisible(true);
        
        try {
            Thread.sleep(DISPLAY_TIME);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        setVisible(false);
        dispose();
    }
    
    /**
     * Muestra el splash screen de forma estática
     */
    public static void showSplashScreen() {
        SplashScreen splash = new SplashScreen();
        splash.showSplash();
    }
}
