package dev.kippu.booking.api;

import dev.kippu.booking.api.dto.ErrorBody;
import dev.kippu.booking.domain.FailureReason;
import dev.kippu.booking.service.BookingFailedException;
import dev.kippu.booking.service.BookingNotAllowedException;
import dev.kippu.booking.service.BookingNotFoundException;
import dev.kippu.booking.service.DownstreamUnavailableException;
import dev.kippu.booking.service.InvalidBookingRequestException;
import dev.kippu.booking.service.TripNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BookingFailedException.class)
    ResponseEntity<ErrorBody> failed(BookingFailedException e) {
        return ResponseEntity.status(statusFor(e.getCode()))
                .body(new ErrorBody(e.getCode(), e.getMessage(), e.getBookingId(),
                        e.getSeatNos().isEmpty() ? null : e.getSeatNos()));
    }

    /** What each saga failure means to the caller. */
    static HttpStatus statusFor(String code) {
        return switch (code) {
            case FailureReason.SEAT_UNAVAILABLE -> HttpStatus.CONFLICT;                 // 409
            case FailureReason.UNKNOWN_SEAT -> HttpStatus.BAD_REQUEST;                  // 400
            case FailureReason.PAYMENT_DECLINED -> HttpStatus.PAYMENT_REQUIRED;         // 402
            case FailureReason.SEAT_SERVICE_UNAVAILABLE,
                 FailureReason.PAYMENT_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;   // 503
            default -> HttpStatus.INTERNAL_SERVER_ERROR;                                // 500, e.g. PAID_BUT_NOT_CONFIRMED
        };
    }

    @ExceptionHandler(BookingNotAllowedException.class)
    ResponseEntity<ErrorBody> notAllowed(BookingNotAllowedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorBody.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidBookingRequestException.class)
    ResponseEntity<ErrorBody> invalid(InvalidBookingRequestException e) {
        return ResponseEntity.badRequest().body(ErrorBody.of("INVALID_REQUEST", e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorBody> unreadable(Exception e) {
        return ResponseEntity.badRequest().body(ErrorBody.of("INVALID_REQUEST", "Request could not be read"));
    }

    @ExceptionHandler(TripNotFoundException.class)
    ResponseEntity<ErrorBody> tripNotFound(TripNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorBody.of("TRIP_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    ResponseEntity<ErrorBody> bookingNotFound(BookingNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorBody.of("BOOKING_NOT_FOUND", e.getMessage()));
    }

    /** Only reached when Catalog is down: nothing was saved yet, so there is no booking to point at. */
    @ExceptionHandler(DownstreamUnavailableException.class)
    ResponseEntity<ErrorBody> downstream(DownstreamUnavailableException e) {
        log.warn("Downstream unavailable: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorBody.of("SERVICE_UNAVAILABLE", "A required service is temporarily unavailable, please try again"));
    }
}
