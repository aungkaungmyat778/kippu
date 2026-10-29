package dev.kippu.payment.api;

import dev.kippu.payment.api.dto.ErrorBody;
import dev.kippu.payment.service.IdempotencyKeyReusedException;
import dev.kippu.payment.service.InvalidPaymentRequestException;
import dev.kippu.payment.service.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<ErrorBody> invalid(InvalidPaymentRequestException e) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorBody> unreadable(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Malformed request (check UUIDs and JSON)");
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ResponseEntity<ErrorBody> reused(IdempotencyKeyReusedException e) {
        return body(HttpStatus.UNPROCESSABLE_CONTENT, "IDEMPOTENCY_KEY_REUSED", e.getMessage());
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorBody> notFound(PaymentNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", e.getMessage());
    }

    private static ResponseEntity<ErrorBody> body(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorBody(code, message));
    }
}
