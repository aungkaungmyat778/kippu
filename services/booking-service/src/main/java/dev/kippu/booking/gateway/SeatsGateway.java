package dev.kippu.booking.gateway;

import java.util.List;
import java.util.UUID;

/**
 * The Seats service as Booking sees it.
 * hold: throws SeatConflictException, UnknownSeatException, TripNotFoundException, DownstreamUnavailableException.
 * confirm: throws HoldExpiredException, DownstreamUnavailableException.
 * release: throws DownstreamUnavailableException. Safe to repeat.
 */
public interface SeatsGateway {

    void hold(UUID tripId, UUID bookingId, List<String> seatNos);

    void confirm(UUID bookingId);

    void release(UUID bookingId);
}
