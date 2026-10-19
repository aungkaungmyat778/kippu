package dev.kippu.catalog.service;

import dev.kippu.catalog.api.dto.LayoutDto;
import dev.kippu.catalog.api.dto.TerminalDto;
import dev.kippu.catalog.api.dto.TripDto;
import dev.kippu.catalog.domain.TripStatus;
import dev.kippu.catalog.repository.BusLayoutRepository;
import dev.kippu.catalog.repository.TerminalRepository;
import dev.kippu.catalog.repository.TripRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only queries. Everything is turned into DTOs inside the transaction, because
 * spring.jpa.open-in-view is off: a lazy field touched after the method returns would fail.
 */
@Service
@Transactional(readOnly = true)
public class CatalogQueryService {

    private final TerminalRepository terminals;
    private final BusLayoutRepository layouts;
    private final TripRepository trips;
    private final Clock clock;
    private final ZoneId zone;

    public CatalogQueryService(TerminalRepository terminals, BusLayoutRepository layouts,
                               TripRepository trips, Clock clock, ZoneId zone) {
        this.terminals = terminals;
        this.layouts = layouts;
        this.trips = trips;
        this.clock = clock;
        this.zone = zone;
    }

    public List<TerminalDto> terminals() {
        return terminals.findAll(Sort.by("city", "name")).stream().map(TerminalDto::from).toList();
    }

    public LayoutDto layout(UUID id) {
        return layouts.findWithSeatsById(id)
                .map(LayoutDto::from)
                .orElseThrow(() -> new NotFoundException("Layout " + id + " not found"));
    }

    public TripDto trip(UUID id) {
        return trips.findDetailById(id)
                .map(trip -> TripDto.from(trip, zone))
                .orElseThrow(() -> new NotFoundException("Trip " + id + " not found"));
    }

    /** Trips from one city to another that depart on the given date in the service time zone. */
    public List<TripDto> searchTrips(String from, String to, LocalDate date) {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new InvalidRequestException("from and to are required");
        }
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        return trips.search(from.trim(), to.trim(), start, end, clock.instant(), TripStatus.SCHEDULED)
                .stream()
                .map(trip -> TripDto.from(trip, zone))
                .toList();
    }
}
