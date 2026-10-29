#!/usr/bin/env bash
# Walks the booking saga through its main outcomes and checks each one.
#
# Needs: catalog (8081), seat (8082), booking (8083) and payment (8084) services running, plus curl.
# Works in Git Bash on Windows, macOS and Linux.
# Usage:   scripts/booking-demo.sh              # picks a trip and free seats for you
#          TRIP_ID=<uuid> scripts/booking-demo.sh
# It books real seats (they stay SOLD). Run it against another trip, or reset the seat database,
# to see the happy path again. Seats that are already taken are skipped automatically.
set -euo pipefail

CATALOG=${CATALOG:-http://localhost:8081}
SEATS=${SEATS:-http://localhost:8082}
BOOKING=${BOOKING:-http://localhost:8083}

new_uuid() {
  od -An -tx1 -N16 /dev/urandom | tr -d ' \n' \
    | sed 's/\(........\)\(....\)\(....\)\(....\)\(............\)/\1-\2-\3-\4-\5/'
}

PASSED=0; FAILED=0
check() { # label, ok(true|false)
  if [ "$2" = "true" ]; then PASSED=$((PASSED+1)); echo "  PASS  $1"; else FAILED=$((FAILED+1)); echo "  FAIL  $1"; fi
}
field() { # JSON field with a string value, from $BODY
  echo "$BODY" | grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4 || true
}
call() { # method, path, [json body] -> sets HTTP_CODE and BODY
  local out
  out=$(curl -s -w '\n%{http_code}' -X "$1" "$BOOKING$2" -H 'Content-Type: application/json' ${3:+-d "$3"}) || true
  HTTP_CODE=${out##*$'\n'}
  BODY=${out%$'\n'*}
}
book() { # seats-json, payment method -> POST /bookings
  call POST /bookings "{\"tripId\":\"$TRIP_ID\",\"seatNos\":$1,\"customerName\":\"Demo Customer\",\"customerEmail\":\"demo@example.com\",\"paymentMethod\":\"$2\"}"
}
seat_status() { # seatNo -> AVAILABLE | HELD | SOLD
  curl -fsS "$SEATS/trips/$TRIP_ID/seats" \
    | grep -o "\"seatNo\":\"$1\",\"row\":[0-9]*,\"column\":\"[^\"]*\",\"status\":\"[A-Z]*\"" \
    | sed 's/.*"status":"\([A-Z]*\)"/\1/' | head -1
}

if [ -z "${TRIP_ID:-}" ]; then
  TOMORROW=$(date -d tomorrow +%F 2>/dev/null || date -v+1d +%F)
  TRIP_ID=$(curl -fsS "$CATALOG/trips/search?from=Tokyo&to=Osaka&date=$TOMORROW" \
            | grep -o '"id":"[0-9a-f-]\{36\}"' | head -1 | cut -d'"' -f4) || true
  [ -n "$TRIP_ID" ] || { echo "No trip found for $TOMORROW. Run: curl -X POST '$CATALOG/admin/trips/generate?days=14'"; exit 1; }
fi
FREE=$(curl -fsS "$SEATS/trips/$TRIP_ID/seats" \
       | grep -o '"seatNo":"[^"]*","row":[0-9]*,"column":"[^"]*","status":"AVAILABLE"' | cut -d'"' -f4) || true
[ "$(echo "$FREE" | grep -c .)" -ge 3 ] || { echo "Need at least 3 free seats on trip $TRIP_ID. Set TRIP_ID to another trip."; exit 1; }
S1=$(echo "$FREE" | sed -n 1p); S2=$(echo "$FREE" | sed -n 2p); S3=$(echo "$FREE" | sed -n 3p)
echo "Trip $TRIP_ID, using seats $S1, $S2 and $S3"

echo
echo "1. Happy path: book $S1 and $S2 with a card that works"
book "[\"$S1\",\"$S2\"]" mock-ok
check "answered 201 Created (got $HTTP_CODE)" "$([ "$HTTP_CODE" = 201 ] && echo true || echo false)"
check "status CONFIRMED (got $(field status))" "$([ "$(field status)" = CONFIRMED ] && echo true || echo false)"
REF=$(field reference); BID=$(field id)
echo "        reference $REF, total $(echo "$BODY" | grep -o '"totalYen":[0-9]*' | cut -d: -f2) yen"
call GET "/bookings/by-reference/$REF"
check "found again by reference (HTTP $HTTP_CODE)" "$([ "$HTTP_CODE" = 200 ] && [ "$(field status)" = CONFIRMED ] && echo true || echo false)"
check "seat $S1 is now SOLD (got $(seat_status "$S1"))" "$([ "$(seat_status "$S1")" = SOLD ] && echo true || echo false)"

echo
echo "2. Seat conflict: someone else tries $S1 again"
book "[\"$S1\"]" mock-ok
check "answered 409 Conflict (got $HTTP_CODE)" "$([ "$HTTP_CODE" = 409 ] && echo true || echo false)"
check "code SEAT_UNAVAILABLE (got $(field code))" "$([ "$(field code)" = SEAT_UNAVAILABLE ] && echo true || echo false)"
FID=$(field bookingId)
call GET "/bookings/$FID"
check "the attempt is kept as FAILED / SEAT_UNAVAILABLE" "$([ "$(field status)" = FAILED ] && [ "$(field failureReason)" = SEAT_UNAVAILABLE ] && echo true || echo false)"

echo
echo "3. Declined card: book $S3 with a card that is refused"
book "[\"$S3\"]" mock-decline
check "answered 402 Payment Required (got $HTTP_CODE)" "$([ "$HTTP_CODE" = 402 ] && echo true || echo false)"
check "code PAYMENT_DECLINED (got $(field code))" "$([ "$(field code)" = PAYMENT_DECLINED ] && echo true || echo false)"
check "seat $S3 was given back (got $(seat_status "$S3"))" "$([ "$(seat_status "$S3")" = AVAILABLE ] && echo true || echo false)"

echo
echo "4. Requests that should be refused before anything happens"
book "[\"1A\",\"1B\",\"1C\",\"1D\",\"2A\"]" mock-ok
check "5 seats -> 400 (got $HTTP_CODE)" "$([ "$HTTP_CODE" = 400 ] && echo true || echo false)"
book "[\"$S3\"]" bitcoin
check "unknown payment method -> 400 (got $HTTP_CODE)" "$([ "$HTTP_CODE" = 400 ] && echo true || echo false)"
SAVED_TRIP=$TRIP_ID; TRIP_ID=$(new_uuid)
book "[\"1A\"]" mock-ok
check "unknown trip -> 404 (got $HTTP_CODE)" "$([ "$HTTP_CODE" = 404 ] && echo true || echo false)"
TRIP_ID=$SAVED_TRIP
book "[\"99Z\"]" mock-ok
check "seat that does not exist -> 400 UNKNOWN_SEAT (got $HTTP_CODE $(field code))" "$([ "$HTTP_CODE" = 400 ] && [ "$(field code)" = UNKNOWN_SEAT ] && echo true || echo false)"

echo
echo "$PASSED checks passed, $FAILED failed."
[ "$FAILED" = 0 ]
