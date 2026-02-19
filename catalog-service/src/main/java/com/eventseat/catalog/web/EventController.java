package com.eventseat.catalog.web;

import com.eventseat.catalog.repository.EventJdbcRepository;
import com.eventseat.catalog.repository.EventSearchRepository;
import com.eventseat.catalog.web.dto.EventDto;
import com.eventseat.catalog.web.dto.EventSearchItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/events")
@Validated
public class EventController {

    private final EventJdbcRepository repo;
    private final EventSearchRepository searchRepo;

    public EventController(EventJdbcRepository repo, EventSearchRepository searchRepo) {
        this.repo = repo;
        this.searchRepo = searchRepo;
    }

    @PostMapping
    public ResponseEntity<EventDto> create(@Valid @RequestBody EventDto dto) {
        EventDto saved = repo.save(dto);
        return ResponseEntity
                .created(URI.create("/api/v1/events/" + saved.getId()))
                .body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getById(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<EventDto> items = repo.findAll(page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("page", Math.max(0, page));
        response.put("size", Math.max(1, size));
        response.put("items", items);
        response.put("count", items.size());
        return response;
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDto> update(@PathVariable Long id, @Valid @RequestBody EventDto dto) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Event not found: " + id);
        }
        int updated = repo.update(id, dto);
        if (updated == 0) {
            throw new ResourceNotFoundException("Event not found: " + id);
        }
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found after update: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Event not found: " + id);
        }
        repo.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String startFrom,
            @RequestParam(required = false) String endUntil) {

        OffsetDateTime start = parseOffsetDateTime(startFrom);
        OffsetDateTime end = parseOffsetDateTime(endUntil);

        List<EventSearchItem> items = searchRepo.search(
                Math.max(0, page),
                Math.max(1, size),
                city,
                category,
                organizerId,
                minPrice,
                maxPrice,
                start,
                end);

        Map<String, Object> response = new HashMap<>();
        response.put("page", Math.max(0, page));
        response.put("size", Math.max(1, size));
        response.put("items", items);
        response.put("count", items.size());

        Map<String, Object> filters = new HashMap<>();
        if (city != null)
            filters.put("city", city);
        if (category != null)
            filters.put("category", category);
        if (organizerId != null)
            filters.put("organizerId", organizerId);
        if (minPrice != null)
            filters.put("minPrice", minPrice);
        if (maxPrice != null)
            filters.put("maxPrice", maxPrice);
        if (startFrom != null)
            filters.put("startFrom", startFrom);
        if (endUntil != null)
            filters.put("endUntil", endUntil);
        response.put("filters", filters);

        return response;
    }

    private OffsetDateTime parseOffsetDateTime(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        try {
            return OffsetDateTime.parse(raw);
        } catch (Exception ex) {
            // Silently ignore parse errors and treat as null to keep API simple
            return null;
        }
    }
}
