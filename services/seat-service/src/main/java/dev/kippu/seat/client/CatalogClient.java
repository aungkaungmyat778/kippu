package dev.kippu.seat.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.kippu.seat.service.CatalogUnavailableException;
import dev.kippu.seat.service.TripNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Asks the Catalog service which seats a trip has. Only the fields we need are listed;
 * unknown fields in the Catalog response are ignored.
 *
 * TODO (M6 hardening): connect/read timeouts, retries and a circuit breaker.
 * TODO (M5): replace this pull with a TripCreated event.
 */
@Component
public class CatalogClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TripInfo(UUID id, UUID layoutId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LayoutSeatInfo(String seatNo, int row, String column) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LayoutInfo(List<LayoutSeatInfo> seats) {
    }

    private final RestClient http;

    public CatalogClient(@Value("${kippu.catalog.base-url}") String baseUrl) {
        // Built directly instead of injecting RestClient.Builder: in Spring Boot 4 the
        // auto-configured builder lives in the separate spring-boot-restclient module,
        // which is not on this service's classpath (that was the startup error).
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    public TripInfo getTrip(UUID tripId) {
        try {
            TripInfo trip = http.get().uri("/trips/{id}", tripId).retrieve().body(TripInfo.class);
            if (trip == null || trip.layoutId() == null) {
                throw new CatalogUnavailableException("Catalog returned no layoutId for trip " + tripId, null);
            }
            return trip;
        } catch (HttpClientErrorException.NotFound e) {
            throw new TripNotFoundException(tripId);
        } catch (RestClientException e) {
            throw new CatalogUnavailableException("Cannot reach Catalog for trip " + tripId, e);
        }
    }

    public LayoutInfo getLayout(UUID layoutId) {
        try {
            LayoutInfo layout = http.get().uri("/layouts/{id}", layoutId).retrieve().body(LayoutInfo.class);
            if (layout == null || layout.seats() == null || layout.seats().isEmpty()) {
                throw new CatalogUnavailableException("Catalog returned no seats for layout " + layoutId, null);
            }
            return layout;
        } catch (RestClientException e) {
            throw new CatalogUnavailableException("Cannot reach Catalog for layout " + layoutId, e);
        }
    }
}
