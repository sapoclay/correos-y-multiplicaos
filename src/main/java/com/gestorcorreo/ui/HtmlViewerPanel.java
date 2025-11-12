package com.gestorcorreo.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;

/**
 * Panel que usa JavaFX WebView para renderizar HTML completo con CSS
 * Proporciona un renderizado mucho mejor que JEditorPane
 */
public class HtmlViewerPanel extends JPanel {
    private JFXPanel jfxPanel;
    private WebView webView;
    private WebEngine webEngine;
    private boolean allowExternalContent = false;
    
    public HtmlViewerPanel() {
        setLayout(new BorderLayout());
        initializeJavaFX();
    }
    
    /**
     * Inicializa el componente JavaFX WebView embebido en Swing
     */
    private void initializeJavaFX() {
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);
        
        // Inicializar JavaFX en el hilo de JavaFX
        Platform.runLater(() -> {
            webView = new WebView();
            webEngine = webView.getEngine();
            
            // Configurar el WebEngine
            webEngine.setJavaScriptEnabled(true); // Necesario para interceptar clics en enlaces
            webEngine.setUserStyleSheetLocation(null);
            
            // Interceptar navegación para abrir enlaces externos en el navegador
            webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && (newValue.startsWith("http://") || newValue.startsWith("https://"))) {
                    // Evitar bucles infinitos
                    if (oldValue != null && oldValue.equals(newValue)) {
                        return;
                    }
                    
                    // Cancelar la navegación
                    Platform.runLater(() -> {
                        webEngine.getLoadWorker().cancel();
                    });
                    
                    // Abrir en navegador externo
                    openInBrowser(newValue);
                }
            });
            
            // También interceptar con listener de carga para inyectar JavaScript adicional
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    injectLinkInterceptor();
                }
            });
            
            // Crear la escena y añadirla al JFXPanel
            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
            
            // Mensaje inicial
            loadHtmlContent("<html><body style='font-family: Arial; padding: 20px; color: #666;'>" +
                    "<h3>Selecciona un mensaje para ver su contenido</h3></body></html>");
        });
    }
    
    /**
     * Carga contenido HTML en el WebView
     */
    public void loadHtmlContent(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            htmlContent = "<html><body><p>Sin contenido</p></body></html>";
        }
        
        final String content = htmlContent;
        
        Platform.runLater(() -> {
            if (webEngine != null) {
                // Si no se permite contenido externo, bloquear recursos externos
                if (!allowExternalContent) {
                    // Procesar HTML para bloquear recursos externos
                    String sanitized = sanitizeHtmlForSecurity(content);
                    webEngine.loadContent(sanitized);
                } else {
                    webEngine.loadContent(content);
                }
            }
        });
    }
    
    /**
     * Sanitiza el HTML bloqueando recursos externos si está configurado
     */
    private String sanitizeHtmlForSecurity(String html) {
        if (allowExternalContent) {
            return html;
        }
        
        String sanitized = html;
        
        // Bloquear imágenes externas (múltiples variantes)
        // Variante 1: src="http://..." o src="https://..."
        sanitized = sanitized.replaceAll(
            "(?i)(<img[^>]*\\s+src\\s*=\\s*['\"])https?://[^'\"]+(['\"][^>]*>)",
            "$1data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='150' height='100'%3E" +
            "%3Crect fill='%23f0f0f0' stroke='%23ccc' stroke-width='2' width='150' height='100'/%3E" +
            "%3Ctext x='50%25' y='50%25' text-anchor='middle' dy='.3em' fill='%23666' font-size='11' font-family='Arial'%3E" +
            "🖼 Imagen bloqueada%3C/text%3E%3C/svg%3E$2"
        );
        
        // Variante 2: <img src=http... (sin comillas)
        sanitized = sanitized.replaceAll(
            "(?i)(<img[^>]*\\s+src\\s*=\\s*)https?://[^\\s>]+",
            "$1data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='150' height='100'%3E" +
            "%3Crect fill='%23f0f0f0' stroke='%23ccc' stroke-width='2' width='150' height='100'/%3E" +
            "%3Ctext x='50%25' y='50%25' text-anchor='middle' dy='.3em' fill='%23666' font-size='11' font-family='Arial'%3E" +
            "🖼 Imagen bloqueada%3C/text%3E%3C/svg%3E"
        );
        
        // Bloquear hojas de estilo externas
        sanitized = sanitized.replaceAll(
            "(?i)<link[^>]+href\\s*=\\s*['\"]?https?://[^'\"\\s>]+['\"]?[^>]*>",
            "<!-- Hoja de estilo externa bloqueada -->"
        );
        
        // Bloquear scripts externos
        sanitized = sanitized.replaceAll(
            "(?i)<script[^>]+src\\s*=\\s*['\"]?https?://[^'\"\\s>]+['\"]?[^>]*>.*?</script>",
            "<!-- Script externo bloqueado -->"
        );
        
        // Bloquear iframes
        sanitized = sanitized.replaceAll(
            "(?i)<iframe[^>]*>.*?</iframe>",
            "<div style='border: 2px dashed #ccc; padding: 20px; margin: 10px 0; background: #f9f9f9; color: #666; text-align: center;'>" +
            "🚫 Contenido externo bloqueado (iframe)</div>"
        );
        
        return sanitized;
    }
    
    /**
     * Inyecta JavaScript para interceptar clics en enlaces y modificar atributo target
     */
    private void injectLinkInterceptor() {
        try {
            // Prevenir que los enlaces naveguen dentro del WebView
            String script = 
                "(function() {" +
                "    console.log('Inyectando interceptor de enlaces...');" +
                "    var links = document.getElementsByTagName('a');" +
                "    for (var i = 0; i < links.length; i++) {" +
                "        var link = links[i];" +
                "        if (link.href && (link.href.startsWith('http://') || link.href.startsWith('https://'))) {" +
                "            link.onclick = function(e) {" +
                "                e.preventDefault();" +
                "                e.stopPropagation();" +
                "                console.log('Clic en enlace: ' + this.href);" +
                "                window.location.href = this.href;" +
                "                return false;" +
                "            };" +
                "        }" +
                "    }" +
                "    console.log('Interceptor inyectado en ' + links.length + ' enlaces');" +
                "})();";
            
            webEngine.executeScript(script);
        } catch (Exception e) {
            System.err.println("ERROR: No se pudo inyectar JavaScript: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Abre una URL en el navegador predeterminado del sistema
     */
    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                // Fallback para sistemas Linux sin Desktop API
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux")) {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", url});
                } else if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                }
            }
            System.out.println("Abriendo enlace en navegador: " + url);
        } catch (Exception e) {
            System.err.println("Error al abrir enlace en navegador: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Permite o bloquea la carga de contenido externo
     */
    public void setAllowExternalContent(boolean allow) {
        this.allowExternalContent = allow;
    }
    
    /**
     * Obtiene si se permite contenido externo
     */
    public boolean isAllowExternalContent() {
        return allowExternalContent;
    }
    
    /**
     * Limpia el contenido del visor
     */
    public void clear() {
        loadHtmlContent("<html><body></body></html>");
    }
    
    /**
     * Detecta si el HTML contiene recursos externos
     */
    public static boolean hasExternalContent(String html) {
        if (html == null) {
            return false;
        }
        
        String lower = html.toLowerCase();
        
        // Detectar imágenes externas (cualquier formato)
        if (lower.contains("<img") && (lower.contains("src=\"http://") || 
                                        lower.contains("src=\"https://") ||
                                        lower.contains("src='http://") ||
                                        lower.contains("src='https://") ||
                                        lower.contains("src=http://") ||
                                        lower.contains("src=https://"))) {
            return true;
        }
        
        // Detectar hojas de estilo externas
        if (lower.contains("<link") && (lower.contains("href=\"http://") || 
                                         lower.contains("href=\"https://") ||
                                         lower.contains("href='http://") ||
                                         lower.contains("href='https://"))) {
            return true;
        }
        
        // Detectar scripts externos
        if (lower.contains("<script") && (lower.contains("src=\"http://") || 
                                           lower.contains("src=\"https://") ||
                                           lower.contains("src='http://") ||
                                           lower.contains("src='https://"))) {
            return true;
        }
        
        // Detectar iframes
        if (lower.contains("<iframe")) {
            return true;
        }
        
        return false;
    }
}
