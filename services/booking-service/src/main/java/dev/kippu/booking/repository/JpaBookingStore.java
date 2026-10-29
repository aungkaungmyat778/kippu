package dev.kippu.booking.repository;

import dev.kippu.booking.domain.Booking;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaBookingStore implements BookingStore {

    private final BookingRepository repository;

    public JpaBookingStore(BookingRepository repository) {
        this.repository = repository;
    }

    @Override
    public Booking save(Booking booking) {
        return repository.save(booking);
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Booking> findByReference(String reference) {
        return repository.findByReference(reference);
    }
}
