package org.example.services;

import org.example.model.Entretien;
import org.example.model.Slot;

import java.sql.SQLException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class SchedulingService {

    private static final int DUREE = 25;
    private static final int BUFFER = 5;
    private static final int SLOT_TOTAL = DUREE + BUFFER;

    private final EntretienService entretienService;

    public SchedulingService(EntretienService entretienService) {
        this.entretienService = entretienService;
    }

    public List<LocalDateTime> generateDailySlots(LocalDate date) {

        List<LocalDateTime> slots = new ArrayList<>();

        generatePeriod(date, LocalTime.of(8, 0),
                LocalTime.of(13, 0), slots);

        generatePeriod(date, LocalTime.of(13, 30),
                LocalTime.of(18, 0), slots);

        return slots;
    }

    private void generatePeriod(LocalDate date,
                                LocalTime start,
                                LocalTime end,
                                List<LocalDateTime> slots) {

        LocalTime current = start;

        while (current.plusMinutes(SLOT_TOTAL).compareTo(end) <= 0) {
            slots.add(LocalDateTime.of(date, current));
            current = current.plusMinutes(SLOT_TOTAL);
        }
    }

    public boolean hasConflict(LocalDateTime start,
                               int rhId) throws SQLException {

        LocalDateTime end = start.plusMinutes(DUREE);

        List<Entretien> list =
                entretienService.getEntretiensByDate(start.toLocalDate());

        for (Entretien e : list) {

            if (e.getCandidatureId() != rhId) continue;

            LocalDateTime existingStart = e.getDateEntretien();
            LocalDateTime existingEnd =
                    existingStart.plusMinutes(DUREE);

            boolean overlap =
                    start.isBefore(existingEnd) &&
                            end.isAfter(existingStart);

            if (overlap) return true;
        }

        return false;
    }

    public List<Slot> suggestAvailableSlots(LocalDate date,
                                            List<Integer> rhIds)
            throws SQLException {

        List<Slot> result = new ArrayList<>();
        List<LocalDateTime> baseSlots =
                generateDailySlots(date);

        for (LocalDateTime slotStart : baseSlots) {

            for (int rhId : rhIds) {

                if (!hasConflict(slotStart, rhId)) {

                    result.add(
                            new Slot(
                                    slotStart,
                                    slotStart.plusMinutes(DUREE),
                                    rhId
                            )
                    );
                }
            }
        }

        return result;
    }
}
