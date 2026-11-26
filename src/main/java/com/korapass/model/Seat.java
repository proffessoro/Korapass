package com.korapass.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "label"}))
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;

    private String label;

    @Enumerated(EnumType.STRING)
    private SeatStatus status = SeatStatus.AVAILABLE;

    private String reservationId;
    private Instant reservedUntil;

    protected Seat() {
    }

    public Seat(String label) {
        this.label = label;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getLabel() {
        return label;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public String getReservationId() {
        return reservationId;
    }

    public Instant getReservedUntil() {
        return reservedUntil;
    }

    public void reserve(String reservationId, Instant reservedUntil) {
        this.status = SeatStatus.RESERVED;
        this.reservationId = reservationId;
        this.reservedUntil = reservedUntil;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
        this.reservationId = null;
        this.reservedUntil = null;
    }

    public void sell() {
        this.status = SeatStatus.SOLD;
        this.reservationId = null;
        this.reservedUntil = null;
    }
}
