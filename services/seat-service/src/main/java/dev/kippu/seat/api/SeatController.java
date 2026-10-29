package dev.kippu.seat.api;

import dev.kippu.seat.api.dto.ConfirmResponse;
import dev.kippu.seat.api.dto.HoldRequest;
import dev.kippu.seat.api.dto.HoldResponse;
import dev.kippu.seat.api.dto.SeatMapDto;
import dev.kippu.seat.service.SeatService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    /** The seat map for a trip. Expired holds already show as AVAILABLE. */
    @GetMapping("/trips/{tripId}/seats")
    public SeatMapDto seatMap(@PathVariable("tripId") UUID tripId) {
        return seatService.seatMap(tripId);
    }

    /** Hold 1..4 seats for a booking. 201 on success, 409 if any seat is taken. */
    @PostMapping("/trips/{tripId}/holds")
    public ResponseEntity<HoldResponse> hold(@PathVariable("tripId") UUID tripId,
                                             @RequestBody HoldRequest body) {
        HoldResponse response = seatService.hold(tripId, body.bookingId(), body.seatNos());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Called by Booking after payment succeeds. 409 if the hold ran out. */
    @PostMapping("/bookings/{bookingId}/confirm")
    public ConfirmResponse confirm(@PathVariable("bookingId") UUID bookingId) {
        return seatService.confirm(bookingId);
    }

    /** Called by Booking when payment fails or the customer abandons. Repeating it is harmless. */
    @DeleteMapping("/bookings/{bookingId}/holds")
    public Map<String, Integer> release(@PathVariable("bookingId") UUID bookingId) {
        return Map.of("released", seatService.release(bookingId));
    }

    /** Dev helper: run the expiry cleanup now instead of waiting for the job. */
    @PostMapping("/admin/holds/sweep")
    public Map<String, Integer> sweep() {
        return Map.of("released", seatService.sweepExpired());
    }
}
