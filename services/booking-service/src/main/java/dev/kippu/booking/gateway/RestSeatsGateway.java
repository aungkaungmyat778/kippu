package dev.kippu.booking.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.kippu.booking.service.DownstreamUnavailableException;
import dev.kippu.booking.service.HoldExpiredException;
import dev.kippu.booking.service.InvalidBookingRequestException;
import dev.kippu.booking.service.SeatConflictException;
import dev.kippu.booking.service.TripNotFoundException;
import dev.kippu.booking.service.UnknownSeatException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Talks to seat-service. The error codes below are the ones seat-service documents in docs/m2-seats.md. */
@Component
public class RestSeatsGateway implements SeatsGateway {

    private static final String SERVICE = "seat-service";

    record HoldBody(UUID bookingId, List<String> seatNos) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorBody(String code, String message, List<String> seatNos) {
    }

    private final RestClient http;

    public RestSeatsGateway(@Value("${kippu.seats.base-url}") String baseUrl) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void hold(UUID tripId, UUID bookingId, List<String> seatNos) {
        try {
            http.post().uri("/trips/{id}/holds", tripId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new HoldBody(bookingId, seatNos))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            ErrorBody err = errorOf(e);
            String code = err == null ? "" : String.valueOf(err.code());
            int status = e.getStatusCode().value();
            if (status == 409) {
                throw new SeatConflictException(err == null ? null : err.seatNos());
            }
            if (status == 400 && "UNKNOWN_SEAT".equals(code)) {
                throw new UnknownSeatException(err.seatNos());
            }
            if (status == 400) {
                throw new InvalidBookingRequestException(err == null ? "seat-service rejected the request" : err.message());
            }
            if (status == 404) {
                throw new TripNotFoundException(tripId);
            }
            throw new DownstreamUnavailableException(SERVICE, "HTTP " + status, e);
        } catch (RestClientException e) {
            throw new DownstreamUnavailableException(SERVICE, "cannot reach the service", e);
        }
    }

    @Override
    public void confirm(UUID bookingId) {
        try {
            http.post().uri("/bookings/{id}/confirm", bookingId).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409) {
                throw new HoldExpiredException(bookingId);
            }
            throw new DownstreamUnavailableException(SERVICE, "HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new DownstreamUnavailableException(SERVICE, "cannot reach the service", e);
        }
    }

    @Override
    public void release(UUID bookingId) {
        try {
            http.delete().uri("/bookings/{id}/holds", bookingId).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new DownstreamUnavailableException(SERVICE, "HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new DownstreamUnavailableException(SERVICE, "cannot reach the service", e);
        }
    }

    private static ErrorBody errorOf(RestClientResponseException e) {
        try {
            return e.getResponseBodyAs(ErrorBody.class);
        } catch (RuntimeException ignored) {
            return null; // an unreadable error body must not hide the real failure
        }
    }
}
