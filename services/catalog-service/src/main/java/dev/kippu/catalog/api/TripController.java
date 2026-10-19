package dev.kippu.catalog.api;

import dev.kippu.catalog.api.dto.TripDto;
import dev.kippu.catalog.service.CatalogQueryService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final CatalogQueryService catalog;

    public TripController(CatalogQueryService catalog) {
        this.catalog = catalog;
    }

    /** Example: GET /trips/search?from=Tokyo&to=Osaka&date=2026-10-12 */
    @GetMapping("/search")
    public List<TripDto> search(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return catalog.searchTrips(from, to, date);
    }

    @GetMapping("/{id}")
    public TripDto get(@PathVariable("id") UUID id) {
        return catalog.trip(id);
    }
}
