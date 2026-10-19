package dev.kippu.catalog.api.dto;

import dev.kippu.catalog.domain.Terminal;
import java.util.UUID;

public record TerminalDto(UUID id, String code, String name, String city) {

    public static TerminalDto from(Terminal terminal) {
        return new TerminalDto(terminal.getId(), terminal.getCode(), terminal.getName(), terminal.getCity());
    }
}
