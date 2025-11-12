package com.gestorcorreo.service;

import org.languagetool.JLanguageTool;
import org.languagetool.language.Spanish;
import org.languagetool.rules.RuleMatch;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Servicio de corrección ortográfica y gramatical usando LanguageTool
 */
public class SpellCheckService {
    private static SpellCheckService instance;
    private JLanguageTool languageTool;
    private final Color errorColor = new Color(255, 0, 0, 100); // Rojo semi-transparente
    
    private SpellCheckService() {
        try {
            // Inicializar LanguageTool con español
            languageTool = new JLanguageTool(new Spanish());
            System.out.println("Corrector ortográfico español inicializado correctamente");
        } catch (Exception e) {
            System.err.println("Error al inicializar el corrector ortográfico: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static SpellCheckService getInstance() {
        if (instance == null) {
            instance = new SpellCheckService();
        }
        return instance;
    }
    
    /**
     * Verifica el texto y marca los errores con subrayado rojo ondulado
     */
    public void checkText(JTextPane textPane) {
        if (languageTool == null) {
            return;
        }
        
        try {
            // Obtener el texto del documento para preservar caracteres UTF-8
            Document doc = textPane.getDocument();
            String text = doc.getText(0, doc.getLength());
            
            // Limpiar marcas previas
            clearHighlights(textPane);
            
            // Obtener errores
            List<RuleMatch> matches = languageTool.check(text);
            
            // Marcar cada error
            Highlighter highlighter = textPane.getHighlighter();
            for (RuleMatch match : matches) {
                try {
                    highlighter.addHighlight(
                        match.getFromPos(),
                        match.getToPos(),
                        new UnderlineHighlightPainter(errorColor)
                    );
                    
                    // Opcional: Añadir tooltip con sugerencias
                    String tooltip = buildTooltip(match);
                    textPane.setToolTipText(tooltip);
                    
                } catch (BadLocationException e) {
                    System.err.println("Error al marcar error: " + e.getMessage());
                }
            }
            
        } catch (BadLocationException | IOException e) {
            System.err.println("Error al verificar el texto: " + e.getMessage());
        }
    }
    
    /**
     * Construye el tooltip con el mensaje de error y sugerencias
     */
    private String buildTooltip(RuleMatch match) {
        StringBuilder tooltip = new StringBuilder("<html>");
        tooltip.append("<b>Error:</b> ").append(match.getMessage()).append("<br>");
        
        if (!match.getSuggestedReplacements().isEmpty()) {
            tooltip.append("<b>Sugerencias:</b> ");
            tooltip.append(String.join(", ", match.getSuggestedReplacements()));
        }
        
        tooltip.append("</html>");
        return tooltip.toString();
    }
    
    /**
     * Limpia todas las marcas de error del texto
     */
    public void clearHighlights(JTextPane textPane) {
        Highlighter highlighter = textPane.getHighlighter();
        highlighter.removeAllHighlights();
    }
    
    /**
     * Obtiene las sugerencias para una palabra
     */
    public List<String> getSuggestions(String word) {
        if (languageTool == null) {
            return List.of();
        }
        
        try {
            List<RuleMatch> matches = languageTool.check(word);
            if (!matches.isEmpty()) {
                return matches.get(0).getSuggestedReplacements();
            }
        } catch (IOException e) {
            System.err.println("Error al obtener sugerencias: " + e.getMessage());
        }
        
        return List.of();
    }
    
    /**
     * Highlighter personalizado para subrayado ondulado
     */
    private static class UnderlineHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        
        public UnderlineHighlightPainter(Color color) {
            super(color);
        }
        
        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, 
                               Shape bounds, JTextComponent c, View view) {
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.RED);
            
            try {
                // Obtener la forma del texto a subrayar
                Rectangle r0 = c.modelToView2D(offs0).getBounds();
                Rectangle r1 = c.modelToView2D(offs1).getBounds();
                
                if (r0.y != r1.y) {
                    // El error abarca múltiples líneas
                    return super.paintLayer(g, offs0, offs1, bounds, c, view);
                }
                
                // Dibujar línea ondulada roja debajo del texto
                int x = r0.x;
                int y = r0.y + r0.height - 2;
                int width = r1.x - r0.x;
                
                drawWavyLine(g2d, x, y, width);
                
                return bounds;
                
            } catch (BadLocationException e) {
                return null;
            }
        }
        
        /**
         * Dibuja una línea ondulada
         */
        private void drawWavyLine(Graphics2D g2d, int x, int y, int width) {
            int amplitude = 2;  // Altura de las ondas
            int wavelength = 4; // Longitud de cada onda
            
            g2d.setStroke(new BasicStroke(1.0f));
            
            for (int i = 0; i < width; i += wavelength) {
                int x1 = x + i;
                int y1 = y + (i % (wavelength * 2) < wavelength ? 0 : amplitude);
                int x2 = x + Math.min(i + wavelength, width);
                int y2 = y + ((i + wavelength) % (wavelength * 2) < wavelength ? 0 : amplitude);
                
                g2d.drawLine(x1, y1, x2, y2);
            }
        }
    }
    
    /**
     * Libera los recursos del corrector
     */
    public void shutdown() {
        if (languageTool != null) {
            // LanguageTool no necesita cierre explícito
            languageTool = null;
        }
    }
}
