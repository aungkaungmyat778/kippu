package dev.kippu.catalog.api.dto;

import dev.kippu.catalog.domain.Trip;
import dev.kippu.catalog.domain.TripStatus;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** What the API returns for a trip. Times are shown in the service time zone, for example +09:00. */
public record TripDto(UUID id, TerminalDto origin, TerminalDto destination,
                      OffsetDateTime departureAt, OffsetDateTime arrivalAt, int durationMinutes,
                      int fareYen, String currency, String busType, UUID layoutId, int seatCount,
                      TripStatus status) {

    public static TripDto from(Trip trip, ZoneId zone) {
        return new TripDto(
                trip.getId(),
                TerminalDto.from(trip.getRoute().getOrigin()),
                TerminalDto.from(trip.getRoute().getDestination()),
                trip.getDepartureAt().atZone(zone).toOffsetDateTime(),
                trip.getArrivalAt().atZone(zone).toOffsetDateTime(),
                trip.getRoute().getDurationMinutes(),
                trip.getFareYen(),
                "JPY",
                trip.getLayout().getName(),
                trip.getLayout().getId(),
                trip.getLayout().seatCount(),
                trip.getStatus());
    }
}
