package com.eventseat.catalog.web;

import com.eventseat.catalog.service.InventoryImportService;
import com.eventseat.catalog.web.dto.ImportDtos.ImportReport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/inventory")
@Validated
public class ImportController {

    private final InventoryImportService service;

    public ImportController(InventoryImportService service) {
        this.service = service;
    }

    // ORGANIZER/ADMIN via SecurityConfig: POST /api/v1/** requires organizer/admin
    // Accepts CSV or XLSX, returns a validation report; idempotent via
    // Idempotency-Key
    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportReport> importInventory(
            @RequestParam("eventId") Long eventId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file_required");
        }
        // Basic organizer/admin enforcement is at filter level. Optional ownership
        // check (future):
        // Long organizerId = getUid(jwt); verify event.organizerId == organizerId when
        // role=ORGANIZER

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file_read_error");
        }
        ImportReport report = service.importInventory(eventId, idempotencyKey, originalFilename, contentType, bytes);
        return ResponseEntity.status(HttpStatus.OK).body(report);
    }

    @SuppressWarnings("unused")
    private Long getUid(Jwt jwt) {
        if (jwt == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing_token");
        Object uid = jwt.getClaim("uid");
        if (uid instanceof Number n)
            return n.longValue();
        if (uid instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignore) {
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_uid_claim");
    }
}
