package com.korapass.repository;

import com.korapass.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.event.id = :eventId and s.label in :labels order by s.id")
    List<Seat> lockByEventAndLabels(@Param("eventId") Long eventId, @Param("labels") List<String> labels);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.reservationId = :reservationId order by s.id")
    List<Seat> lockByReservationId(@Param("reservationId") String reservationId);
}
