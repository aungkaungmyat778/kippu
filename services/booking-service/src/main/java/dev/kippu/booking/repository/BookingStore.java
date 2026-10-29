package dev.kippu.booking.repository;

import dev.kippu.booking.domain.Booking;
import java.util.Optional;
import java.util.UUID;

/**
 * The small slice of persistence the saga needs. Having it as an interface lets the saga be
 * tested with an in-memory fake instead of a database.
 */
public interface BookingStore {

    /** Inserts or updates, and returns the saved booking. Use the returned object for later steps. */
    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByReference(String reference);
}
