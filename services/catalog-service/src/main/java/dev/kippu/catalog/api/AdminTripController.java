package dev.kippu.catalog.api;

import dev.kippu.catalog.api.dto.GenerationResult;
import dev.kippu.catalog.service.TripGenerationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** No login yet. The gateway will keep /admin private later. */
@RestController
@RequestMapping("/admin/trips")
public class AdminTripController {

    private final TripGenerationService generation;
    private final int defaultHorizonDays;

    public AdminTripController(TripGenerationService generation,
                               @Value("${kippu.generation.horizon-days}") int defaultHorizonDays) {
        this.generation = generation;
        this.defaultHorizonDays = defaultHorizonDays;
    }

    /** Example: POST /admin/trips/generate?days=14 */
    @PostMapping("/generate")
    public GenerationResult generate(@RequestParam(name = "days", required = false) Integer days) {
        int horizon = days != null ? days : defaultHorizonDays;
        return new GenerationResult(generation.generate(horizon), horizon);
    }
}
