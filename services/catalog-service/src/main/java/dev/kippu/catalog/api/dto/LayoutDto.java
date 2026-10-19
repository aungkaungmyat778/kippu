package dev.kippu.catalog.api.dto;

import dev.kippu.catalog.domain.BusLayout;
import java.util.List;
import java.util.UUID;

public record LayoutDto(UUID id, String code, String name, int rows, List<String> columns,
                        int aisleAfterColumn, int seatCount, List<SeatDto> seats) {

    public static LayoutDto from(BusLayout layout) {
        return new LayoutDto(
                layout.getId(),
                layout.getCode(),
                layout.getName(),
                layout.getRowCount(),
                layout.columns(),
                layout.getAisleAfter(),
                layout.seatCount(),
                layout.getSeats().stream().map(SeatDto::from).toList());
    }
}
