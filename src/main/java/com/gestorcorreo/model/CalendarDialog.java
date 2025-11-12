package com.gestorcorreo.model;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Dialogo simple de calendario mensual con listado de citas por día.
 */
public class CalendarDialog extends JDialog {
    private YearMonth currentMonth;
    private final AppointmentService service;
    private final JPanel daysPanel;
    private final JLabel monthLabel;
    private final JTextArea dayDetails;
    private LocalDate selectedDate;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public CalendarDialog(Frame owner, AppointmentService service) {
        super(owner, "Calendario", true);
        this.service = service;
        this.currentMonth = YearMonth.now();
        // Selección por defecto: hoy si pertenece al mes visible inicial
        LocalDate today = LocalDate.now();
        if (YearMonth.from(today).equals(this.currentMonth)) {
            this.selectedDate = today;
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        JButton prev = new JButton("←");
        JButton next = new JButton("→");
        monthLabel = new JLabel("Mes", SwingConstants.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 16f));
        prev.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); refreshMonth(); });
        next.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); refreshMonth(); });
        header.add(prev, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Days grid
        daysPanel = new JPanel(new GridLayout(0,7,4,4));
        root.add(daysPanel, BorderLayout.CENTER);

        // Detail area
        dayDetails = new JTextArea();
        dayDetails.setEditable(false);
        dayDetails.setLineWrap(true);
        dayDetails.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(dayDetails);
        scroll.setPreferredSize(new Dimension(250, 0));
        root.add(scroll, BorderLayout.EAST);

        // Bottom actions
    JButton createBtn = new JButton("Nueva cita");
    createBtn.addActionListener(e -> openEditor(selectedDate != null ? selectedDate : LocalDate.now()));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(createBtn);
        root.add(bottom, BorderLayout.SOUTH);

        refreshMonth();
    }

    private void refreshMonth() {
        daysPanel.removeAll();
    // Calcular total de citas del mes y pluralizar
    int totalMonth = service.between(currentMonth.atDay(1), currentMonth.atEndOfMonth()).size();
    String palabra = (totalMonth == 1) ? "cita" : "citas";
    monthLabel.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentMonth.getYear() +
        "  (" + totalMonth + " " + palabra + ")" );

        // Cabecera días semana (Lunes a Domingo)
        for (DayOfWeek dow : DayOfWeek.values()) {
            JLabel lbl = new JLabel(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            daysPanel.add(lbl);
        }

        LocalDate first = currentMonth.atDay(1);
        // 1 (Lunes) -> 0 desplazamientos, 2 (Martes) ->1 ... 7 (Domingo) ->6
        int shift = first.getDayOfWeek().getValue() - 1; // MONDAY=1
        if (shift < 0) shift = 0;
        for (int i = 0; i < shift; i++) {
            daysPanel.add(new JLabel(""));
        }

        int length = currentMonth.lengthOfMonth();
        for (int day=1; day<=length; day++) {
            LocalDate date = currentMonth.atDay(day);
            List<Appointment> appts = service.forDay(date);
            JPanel cell = new JPanel(new BorderLayout());
            boolean isSelected = selectedDate != null && selectedDate.equals(date);
            if (isSelected) {
                cell.setBorder(BorderFactory.createLineBorder(new Color(0,120,215), 2));
                cell.setBackground(new Color(227,242,253)); // azul muy claro
                cell.setOpaque(true);
            } else {
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            }
            JLabel num = new JLabel(String.valueOf(day), SwingConstants.RIGHT);
            num.setFont(num.getFont().deriveFont(Font.PLAIN, 12f));
            cell.add(num, BorderLayout.NORTH);

            if (!appts.isEmpty()) {
                DefaultListModel<Appointment> lm = new DefaultListModel<>();
                appts.forEach(lm::addElement);
                JList<Appointment> list = new JList<>(lm);
                list.setCellRenderer((jList, value, index, isSel, cellHasFocus) -> {
                    JLabel lbl = new JLabel(renderShort(value));
                    lbl.setOpaque(true);
                    Color base = new Color(255,250,230);
                    lbl.setBackground(isSel ? base.darker() : base);
                    lbl.setToolTipText(value.getDescription());
                    return lbl;
                });
                list.setBackground(new Color(255,250,230));
                // Doble clic para editar
                list.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            int idx = list.locationToIndex(e.getPoint());
                            if (idx >= 0) {
                                Appointment a = list.getModel().getElementAt(idx);
                                openEditorForEdit(a);
                            }
                        }
                    }
                });
                // Menú contextual Editar/Eliminar
                JPopupMenu popup = new JPopupMenu();
                JMenuItem editItem = new JMenuItem("Editar");
                editItem.addActionListener(ev -> {
                    Appointment a = list.getSelectedValue();
                    if (a != null) openEditorForEdit(a);
                });
                JMenuItem deleteItem = new JMenuItem("Eliminar");
                deleteItem.addActionListener(ev -> {
                    Appointment a = list.getSelectedValue();
                    if (a != null) deleteAppointment(a);
                });
                popup.add(editItem);
                popup.add(deleteItem);
                list.setComponentPopupMenu(popup);
                cell.add(list, BorderLayout.CENTER);
            }

            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedDate = date;
                    refreshMonth();
                    showDayDetails(date);
                    if (e.getClickCount() == 2) {
                        openEditor(date);
                    }
                }
            });
            daysPanel.add(cell);
        }

        daysPanel.revalidate();
        daysPanel.repaint();
        // Mantener detalle si hay selección dentro del mes actual
        if (selectedDate != null && YearMonth.from(selectedDate).equals(currentMonth)) {
            showDayDetails(selectedDate);
        } else {
            dayDetails.setText("");
        }
    }

    private String renderShort(Appointment a) {
        if (a.isAllDay()) return "(día) " + safe(a.getTitle());
        if (a.getStart() == null) return safe(a.getTitle());
        String hhmm = a.getStart().toLocalTime().toString();
        return hhmm + " " + safe(a.getTitle());
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void showDayDetails(LocalDate date) {
        List<Appointment> appts = service.forDay(date);
        StringBuilder sb = new StringBuilder();
    sb.append(DATE_FMT.format(date)).append('\n');
        sb.append("-----------------------------\n");
        if (appts.isEmpty()) {
            sb.append("(Sin citas)\n");
        } else {
            for (Appointment a : appts) {
                sb.append(detail(a)).append("\n\n");
            }
        }
        dayDetails.setText(sb.toString());
    }

    private String detail(Appointment a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.getTitle()).append('\n');
        if (a.isAllDay()) {
            sb.append("Evento de todo el día\n");
        } else if (a.getStart() != null) {
            sb.append("Inicio: ").append(a.getStart().format(DATETIME_FMT)).append('\n');
            if (a.getEnd() != null) sb.append("Fin: ").append(a.getEnd().format(DATETIME_FMT)).append('\n');
        }
        if (a.getLocation() != null && !a.getLocation().isBlank()) sb.append("Lugar: ").append(a.getLocation()).append('\n');
        if (a.getDescription() != null && !a.getDescription().isBlank()) sb.append(a.getDescription()).append('\n');
        sb.append("ID: ").append(a.getId());
        return sb.toString();
    }

    private void openEditor(LocalDate presetDay) {
        AppointmentEditorDialog dlg = new AppointmentEditorDialog(this, service, presetDay);
        dlg.setVisible(true);
        refreshMonth();
    }

    private void openEditorForEdit(Appointment appt) {
        if (appt == null) return;
        LocalDate preset = appt.getStart() != null ? appt.getStart().toLocalDate() : null;
        AppointmentEditorDialog dlg = new AppointmentEditorDialog(this, service, preset);
        dlg.edit(appt);
        dlg.setVisible(true);
        refreshMonth();
    }

    private void deleteAppointment(Appointment appt) {
        if (appt == null) return;
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar la cita '" + (appt.getTitle() == null ? "(sin título)" : appt.getTitle()) + "'?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            try {
                service.delete(appt.getId());
                refreshMonth();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo eliminar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
