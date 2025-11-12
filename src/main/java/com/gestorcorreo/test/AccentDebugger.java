package com.gestorcorreo.test;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Herramienta de diagnóstico para ver qué caracteres Unicode genera el teclado
 */
public class AccentDebugger extends JFrame {
    private JTextArea textArea;
    private JTextArea debugArea;
    
    public AccentDebugger() {
        setTitle("Depurador de Acentos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout(10, 10));
        
        // Instrucciones
        JLabel instructions = new JLabel("<html><b>Instrucciones:</b> Escribe en el área de texto superior. " +
            "Abajo verás los códigos Unicode de cada carácter.</html>");
        instructions.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(instructions, BorderLayout.NORTH);
        
        // Área de texto para escribir
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel("Escribe aquí (prueba: presiona ´ luego a):"), BorderLayout.NORTH);
        textArea = new JTextArea();
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 18));
        textArea.setLineWrap(true);
        inputPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        JPanel debugPanel = new JPanel(new BorderLayout());
        debugPanel.add(new JLabel("Análisis de caracteres Unicode:"), BorderLayout.NORTH);
        debugArea = new JTextArea();
        debugArea.setEditable(false);
        debugArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        debugPanel.add(new JScrollPane(debugArea), BorderLayout.CENTER);
        
        centerPanel.add(inputPanel);
        centerPanel.add(debugPanel);
        add(centerPanel, BorderLayout.CENTER);
        
        // Listener para analizar el texto
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> analyzeText());
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> analyzeText());
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> analyzeText());
            }
        });
        
        // Botón para limpiar
        JButton clearButton = new JButton("Limpiar");
        clearButton.addActionListener(e -> {
            textArea.setText("");
            debugArea.setText("");
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        setLocationRelativeTo(null);
    }
    
    private void analyzeText() {
        String text = textArea.getText();
        StringBuilder analysis = new StringBuilder();
        analysis.append("Texto completo: \"").append(text).append("\"\n");
        analysis.append("Longitud: ").append(text.length()).append(" caracteres\n\n");
        analysis.append("Análisis carácter por carácter:\n");
        analysis.append("─────────────────────────────────────────────\n");
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int codePoint = text.codePointAt(i);
            
            analysis.append(String.format("Posición %d: '%c' (char=%d, U+%04X) - %s\n", 
                i, c, (int)c, codePoint, getCharacterName(c)));
            
            // Si es un carácter surrogate, avanzar
            if (Character.isHighSurrogate(c)) {
                i++;
            }
        }
        
        analysis.append("\n─────────────────────────────────────────────\n");
        analysis.append("PATRONES DETECTADOS:\n");
        
        // Detectar patrones de acentos
        if (text.contains("´")) {
            analysis.append("✓ Contiene acento agudo independiente (´) U+00B4\n");
        }
        if (text.contains("\u0301")) {
            analysis.append("✓ Contiene combining acute accent U+0301\n");
        }
        if (text.matches(".*[aeiouAEIOU][\u00B4\u0301´].*")) {
            analysis.append("✓ Patrón VOCAL+ACENTO detectado\n");
        }
        if (text.matches(".*[\u00B4\u0301´][aeiouAEIOU].*")) {
            analysis.append("✓ Patrón ACENTO+VOCAL detectado\n");
        }
        if (text.matches(".*[áéíóúÁÉÍÓÚ].*")) {
            analysis.append("✓ Contiene vocales acentuadas correctas\n");
        }
        
        debugArea.setText(analysis.toString());
        debugArea.setCaretPosition(0);
    }
    
    private String getCharacterName(char c) {
        switch (c) {
            case '\n': return "NUEVA LÍNEA";
            case '\t': return "TABULADOR";
            case ' ': return "ESPACIO";
            case '´': return "ACENTO AGUDO (spacing)";
            case '\u0301': return "COMBINING ACUTE ACCENT";
            case '`': return "ACENTO GRAVE (spacing)";
            case '\u0300': return "COMBINING GRAVE ACCENT";
            case 'á': return "LATIN SMALL LETTER A WITH ACUTE";
            case 'é': return "LATIN SMALL LETTER E WITH ACUTE";
            case 'í': return "LATIN SMALL LETTER I WITH ACUTE";
            case 'ó': return "LATIN SMALL LETTER O WITH ACUTE";
            case 'ú': return "LATIN SMALL LETTER U WITH ACUTE";
            case 'Á': return "LATIN CAPITAL LETTER A WITH ACUTE";
            case 'É': return "LATIN CAPITAL LETTER E WITH ACUTE";
            case 'Í': return "LATIN CAPITAL LETTER I WITH ACUTE";
            case 'Ó': return "LATIN CAPITAL LETTER O WITH ACUTE";
            case 'Ú': return "LATIN CAPITAL LETTER U WITH ACUTE";
            case 'ñ': return "LATIN SMALL LETTER N WITH TILDE";
            case 'Ñ': return "LATIN CAPITAL LETTER N WITH TILDE";
            default:
                if (Character.isLetter(c)) return "LETRA";
                if (Character.isDigit(c)) return "DÍGITO";
                if (Character.isWhitespace(c)) return "ESPACIO EN BLANCO";
                return "OTRO";
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AccentDebugger debugger = new AccentDebugger();
            debugger.setVisible(true);
        });
    }
}
