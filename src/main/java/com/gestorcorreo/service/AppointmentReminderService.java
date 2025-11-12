package com.gestorcorreo.service;

import com.gestorcorreo.model.Appointment;
import com.gestorcorreo.model.AppointmentService;
import com.gestorcorreo.ui.SystemTrayManager;

import java.time.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Servicio que recuerda citas del día siguiente mostrando una notificación.
 * - Ejecuta un chequeo inmediato al iniciar.
 * - Programa un recordatorio diario a una hora fija (por defecto 18:00 local).
 */
public class AppointmentReminderService {
    private static AppointmentReminderService instance;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "appt-reminder");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> dailyTask;
    private LocalDate lastNotifiedForDay; // Evitar notificar dos veces el mismo día
    // Firma de las citas por fecha (para silenciar si no hay cambios)
    private final Map<LocalDate, String> lastSignatures = new HashMap<>();
    private final AppointmentService apptService = new AppointmentService();

    public static synchronized AppointmentReminderService getInstance() {
        if (instance == null) instance = new AppointmentReminderService();
        return instance;
    }

    private AppointmentReminderService() {}

    public synchronized void start(SystemTrayManager tray) {
        // Chequeo inmediato al iniciar
        try { checkForTomorrow(tray); } catch (Exception ignored) {}

    // Hora configurable desde preferencias
    int hour = com.gestorcorreo.service.PreferencesService.getInstance().getPreferences().getReminderHour();
    long initialDelay = computeInitialDelayTo(LocalTime.of(hour, 0));
        stop();
        dailyTask = scheduler.scheduleAtFixedRate(() -> {
            try { checkForTomorrow(tray); } catch (Exception ignored) {}
        }, initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (dailyTask != null) {
            dailyTask.cancel(false);
            dailyTask = null;
        }
    }

    private long computeInitialDelayTo(LocalTime target) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.withHour(target.getHour()).withMinute(target.getMinute()).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Duration.between(now, next).getSeconds();
    }

    private void checkForTomorrow(SystemTrayManager tray) {
        LocalDate today = LocalDate.now();
        if (lastNotifiedForDay != null && lastNotifiedForDay.equals(today)) {
            return; // ya notificado hoy
        }
        LocalDate tomorrow = today.plusDays(1);
        List<Appointment> appts = apptService.between(tomorrow, tomorrow);
        if (appts == null || appts.isEmpty()) {
            lastNotifiedForDay = today; // marcar día para no insistir
            return;
        }

        // Silenciar si no hay cambios en las citas de mañana (opcional por preferencia)
        boolean onlyOnChanges = com.gestorcorreo.service.PreferencesService.getInstance()
                .getPreferences().isReminderOnlyOnChanges();
        if (onlyOnChanges) {
            String previous = lastSignatures.get(tomorrow);
            String current = buildSignature(appts);
            if (previous != null && previous.equals(current)) {
                // Sin cambios desde la última vez que notificamos sobre este día
                return;
            }
            // No actualizamos la firma aún; solo cuando notifiquemos con éxito
        }
        String title = "Recordatorio de citas (mañana)";
        String body;
        int n = appts.size();
        if (n == 1) {
            Appointment a = appts.get(0);
            String when = a.isAllDay() ? "todo el día" : (a.getStart() != null ? a.getStart().toLocalTime().toString() : "");
            body = "Mañana tienes 1 cita: " + a.getTitle() + (when.isEmpty()?"":" a las "+when);
        } else {
            body = "Mañana tienes " + n + " citas";
            // Añadir hasta 3 títulos
            String list = appts.stream().limit(3)
                    .map(a -> {
                        String when = a.isAllDay() ? "(día)" : (a.getStart() != null ? a.getStart().toLocalTime().toString() : "");
                        String t = a.getTitle() == null ? "(sin título)" : a.getTitle();
                        return (when.isEmpty()?"":"["+when+"] ") + t;
                    })
                    .collect(Collectors.joining("\n"));
            body += "\n" + list;
        }
        if (tray != null) {
            tray.showNotification(title, body, java.awt.TrayIcon.MessageType.INFO);
        }
        lastNotifiedForDay = today;
        // Guardar firma actual si aplica
        boolean onlyOnChangesSave = com.gestorcorreo.service.PreferencesService.getInstance()
                .getPreferences().isReminderOnlyOnChanges();
        if (onlyOnChangesSave) {
            lastSignatures.put(tomorrow, buildSignature(appts));
        }
    }

    private String buildSignature(List<Appointment> appts) {
        return appts.stream()
                .map(a -> {
                    String id = a.getId() == null ? "" : a.getId();
                    String start = a.getStart() == null ? "" : a.getStart().toString();
                    String title = a.getTitle() == null ? "" : a.getTitle();
                    return id + "|" + start + "|" + title;
                })
                .sorted()
                .collect(Collectors.joining(";"));
    }
}
