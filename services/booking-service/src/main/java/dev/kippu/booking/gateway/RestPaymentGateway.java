package dev.kippu.booking.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.kippu.booking.service.DownstreamUnavailableException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestPaymentGateway implements PaymentGateway {

    private static final String SERVICE = "payment-service";

    record ChargeBody(UUID bookingId, int amountYen, String method) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentBody(UUID paymentId, String status, String failureReason) {
    }

    private final RestClient http;

    public RestPaymentGateway(@Value("${kippu.payment.base-url}") String baseUrl) {
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public PaymentResult charge(UUID bookingId, String idempotencyKey, int amountYen, String method) {
        try {
            PaymentBody p = http.post().uri("/payments")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChargeBody(bookingId, amountYen, method))
                    .retrieve()
                    .body(PaymentBody.class);
            if (p == null || p.paymentId() == null || p.status() == null) {
                throw new DownstreamUnavailableException(SERVICE, "incomplete payment response", null);
            }
            return new PaymentResult(p.paymentId(), "SUCCEEDED".equals(p.status()), p.failureReason());
        } catch (RestClientResponseException e) {
            throw new DownstreamUnavailableException(SERVICE, "HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new DownstreamUnavailableException(SERVICE, "cannot reach the service", e);
        }
    }
}
