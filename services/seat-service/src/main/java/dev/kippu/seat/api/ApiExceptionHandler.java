package dev.kippu.seat.api;

import dev.kippu.seat.api.dto.ErrorBody;
import dev.kippu.seat.service.CatalogUnavailableException;
import dev.kippu.seat.service.HoldExpiredException;
import dev.kippu.seat.service.InvalidHoldRequestException;
import dev.kippu.seat.service.SeatConflictException;
import dev.kippu.seat.service.TripNotFoundException;
import dev.kippu.seat.service.UnknownSeatException;
import java.util.List;
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

    @ExceptionHandler(SeatConflictException.class)
    public ResponseEntity<ErrorBody> conflict(SeatConflictException e) {
        return body(HttpStatus.CONFLICT, "SEAT_UNAVAILABLE", e.getMessage(), e.getSeatNos());
    }

    @ExceptionHandler(HoldExpiredException.class)
    public ResponseEntity<ErrorBody> expired(HoldExpiredException e) {
        return body(HttpStatus.CONFLICT, "HOLD_EXPIRED", e.getMessage(), List.of());
    }

    @ExceptionHandler(UnknownSeatException.class)
    public ResponseEntity<ErrorBody> unknownSeat(UnknownSeatException e) {
        return body(HttpStatus.BAD_REQUEST, "UNKNOWN_SEAT", e.getMessage(), e.getSeatNos());
    }

    @ExceptionHandler(InvalidHoldRequestException.class)
    public ResponseEntity<ErrorBody> invalid(InvalidHoldRequestException e) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage(), List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorBody> unreadable(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Malformed request (check UUIDs and JSON)", List.of());
    }

    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<ErrorBody> tripNotFound(TripNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, "TRIP_NOT_FOUND", e.getMessage(), List.of());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public ResponseEntity<ErrorBody> catalogDown(CatalogUnavailableException e) {
        log.warn("Catalog problem: {}", e.getMessage(), e);
        return body(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE", e.getMessage(), List.of());
    }

    private static ResponseEntity<ErrorBody> body(HttpStatus status, String code, String message, List<String> seatNos) {
        return ResponseEntity.status(status).body(new ErrorBody(code, message, seatNos));
    }
}
