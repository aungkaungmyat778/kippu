# Kippu

Intercity bus booking, built as Spring Boot microservices.
Customers search a route and date, pick a trip, hold seats for 10 minutes, pay (mock) and get a QR ticket by email.

v1 sells whole-route seats only. Seed data uses Japanese routes (JPY, Asia/Tokyo). Fares and timetables are made up.

## Local infrastructure

```
docker compose up -d
docker compose ps
```

| What | Where |
|---|---|
| Catalog DB | localhost:15433 (db `catalog`) |
| Seat DB | localhost:15434 (db `seats`) |
| Booking DB | localhost:15435 (db `booking`) |
| Payment DB | localhost:15436 (db `payment`) |
| RabbitMQ UI | http://localhost:15672 |
| Mailpit inbox | http://localhost:8025 |

All credentials are local-development values from `docker-compose.yml`.

## Services

| Service | Port | Status |
|---|---|---|
| gateway | 8080 | planned |
| catalog-service | 8081 | M1 |
| seat-service | 8082 | planned |
| booking-service | 8083 | planned |
| payment-service | 8084 | planned |
| notification-service | 8085 | planned |

## Catalog service (port 8081)

```
cd services/catalog-service
./mvnw spring-boot:run
```

At startup it applies the Flyway migrations and generates trips for the next 30 days.

| Endpoint | What it does |
|---|---|
| `GET /terminals` | all terminals |
| `GET /trips/search?from=Tokyo&to=Osaka&date=2026-10-12` | trips between two cities that depart on that date (Tokyo time) |
| `GET /trips/{id}` | one trip |
| `GET /layouts/{id}` | a bus layout with every seat |
| `POST /admin/trips/generate?days=14` | create any missing trips for the next N days |

Concepts: a route is a fixed path between two terminals, a schedule is a recurring timetable entry, and a trip is one real run on one date. Customers book trips.
