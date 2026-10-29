package dev.kippu.booking.gateway;

import java.time.Instant;
import java.util.UUID;

/** What Booking needs to know about a trip. Throws TripNotFoundException or DownstreamUnavailableException. */
public interface CatalogGateway {

    TripInfo getTrip(UUID tripId);

    /** status is the Catalog's text: SCHEDULED or CANCELLED. */
    record TripInfo(UUID id, String status, int fareYen, Instant departureAt) {
    }
}
