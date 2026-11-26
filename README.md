# Korapass

Korapass is a Spring Boot event ticketing API with seat reservations, ticket purchases,
QR ticket generation, and single-use venue check-in. Reservations hold seats for 10 minutes.
The default development database is a local H2 file (`./korapass`); configure
`spring.datasource.*` for a production database before deployment.

## Run

Requirements: Java 17 and Maven.

```sh
mvn spring-boot:run
```

## API

Create an event and define its seat labels:

```http
POST /api/events
Content-Type: application/json

{"name":"Summer show","startsAt":"2030-07-12T19:00:00","seats":["A1","A2"]}
```

Reserve available seats:

```http
POST /api/events/1/reservations
Content-Type: application/json

{"customerEmail":"buyer@example.com","seatLabels":["A1"]}
```

Purchase the reservation with `POST /api/reservations/{reservationId}/purchase`.
Each purchased seat returns a unique `qrToken` and a PNG QR code as base64 in
`qrCodeBase64`. The ticket buyer can present the code at the venue; submit the
decoded token to `POST /api/tickets/scan` (`{"qrToken":"..."}`). A successful
scan checks in the ticket, and subsequent scans are rejected.

Purchasing currently records the sale directly; connect a payment provider before
using this API for real-money transactions. Protect event administration and venue
scanner endpoints with authentication/authorization before public deployment.

## Test

```sh
mvn test
```
