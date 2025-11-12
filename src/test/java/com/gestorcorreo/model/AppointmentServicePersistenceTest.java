package com.gestorcorreo.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AppointmentServicePersistenceTest {

    @TempDir
    Path tempDir;

    private String originalDataDirProp;

    @BeforeEach
    void setUp() {
        originalDataDirProp = System.getProperty("gestor.data.dir");
        System.setProperty("gestor.data.dir", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (originalDataDirProp == null) {
            System.clearProperty("gestor.data.dir");
        } else {
            System.setProperty("gestor.data.dir", originalDataDirProp);
        }
    }

    @Test
    void testSaveLoadAndDelete() throws Exception {
        AppointmentService service = new AppointmentService();

        Appointment a1 = new Appointment("Reunión", "Revisión sprint",
                LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0)),
                LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0)),
                "Sala A", false);

        Appointment a2 = new Appointment("Comida", "Con cliente",
                LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)),
                LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 0)),
                "Restaurante", false);

        service.upsert(a1);
        service.upsert(a2);

        List<Appointment> loaded = service.loadAll();
        assertEquals(2, loaded.size());
        assertTrue(loaded.stream().anyMatch(a -> a.getTitle().equals("Reunión")));
        assertTrue(loaded.stream().anyMatch(a -> a.getTitle().equals("Comida")));

        // Delete one
        boolean removed = service.delete(a1.getId());
        assertTrue(removed);
        List<Appointment> afterDelete = service.loadAll();
        assertEquals(1, afterDelete.size());
        assertEquals("Comida", afterDelete.get(0).getTitle());
    }

    @Test
    void testQueriesForDayAndBetween() throws Exception {
        AppointmentService service = new AppointmentService();

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Appointment todayEvent = new Appointment("Día entero", "",
                LocalDateTime.of(today, LocalTime.MIDNIGHT), null, "", true);
        Appointment rangeEvent = new Appointment("Rango", "",
                LocalDateTime.of(today, LocalTime.of(18, 0)),
                LocalDateTime.of(tomorrow, LocalTime.of(9, 0)), "", false);

        service.upsert(todayEvent);
        service.upsert(rangeEvent);

        List<Appointment> onlyToday = service.forDay(today);
        assertFalse(onlyToday.isEmpty());

        List<Appointment> inBetween = service.between(today, tomorrow);
        assertTrue(inBetween.size() >= 2);
    }
}
