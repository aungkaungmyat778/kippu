package dev.kippu.payment.api;

import dev.kippu.payment.api.dto.ChargeRequest;
import dev.kippu.payment.api.dto.PaymentDto;
import dev.kippu.payment.service.PaymentService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Internal API: only the Booking service calls this. It is not exposed through the gateway. */
@RestController
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /**
     * 201 for a new payment, 200 when the Idempotency-Key was seen before (same result as the
     * first time, header Idempotent-Replayed: true). A declined card is a normal business result,
     * so it is answered with status DECLINED in the body, not an HTTP error.
     */
    @PostMapping("/payments")
    public ResponseEntity<PaymentDto> charge(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ChargeRequest body) {
        PaymentService.ChargeResult result =
                service.charge(idempotencyKey, body.bookingId(), body.amountYen(), body.method());
        return ResponseEntity
                .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                .body(PaymentDto.from(result.payment()));
    }

    @GetMapping("/payments/{id}")
    public PaymentDto get(@PathVariable("id") UUID id) {
        return PaymentDto.from(service.get(id));
    }
}
