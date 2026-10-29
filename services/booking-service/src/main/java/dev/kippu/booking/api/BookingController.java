package dev.kippu.booking.api;

import dev.kippu.booking.api.dto.BookingDto;
import dev.kippu.booking.api.dto.CreateBookingRequest;
import dev.kippu.booking.service.BookingService;
import dev.kippu.booking.service.CreateBookingCommand;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    private final BookingService service;
    private final ZoneId zone;

    public BookingController(BookingService service, ZoneId displayZone) {
        this.service = service;
        this.zone = displayZone;
    }

    /** 201 with the CONFIRMED booking, or an error body (see ApiExceptionHandler) if the saga stopped. */
    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@RequestBody CreateBookingRequest r) {
        var cmd = new CreateBookingCommand(r.tripId(), r.seatNos(), r.customerName(),
                r.customerEmail(), r.paymentMethod());
        return BookingDto.from(service.createBooking(cmd), zone);
    }

    @GetMapping("/bookings/{id}")
    public BookingDto get(@PathVariable("id") UUID id) {
        return BookingDto.from(service.get(id), zone);
    }

    @GetMapping("/bookings/by-reference/{reference}")
    public BookingDto getByReference(@PathVariable("reference") String reference) {
        return BookingDto.from(service.getByReference(reference), zone);
    }
}
