package dev.kippu.catalog.api;

import dev.kippu.catalog.api.dto.TerminalDto;
import dev.kippu.catalog.service.CatalogQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/terminals")
public class TerminalController {

    private final CatalogQueryService catalog;

    public TerminalController(CatalogQueryService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<TerminalDto> list() {
        return catalog.terminals();
    }
}
