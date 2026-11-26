package com.korapass.api;

import java.time.LocalDateTime;
import java.util.List;

public final class Requests {
    private Requests() {
    }

    public record CreateEvent(String name, LocalDateTime startsAt, List<String> seats) {
    }

    public record ReserveSeats(String customerEmail, List<String> seatLabels) {
    }

    public record ScanTicket(String qrToken) {
    }
}
