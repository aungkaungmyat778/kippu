package dev.kippu.catalog.repository;

import dev.kippu.catalog.domain.Terminal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalRepository extends JpaRepository<Terminal, UUID> {
}
