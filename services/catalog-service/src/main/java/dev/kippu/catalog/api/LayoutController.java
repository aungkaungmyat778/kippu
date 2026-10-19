package dev.kippu.catalog.api;

import dev.kippu.catalog.api.dto.LayoutDto;
import dev.kippu.catalog.service.CatalogQueryService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/layouts")
public class LayoutController {

    private final CatalogQueryService catalog;

    public LayoutController(CatalogQueryService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/{id}")
    public LayoutDto get(@PathVariable("id") UUID id) {
        return catalog.layout(id);
    }
}
