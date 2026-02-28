package org.example.model;

import java.time.LocalDateTime;

public class Slot {

    private LocalDateTime start;
    private LocalDateTime end;
    private int rhId;

    public Slot() {}

    public Slot(LocalDateTime start, LocalDateTime end, int rhId) {
        this.start = start;
        this.end = end;
        this.rhId = rhId;
    }

    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }

    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }

    public int getRhId() { return rhId; }
    public void setRhId(int rhId) { this.rhId = rhId; }

    @Override
    public String toString() {
        return "Slot{start=" + start + ", end=" + end + ", rhId=" + rhId + "}";
    }
}
