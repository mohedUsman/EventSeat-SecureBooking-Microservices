package com.eventseat.catalog.web;

import com.eventseat.catalog.repository.SeatJdbcRepository;
import com.eventseat.catalog.web.dto.SeatDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/seats")
@Validated
public class SeatController {

    private final SeatJdbcRepository repo;

    public SeatController(SeatJdbcRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<SeatDto> create(@Valid @RequestBody SeatDto dto) {
        SeatDto saved = repo.save(dto);
        return ResponseEntity
                .created(URI.create("/api/v1/seats/" + saved.getId()))
                .body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatDto> getById(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + id));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long eventId) {
        List<SeatDto> items = (eventId == null)
                ? repo.findAll(page, size)
                : repo.findAllByEventId(eventId, page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("page", Math.max(0, page));
        response.put("size", Math.max(1, size));
        response.put("items", items);
        response.put("count", items.size());
        if (eventId != null) {
            response.put("eventId", eventId);
        }
        return response;
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeatDto> update(@PathVariable Long id, @Valid @RequestBody SeatDto dto) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Seat not found: " + id);
        }
        int updated = repo.update(id, dto);
        if (updated == 0) {
            throw new ResourceNotFoundException("Seat not found: " + id);
        }
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found after update: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Seat not found: " + id);
        }
        repo.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
