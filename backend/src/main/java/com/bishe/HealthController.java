package com.bishe;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class HealthController {
    private final AtomicBoolean ready = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    void ready() {
        ready.set(true);
    }

    @GetMapping("/api/health")
    Map<String, String> health() {
        if (!ready.get()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Starting");
        }
        return Map.of("status", "UP");
    }
}
