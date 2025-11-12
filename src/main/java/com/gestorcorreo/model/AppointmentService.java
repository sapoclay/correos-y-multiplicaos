package com.gestorcorreo.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de persistencia de citas en JSON local.
 */
public class AppointmentService {
    private static final String DEFAULT_APP_DIR = System.getProperty("user.home") + "/.correosymultiplicaos";
    private static final String APP_DIR_PROP = "gestor.data.dir"; // Permite override en tests
    private static final String FILE_NAME = "appointments.json";

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();

    private static final Type LIST_TYPE = new TypeToken<List<Appointment>>(){}.getType();

    private static Path appDirPath() {
        String base = System.getProperty(APP_DIR_PROP, DEFAULT_APP_DIR);
        return Paths.get(base);
    }

    private static Path filePath() {
        return appDirPath().resolve(FILE_NAME);
    }

    public synchronized List<Appointment> loadAll() {
        try {
            Path p = filePath();
            if (!Files.exists(p)) return new ArrayList<>();
            String json = Files.readString(p, StandardCharsets.UTF_8);
            List<Appointment> list = gson.fromJson(json, LIST_TYPE);
            if (list == null) return new ArrayList<>();
            return sort(list);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public synchronized void saveAll(List<Appointment> items) throws IOException {
        if (!Files.exists(appDirPath())) {
            Files.createDirectories(appDirPath());
        }
        String json = gson.toJson(sort(items), LIST_TYPE);
        Files.writeString(filePath(), json, StandardCharsets.UTF_8);
    }

    public synchronized Appointment upsert(Appointment appt) throws IOException {
        List<Appointment> list = loadAll();
        Optional<Appointment> existing = list.stream().filter(a -> Objects.equals(a.getId(), appt.getId())).findFirst();
        if (existing.isPresent()) {
            Appointment e = existing.get();
            e.setTitle(appt.getTitle());
            e.setDescription(appt.getDescription());
            e.setStart(appt.getStart());
            e.setEnd(appt.getEnd());
            e.setLocation(appt.getLocation());
            e.setAllDay(appt.isAllDay());
        } else {
            list.add(appt);
        }
        saveAll(list);
        return appt;
    }

    public synchronized boolean delete(String id) throws IOException {
        List<Appointment> list = loadAll();
        boolean removed = list.removeIf(a -> Objects.equals(a.getId(), id));
        if (removed) saveAll(list);
        return removed;
    }

    public List<Appointment> forDay(LocalDate day) {
        List<Appointment> list = loadAll();
        return list.stream().filter(a -> isSameDay(a, day)).collect(Collectors.toList());
    }

    public List<Appointment> between(LocalDate from, LocalDate to) {
        List<Appointment> list = loadAll();
        return list.stream().filter(a -> overlaps(a, from, to)).collect(Collectors.toList());
    }

    private boolean isSameDay(Appointment a, LocalDate d) {
        if (a.isAllDay()) {
            return a.getStart() != null && a.getStart().toLocalDate().equals(d);
        }
        if (a.getStart() == null) return false;
        return a.getStart().toLocalDate().equals(d) || (a.getEnd() != null && a.getEnd().toLocalDate().equals(d));
    }

    private boolean overlaps(Appointment a, LocalDate from, LocalDate to) {
        LocalDate start = a.getStart() != null ? a.getStart().toLocalDate() : null;
        LocalDate end = a.getEnd() != null ? a.getEnd().toLocalDate() : start;
        if (start == null) return false;
        if (end == null) end = start;
        return !start.isAfter(to) && !end.isBefore(from);
    }

    private List<Appointment> sort(List<Appointment> items) {
        Collections.sort(items, Comparator
                .comparing(Appointment::getStart, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Appointment::getTitle, Comparator.nullsLast(String::compareToIgnoreCase))
        );
        return items;
    }
}
