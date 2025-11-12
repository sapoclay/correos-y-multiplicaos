package com.gestorcorreo.model;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Editor simple para crear/editar citas.
 */
public class AppointmentEditorDialog extends JDialog {
    private final AppointmentService service;
    private Appointment editing;
    private final JTextField titleField = new JTextField();
    private final JTextField locationField = new JTextField();
    private final JTextArea descriptionArea = new JTextArea(5,40);
    private final JCheckBox allDayCheck = new JCheckBox("Todo el día");
    private final JFormattedTextField dateField = new JFormattedTextField(createDateMask()); // dd-MM-yyyy
    private final JTextField startField = new JTextField(); // HH:mm
    private final JTextField endField = new JTextField(); // HH:mm
    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public AppointmentEditorDialog(Window owner, AppointmentService service, LocalDate presetDay) {
        super(owner, "Cita", ModalityType.APPLICATION_MODAL);
        this.service = service;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        setContentPane(root);

        JPanel form = new JPanel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,4,4,4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Título"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; form.add(titleField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Lugar"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; form.add(locationField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Fecha"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; form.add(dateField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; form.add(new JLabel("Inicio"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; form.add(startField, gbc);
        gbc.gridx = 0; gbc.gridy = 4; form.add(new JLabel("Fin"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; form.add(endField, gbc);
        gbc.gridx = 0; gbc.gridy = 5; form.add(allDayCheck, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        form.add(new JScrollPane(descriptionArea), gbc);

        root.add(form, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Guardar");
        saveBtn.addActionListener(this::doSave);
        JButton cancelBtn = new JButton("Cancelar");
        cancelBtn.addActionListener(e -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(saveBtn);
        actions.add(cancelBtn);
        root.add(actions, BorderLayout.SOUTH);

        if (presetDay != null) {
            dateField.setText(presetDay.format(DATE_FMT));
            startField.setText("09:00");
            endField.setText("10:00");
        }
    }

    public void edit(Appointment appt) {
        this.editing = appt;
        titleField.setText(appt.getTitle());
        locationField.setText(appt.getLocation());
        descriptionArea.setText(appt.getDescription());
        allDayCheck.setSelected(appt.isAllDay());
        if (appt.getStart() != null) dateField.setText(appt.getStart().toLocalDate().format(DATE_FMT));
        if (appt.getStart() != null) startField.setText(appt.getStart().toLocalTime().format(TIME_FMT));
        if (appt.getEnd() != null) endField.setText(appt.getEnd().toLocalTime().format(TIME_FMT));
    }

    private void doSave(ActionEvent e) {
        try {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El título es obligatorio", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean allDay = allDayCheck.isSelected();
            String dateText = dateField.getText().trim();
            if (!dateText.isEmpty() && !dateText.matches("\\d{2}-\\d{2}-\\d{4}")) {
                JOptionPane.showMessageDialog(this, "La fecha debe tener formato dd-MM-aaaa", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            LocalDate date = dateText.isBlank() ? null : LocalDate.parse(dateText, DATE_FMT);
            LocalDateTime start = null;
            LocalDateTime end = null;
            if (date != null) {
                if (!allDay) {
                    if (!startField.getText().isBlank()) {
                        LocalTime st = LocalTime.parse(startField.getText().trim(), TIME_FMT);
                        start = LocalDateTime.of(date, st);
                    }
                    if (!endField.getText().isBlank()) {
                        LocalTime et = LocalTime.parse(endField.getText().trim(), TIME_FMT);
                        end = LocalDateTime.of(date, et);
                    }
                    if (start != null && end != null && end.isBefore(start)) {
                        JOptionPane.showMessageDialog(this, "La hora fin es anterior a la inicial", "Validación", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } else {
                    start = LocalDateTime.of(date, LocalTime.MIDNIGHT);
                }
            }

            Appointment appt = editing != null ? editing : new Appointment();
            appt.setTitle(title);
            appt.setDescription(descriptionArea.getText());
            appt.setLocation(locationField.getText());
            appt.setAllDay(allDay);
            appt.setStart(start);
            appt.setEnd(end);

            // Comprobar solapado simple
            if (!allDay) {
                List<Appointment> sameDay = service.forDay(date);
                boolean overlap = sameDay.stream().anyMatch(a -> !a.getId().equals(appt.getId()) && a.overlaps(appt));
                if (overlap) {
                    int r = JOptionPane.showConfirmDialog(this, "La cita se solapa con otra. ¿Guardar igualmente?", "Solapado", JOptionPane.YES_NO_OPTION);
                    if (r != JOptionPane.YES_OPTION) return;
                }
            }

            service.upsert(appt);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private MaskFormatter createDateMask() {
        try {
            MaskFormatter mf = new MaskFormatter("##-##-####");
            mf.setPlaceholderCharacter('_');
            mf.setAllowsInvalid(false);
            return mf;
        } catch (Exception e) {
            return null; // fallback: JFormattedTextField aceptará texto libre si null, pero validamos al guardar
        }
    }
}
