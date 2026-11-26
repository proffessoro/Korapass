package com.korapass.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Event event;

    private String customerEmail;
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    protected Reservation() {
    }

    public Reservation(String id, Event event, String customerEmail, Instant expiresAt) {
        this.id = id;
        this.event = event;
        this.customerEmail = customerEmail;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
