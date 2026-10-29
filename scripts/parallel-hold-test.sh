#!/usr/bin/env bash
# 50 customers try to hold the SAME seat at the same moment. Exactly one must win.
#
# Needs: catalog-service (8081) and seat-service (8082) running, plus curl.
# Works in Git Bash on Windows, macOS and Linux.
# Usage:   scripts/parallel-hold-test.sh                 # picks a trip and a free seat for you
#          TRIP_ID=<uuid> SEAT=1A scripts/parallel-hold-test.sh
# A winning hold lasts 10 minutes. To run again right away, pick another SEAT or release it:
#          curl -X DELETE localhost:8082/bookings/<winning-bookingId>/holds
set -euo pipefail

CATALOG=${CATALOG:-http://localhost:8081}
SEATS=${SEATS:-http://localhost:8082}
PARALLEL=${PARALLEL:-50}

# Random UUID without uuidgen (Git Bash on Windows does not have it).
# od, tr and sed exist in Git Bash, macOS and Linux.
new_uuid() {
  od -An -tx1 -N16 /dev/urandom | tr -d ' \n' \
    | sed 's/\(........\)\(....\)\(....\)\(....\)\(............\)/\1-\2-\3-\4-\5/'
}
case "$(new_uuid)" in
  ????????-????-????-????-????????????) ;;
  *) echo "Could not generate a UUID on this machine."; exit 1 ;;
esac
export -f new_uuid

if [ -z "${TRIP_ID:-}" ]; then
  TOMORROW=$(date -d tomorrow +%F 2>/dev/null || date -v+1d +%F)
  TRIP_ID=$(curl -fsS "$CATALOG/trips/search?from=Tokyo&to=Osaka&date=$TOMORROW" \
            | grep -o '"id":"[0-9a-f-]\{36\}"' | head -1 | cut -d'"' -f4) || true
  [ -n "$TRIP_ID" ] || { echo "No trip found for $TOMORROW. Run: curl -X POST '$CATALOG/admin/trips/generate?days=14'"; exit 1; }
fi
echo "Trip:  $TRIP_ID"

if [ -z "${SEAT:-}" ]; then
  MAP=$(curl -fsS "$SEATS/trips/$TRIP_ID/seats")
  FREE=$(echo "$MAP" | grep -o '"seatNo":"[^"]*","row":[0-9]*,"column":"[^"]*","status":"AVAILABLE"' | cut -d'"' -f4) || true
  N=$(echo "$FREE" | grep -c . || true)
  [ "$N" -gt 0 ] || { echo "No free seats on this trip. Set TRIP_ID to another trip."; exit 1; }
  SEAT=$(echo "$FREE" | sed -n "$(( RANDOM % N + 1 ))p")
fi
echo "Seat:  $SEAT"
echo "Firing $PARALLEL requests at once, each with a different bookingId..."
echo

export SEATS TRIP_ID SEAT
RESULTS=$(seq 1 "$PARALLEL" | xargs -n1 -P "$PARALLEL" bash -c '
  ID=$(new_uuid)
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$SEATS/trips/$TRIP_ID/holds" \
       -H "Content-Type: application/json" \
       -d "{\"bookingId\":\"$ID\",\"seatNos\":[\"$SEAT\"]}" || true
' _ | sort | uniq -c) || true

echo "$RESULTS"
echo
WON=$(echo "$RESULTS" | awk '$2==201{print $1}'); WON=${WON:-0}
LOST=$(echo "$RESULTS" | awk '$2==409{print $1}'); LOST=${LOST:-0}
if [ "$WON" = "1" ] && [ "$LOST" = "$(( PARALLEL - 1 ))" ]; then
  echo "PASS: exactly 1 hold won, $LOST were refused with 409."
  exit 0
fi

echo "FAIL: expected 1 x 201 and $(( PARALLEL - 1 )) x 409."
if echo "$RESULTS" | awk '$2!=201 && $2!=409 {found=1} END{exit !found}'; then
  echo
  echo "Some responses were neither 201 nor 409. One more request, with the full response:"
  curl -s -w "\nHTTP %{http_code}\n" -X POST "$SEATS/trips/$TRIP_ID/holds" \
       -H "Content-Type: application/json" \
       -d "{\"bookingId\":\"$(new_uuid)\",\"seatNos\":[\"$SEAT\"]}" || true
fi
exit 1
