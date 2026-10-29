#!/usr/bin/env bash
# 50 customers try to BOOK the same seat at the same moment, through the whole saga.
# Exactly one booking must be CONFIRMED (201) and the other 49 refused (409 SEAT_UNAVAILABLE).
#
# Needs: catalog (8081), seat (8082), booking (8083) and payment (8084) services running, plus curl.
# Works in Git Bash on Windows, macOS and Linux.
# Usage:   scripts/parallel-booking-test.sh
#          TRIP_ID=<uuid> SEAT=1A PARALLEL=100 scripts/parallel-booking-test.sh
# The winning seat stays SOLD. Run again and a different free seat is picked.
set -euo pipefail

CATALOG=${CATALOG:-http://localhost:8081}
SEATS=${SEATS:-http://localhost:8082}
BOOKING=${BOOKING:-http://localhost:8083}
PARALLEL=${PARALLEL:-50}

if [ -z "${TRIP_ID:-}" ]; then
  TOMORROW=$(date -d tomorrow +%F 2>/dev/null || date -v+1d +%F)
  TRIP_ID=$(curl -fsS "$CATALOG/trips/search?from=Tokyo&to=Osaka&date=$TOMORROW" \
            | grep -o '"id":"[0-9a-f-]\{36\}"' | head -1 | cut -d'"' -f4) || true
  [ -n "$TRIP_ID" ] || { echo "No trip found for $TOMORROW. Run: curl -X POST '$CATALOG/admin/trips/generate?days=14'"; exit 1; }
fi
echo "Trip:  $TRIP_ID"

if [ -z "${SEAT:-}" ]; then
  FREE=$(curl -fsS "$SEATS/trips/$TRIP_ID/seats" \
         | grep -o '"seatNo":"[^"]*","row":[0-9]*,"column":"[^"]*","status":"AVAILABLE"' | cut -d'"' -f4) || true
  N=$(echo "$FREE" | grep -c . || true)
  [ "$N" -gt 0 ] || { echo "No free seats on this trip. Set TRIP_ID to another trip."; exit 1; }
  SEAT=$(echo "$FREE" | sed -n "$(( RANDOM % N + 1 ))p")
fi
echo "Seat:  $SEAT"
echo "Firing $PARALLEL booking requests at once (this runs the full saga each time)..."
echo

export BOOKING TRIP_ID SEAT
RESULTS=$(seq 1 "$PARALLEL" | xargs -n1 -P "$PARALLEL" bash -c '
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BOOKING/bookings" \
       -H "Content-Type: application/json" \
       -d "{\"tripId\":\"$TRIP_ID\",\"seatNos\":[\"$SEAT\"],\"customerName\":\"Customer $1\",\"customerEmail\":\"customer$1@example.com\",\"paymentMethod\":\"mock-ok\"}" || true
' _ | sort | uniq -c) || true

echo "$RESULTS"
echo
WON=$(echo "$RESULTS" | awk '$2==201{print $1}'); WON=${WON:-0}
LOST=$(echo "$RESULTS" | awk '$2==409{print $1}'); LOST=${LOST:-0}
STATUS=$(curl -fsS "$SEATS/trips/$TRIP_ID/seats" \
  | grep -o "\"seatNo\":\"$SEAT\",\"row\":[0-9]*,\"column\":\"[^\"]*\",\"status\":\"[A-Z]*\"" \
  | sed 's/.*"status":"\([A-Z]*\)"/\1/' | head -1) || true

if [ "$WON" = "1" ] && [ "$LOST" = "$(( PARALLEL - 1 ))" ] && [ "$STATUS" = "SOLD" ]; then
  echo "PASS: exactly 1 booking confirmed, $LOST refused with 409, seat $SEAT is SOLD."
  exit 0
fi

echo "FAIL: expected 1 x 201, $(( PARALLEL - 1 )) x 409 and seat $SEAT SOLD (seat is: ${STATUS:-unknown})."
if echo "$RESULTS" | awk '$2!=201 && $2!=409 {found=1} END{exit !found}'; then
  echo
  echo "Some responses were neither 201 nor 409. One more request, with the full response:"
  curl -s -w "\nHTTP %{http_code}\n" -X POST "$BOOKING/bookings" -H "Content-Type: application/json" \
       -d "{\"tripId\":\"$TRIP_ID\",\"seatNos\":[\"$SEAT\"],\"customerName\":\"Customer X\",\"customerEmail\":\"x@example.com\",\"paymentMethod\":\"mock-ok\"}" || true
fi
exit 1
