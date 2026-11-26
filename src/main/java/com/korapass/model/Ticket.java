package com.korapass.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Seat seat;

    private String qrToken;
    private Instant checkedInAt;

    protected Ticket() {
    }

    public Ticket(Reservation reservation, Seat seat, String qrToken) {
        this.reservation = reservation;
        this.seat = seat;
        this.qrToken = qrToken;
    }

    public Long getId() {
        return id;
    }

    public Seat getSeat() {
        return seat;
    }

    public String getQrToken() {
        return qrToken;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public void checkIn(Instant checkedInAt) {
        this.checkedInAt = checkedInAt;
    }
}
