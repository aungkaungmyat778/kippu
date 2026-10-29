package dev.kippu.booking.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.kippu.booking.service.DownstreamUnavailableException;
import dev.kippu.booking.service.TripNotFoundException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestCatalogGateway implements CatalogGateway {

    private static final String SERVICE = "catalog-service";

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TripBody(UUID id, String status, int fareYen, OffsetDateTime departureAt) {
    }

    private final RestClient http;

    public RestCatalogGateway(@Value("${kippu.catalog.base-url}") String baseUrl) {
        // Built directly: in Spring Boot 4 the auto-configured RestClient.Builder is not on our classpath.
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public TripInfo getTrip(UUID tripId) {
        try {
            TripBody t = http.get().uri("/trips/{id}", tripId).retrieve().body(TripBody.class);
            if (t == null || t.departureAt() == null || t.status() == null) {
                throw new DownstreamUnavailableException(SERVICE, "incomplete trip response for " + tripId, null);
            }
            return new TripInfo(t.id(), t.status(), t.fareYen(), t.departureAt().toInstant());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new TripNotFoundException(tripId);
            }
            throw new DownstreamUnavailableException(SERVICE, "HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new DownstreamUnavailableException(SERVICE, "cannot reach the service", e);
        }
    }
}
