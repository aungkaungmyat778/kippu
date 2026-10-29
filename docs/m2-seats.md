# M2: Seats service

Port **8082**, database `seats` on **15434**. Owns the seat map of every trip: which seat is
free, held for a booking, or sold. The Catalog service owns trips and layouts; Seats asks it
for the layout once, the first time a trip's seats are needed.

## Seat states

```
AVAILABLE --hold--> HELD --confirm--> SOLD
              ^       |
              |  release / hold runs out
              +-------+
```

A hold lasts 10 minutes (`kippu.seats.hold-minutes`). A hold that has run out already counts as
available, even before the cleanup job (every 30 s) tidies the row.

## API

| Call | Meaning | Success | Failure |
|---|---|---|---|
| `GET /trips/{tripId}/seats` | Seat map with counts. Creates the seats on first use. | 200 | 404 trip unknown, 503 Catalog down |
| `POST /trips/{tripId}/holds` `{"bookingId":"…","seatNos":["1A","1B"]}` | Hold 1 to 4 seats for a booking, all or nothing. | 201 | **409 `SEAT_UNAVAILABLE`** (lists the seats), 400 bad input / unknown seat |
| `POST /bookings/{bookingId}/confirm` | Held seats become sold. Safe to repeat. | 200 | **409 `HOLD_EXPIRED`** |
| `DELETE /bookings/{bookingId}/holds` | Free the booking's held seats. Never touches sold seats. Safe to repeat. | 200 `{"released":n}` | |
| `POST /admin/holds/sweep` | Dev helper: run the cleanup now. | 200 | |

Errors look like `{"code":"SEAT_UNAVAILABLE","message":"…","seatNos":["1B"]}`.

## Why it is built this way

- **One SQL statement decides.** `hold` is a single `UPDATE … WHERE id IN (SELECT … FOR UPDATE)`.
  There is no "read the seat, then write it" gap for a second request to slip into, so 50 requests
  for one seat produce exactly one winner without any Java-side locking.
- **Same lock order everywhere** (`ORDER BY trip_id, seat_no`). Two bookings that want the same two
  seats in opposite order wait for each other instead of deadlocking.
- **All or nothing.** If the UPDATE took fewer seats than requested, the service throws and the
  transaction rolls back, so nobody is left holding half a booking.
- **Retries are safe.** Holding again with the same `bookingId` succeeds and extends the hold.
  Confirm and release can be repeated.
- **The database refuses impossible states.** CHECK constraints: AVAILABLE has no booking or expiry,
  HELD has both, SOLD has a booking and no expiry.
- **No HTTP call inside a transaction.** Calling Catalog while row locks are open would let a slow
  Catalog freeze the seat map.

## Run it

1. `docker compose up -d` (the `seats` database is on 15434)
2. Terminal 1: `cd services/catalog-service && ./mvnw spring-boot:run`
3. Terminal 2: `cd services/seat-service && ./mvnw spring-boot:run`
4. Look at a trip:
   ```
   TRIP=$(curl -s "localhost:8081/trips/search?from=Tokyo&to=Osaka&date=$(date -v+1d +%F)" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
   curl -s localhost:8082/trips/$TRIP/seats
   ```
5. Hold, confirm and release by hand:
   ```
   B=$(uuidgen | tr A-Z a-z)
   curl -s -X POST localhost:8082/trips/$TRIP/holds -H 'Content-Type: application/json' -d "{\"bookingId\":\"$B\",\"seatNos\":[\"1A\",\"1B\"]}"
   curl -s -X POST localhost:8082/bookings/$B/confirm
   ```
6. The demo: `scripts/parallel-hold-test.sh`. Expect `1 x 201` and `49 x 409`.

## Known gaps (planned)

- Seats are created by pulling from Catalog on first use. **M5** replaces this with a `TripCreated` event.
- The cleanup job does not publish `SeatHoldExpired` yet. **M5** adds it (needs `UPDATE … RETURNING booking_id`).
- No HTTP timeouts, retries or circuit breaker on the Catalog call. **M6** (hardening).
- Seats does not check that a trip is still bookable (cancelled, departed). Booking enforces the cut-off in **M3**.
