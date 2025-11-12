package com.gestorcorreo.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class AppointmentOverlapTest {

    @Test
    void testOverlapDetection() {
        LocalDate d = LocalDate.of(2025, 1, 15);
        Appointment a = new Appointment("A", "", LocalDateTime.of(d, LocalTime.of(9,0)), LocalDateTime.of(d, LocalTime.of(10,0)), "", false);
        Appointment b = new Appointment("B", "", LocalDateTime.of(d, LocalTime.of(9,30)), LocalDateTime.of(d, LocalTime.of(10,30)), "", false);
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));

        Appointment c = new Appointment("C", "", LocalDateTime.of(d, LocalTime.of(10,0)), LocalDateTime.of(d, LocalTime.of(11,0)), "", false);
        assertTrue(a.overlaps(c)); // comparte límite

        Appointment dEvent = new Appointment("D", "", LocalDateTime.of(d, LocalTime.of(11,0)), LocalDateTime.of(d, LocalTime.of(12,0)), "", false);
        assertFalse(a.overlaps(dEvent));

        Appointment allDay = new Appointment("AllDay", "", LocalDateTime.of(d, LocalTime.MIDNIGHT), null, "", true);
        assertFalse(allDay.overlaps(a));
        assertFalse(a.overlaps(allDay));
    }
}
