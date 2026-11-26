package com.korapass.repository;

import com.korapass.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.qrToken = :token")
    Optional<Ticket> lockByQrToken(@Param("token") String token);
}
