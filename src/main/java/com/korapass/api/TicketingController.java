package com.korapass.api;

import com.korapass.service.TicketingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TicketingController {
    private final TicketingService ticketingService;

    public TicketingController(TicketingService ticketingService) {
        this.ticketingService = ticketingService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.EventView createEvent(@RequestBody Requests.CreateEvent request) {
        return ticketingService.createEvent(request);
    }

    @GetMapping("/events")
    public List<Responses.EventView> listEvents() {
        return ticketingService.listEvents();
    }

    @PostMapping("/events/{eventId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.ReservationView reserve(@PathVariable Long eventId,
                                             @RequestBody Requests.ReserveSeats request) {
        return ticketingService.reserveSeats(eventId, request);
    }

    @PostMapping("/reservations/{reservationId}/purchase")
    public List<Responses.TicketView> purchase(@PathVariable String reservationId) {
        return ticketingService.purchase(reservationId);
    }

    @PostMapping("/tickets/scan")
    public Responses.ScanView scan(@RequestBody Requests.ScanTicket request) {
        return ticketingService.scan(request == null ? null : request.qrToken());
    }
}
