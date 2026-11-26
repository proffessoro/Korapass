package com.korapass.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.korapass.api.ApiException;
import com.korapass.api.Requests;
import com.korapass.api.Responses;
import com.korapass.model.Event;
import com.korapass.model.Reservation;
import com.korapass.model.ReservationStatus;
import com.korapass.model.Seat;
import com.korapass.model.SeatStatus;
import com.korapass.model.Ticket;
import com.korapass.repository.EventRepository;
import com.korapass.repository.ReservationRepository;
import com.korapass.repository.SeatRepository;
import com.korapass.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class TicketingService {
    private static final Duration RESERVATION_DURATION = Duration.ofMinutes(10);

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;

    public TicketingService(EventRepository eventRepository, SeatRepository seatRepository,
                            ReservationRepository reservationRepository, TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public Responses.EventView createEvent(Requests.CreateEvent request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.startsAt() == null || request.seats() == null || request.seats().isEmpty()) {
            throw ApiException.badRequest("Event name, start time, and at least one seat are required");
        }
        List<String> labels = request.seats().stream()
                .map(label -> label == null ? null : label.trim()).toList();
        if (labels.stream().anyMatch(label -> label == null || label.isBlank())
                || new HashSet<>(labels).size() != labels.size()) {
            throw ApiException.badRequest("Seat labels must be non-empty and unique");
        }

        Event event = new Event(request.name().trim(), request.startsAt());
        labels.forEach(label -> event.addSeat(new Seat(label)));
        Event saved = eventRepository.save(event);
        return new Responses.EventView(saved.getId(), saved.getName(), saved.getStartsAt(),
                labels);
    }

    @Transactional(readOnly = true)
    public List<Responses.EventView> listEvents() {
        return eventRepository.findAll().stream()
                .map(event -> new Responses.EventView(event.getId(), event.getName(), event.getStartsAt(),
                        event.getSeats().stream().map(Seat::getLabel).toList()))
                .toList();
    }

    @Transactional
    public Responses.ReservationView reserveSeats(Long eventId, Requests.ReserveSeats request) {
        if (request == null || request.customerEmail() == null || request.customerEmail().isBlank()
                || request.seatLabels() == null || request.seatLabels().isEmpty()) {
            throw ApiException.badRequest("Customer email and at least one seat are required");
        }
        List<String> labels = request.seatLabels().stream().map(label -> label == null ? null : label.trim()).toList();
        if (labels.stream().anyMatch(label -> label == null || label.isBlank())
                || new HashSet<>(labels).size() != labels.size()) {
            throw ApiException.badRequest("Seat labels must be non-empty and unique");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));
        List<Seat> seats = seatRepository.lockByEventAndLabels(eventId, labels);
        if (seats.size() != labels.size()) {
            throw ApiException.notFound("One or more seats were not found for this event");
        }

        Instant now = Instant.now();
        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.RESERVED && !seat.getReservedUntil().isAfter(now)) {
                seat.release();
            }
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw ApiException.conflict("One or more seats are no longer available");
            }
        }

        Instant expiresAt = now.plus(RESERVATION_DURATION);
        String reservationId = UUID.randomUUID().toString();
        reservationRepository.save(new Reservation(reservationId, event, request.customerEmail().trim(), expiresAt));
        seats.forEach(seat -> seat.reserve(reservationId, expiresAt));
        return new Responses.ReservationView(reservationId, expiresAt);
    }

    @Transactional
    public List<Responses.TicketView> purchase(String reservationId) {
        Reservation reservation = reservationRepository.lockById(reservationId)
                .orElseThrow(() -> ApiException.notFound("Reservation not found"));
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw ApiException.conflict("Reservation has already been completed");
        }
        Instant now = Instant.now();
        if (!reservation.getExpiresAt().isAfter(now)) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            throw ApiException.conflict("Reservation has expired");
        }

        List<Seat> seats = seatRepository.lockByReservationId(reservationId);
        if (seats.isEmpty() || seats.stream().anyMatch(seat ->
                seat.getStatus() != SeatStatus.RESERVED || !seat.getReservedUntil().isAfter(now))) {
            throw ApiException.conflict("Reserved seats are no longer available");
        }

        List<Ticket> tickets = seats.stream()
                .map(seat -> new Ticket(reservation, seat, UUID.randomUUID().toString()))
                .toList();
        seats.forEach(Seat::sell);
        reservation.setStatus(ReservationStatus.PURCHASED);
        ticketRepository.saveAll(tickets);
        ticketRepository.flush();
        return tickets.stream().map(this::ticketView).toList();
    }

    @Transactional
    public Responses.ScanView scan(String qrToken) {
        if (qrToken == null || qrToken.isBlank()) {
            throw ApiException.badRequest("QR token is required");
        }
        Ticket ticket = ticketRepository.lockByQrToken(qrToken.trim())
                .orElseThrow(() -> ApiException.notFound("Ticket not found"));
        if (ticket.getCheckedInAt() != null) {
            throw ApiException.conflict("Ticket has already been scanned");
        }
        Instant checkedInAt = Instant.now();
        ticket.checkIn(checkedInAt);
        return new Responses.ScanView(ticket.getId(), ticket.getSeat().getLabel(), checkedInAt, "VALID");
    }

    private Responses.TicketView ticketView(Ticket ticket) {
        return new Responses.TicketView(ticket.getId(), ticket.getSeat().getLabel(), ticket.getQrToken(),
                createQrCode(ticket.getQrToken()));
    }

    private String createQrCode(String token) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 240, 240);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (WriterException | IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate ticket QR code");
        }
    }
}
