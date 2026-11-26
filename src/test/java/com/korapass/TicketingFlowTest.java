package com.korapass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korapass.repository.EventRepository;
import com.korapass.repository.ReservationRepository;
import com.korapass.repository.SeatRepository;
import com.korapass.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:korapass-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TicketingFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void clearDatabase() {
        ticketRepository.deleteAll();
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void reservationPurchaseAndSingleUseQrCheckIn() throws Exception {
        long eventId = createEvent();
        String reservationId = reserve(eventId);

        mockMvc.perform(post("/api/events/{eventId}/reservations", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"other@example.com","seatLabels":["A1"]}
                                """))
                .andExpect(status().isConflict());

        MvcResult purchase = mockMvc.perform(post("/api/reservations/{id}/purchase", reservationId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ticket = objectMapper.readTree(purchase.getResponse().getContentAsString()).get(0);
        String token = ticket.get("qrToken").asText();
        byte[] qrImage = Base64.getDecoder().decode(ticket.get("qrCodeBase64").asText());
        assertThat(token).isNotBlank();
        assertThat(qrImage).startsWith(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47});

        mockMvc.perform(post("/api/tickets/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of("qrToken", token))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/tickets/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of("qrToken", token))))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsUnknownQrTokenAndDuplicateSeatLabels() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Concert","startsAt":"2030-01-01T20:00:00","seats":["A1"," A1 "]}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/tickets/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"qrToken":"not-a-ticket"}
                                """))
                .andExpect(status().isNotFound());
    }

    private long createEvent() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Concert","startsAt":"2030-01-01T20:00:00","seats":["A1","A2"]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String reserve(long eventId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/events/{eventId}/reservations", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"buyer@example.com","seatLabels":["A1"]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("reservationId").asText();
    }
}
