package dev.kippu.booking.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.kippu.booking.domain.Booking;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingDto(UUID id, String reference, UUID tripId, String status, String failureReason,
                         List<String> seatNos, int fareYen, int totalYen, String currency,
                         String customerName, String customerEmail, OffsetDateTime departureAt,
                         UUID paymentId, Instant createdAt) {

    public static BookingDto from(Booking b, ZoneId zone) {
        return new BookingDto(b.getId(), b.getReference(), b.getTripId(), b.getStatus().name(),
                b.getFailureReason(), List.copyOf(b.getSeatNos()), b.getFareYen(), b.getTotalYen(), "JPY",
                b.getCustomerName(), b.getCustomerEmail(), b.getDepartureAt().atZone(zone).toOffsetDateTime(),
                b.getPaymentId(), b.getCreatedAt());
    }
}
