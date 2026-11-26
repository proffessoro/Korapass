package com.korapass.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public final class Responses {
    private Responses() {
    }

    public record EventView(Long id, String name, LocalDateTime startsAt, List<String> seats) {
    }

    public record ReservationView(String reservationId, Instant expiresAt) {
    }

    public record TicketView(Long ticketId, String seat, String qrToken, String qrCodeBase64) {
    }

    public record ScanView(Long ticketId, String seat, Instant checkedInAt, String status) {
    }
}
