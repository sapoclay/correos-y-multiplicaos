package com.gestorcorreo;

import com.gestorcorreo.ui.MainWindow;
import com.gestorcorreo.ui.SplashScreen;

import javax.swing.*;

/**
 * Clase principal de la aplicación Correos Y Multiplicaos
 */
public class Main {
    public static void main(String[] args) {
        // Configurar el look and feel del sistema
        try {
            // Intentar cargar FlatLaf por reflexión para no fallar si no está en el classpath
            Class<?> laf = Class.forName("com.formdev.flatlaf.FlatDarculaLaf");
            java.lang.reflect.Method setup = laf.getMethod("setup");
            setup.invoke(null);
            UIManager.put("Component.arc", 8);
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        // Mostrar splash screen
        SplashScreen.showSplashScreen();
        
    // (Servidor puente eliminado - editor JavaFX nativo)

        // Inicializar y mostrar la ventana principal
        SwingUtilities.invokeLater(() -> {
            MainWindow mainWindow = new MainWindow();
            mainWindow.showWindow();
        });
        
        System.out.println("Aplicación Correos Y Multiplicaos iniciada correctamente");
    }
}
