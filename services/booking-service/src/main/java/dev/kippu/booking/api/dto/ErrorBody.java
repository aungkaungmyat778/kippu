package dev.kippu.booking.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/** bookingId is set when a FAILED booking was saved, so the customer can quote it to support. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorBody(String code, String message, UUID bookingId, List<String> seatNos) {

    public static ErrorBody of(String code, String message) {
        return new ErrorBody(code, message, null, null);
    }
}
