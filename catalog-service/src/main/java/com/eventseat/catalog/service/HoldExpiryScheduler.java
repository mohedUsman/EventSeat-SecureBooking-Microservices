package com.eventseat.catalog.service;

import com.eventseat.catalog.domain.HoldEntity;
import com.eventseat.catalog.repository.HoldJdbcRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically scans for expired ACTIVE holds and releases the seats.
 * Runs locally inside catalog-service (no external scheduler).
 */
@Component
public class HoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryScheduler.class);

    private final HoldJdbcRepository holdRepo;
    private final HoldService holdService;

    public HoldExpiryScheduler(HoldJdbcRepository holdRepo, HoldService holdService) {
        this.holdRepo = holdRepo;
        this.holdService = holdService;
    }

    // Run every 60 seconds; fixedDelay means next run starts 60s after previous
    // completes
    @Scheduled(fixedDelay = 60_000L, initialDelay = 20_000L)
    @Transactional // ensure each batch iteration has a txn boundary
    public void sweepExpiredHolds() {
        OffsetDateTime now = OffsetDateTime.now();
        List<HoldEntity> expired = holdRepo.findExpiredActiveHolds(now);
        if (expired.isEmpty()) {
            return;
        }
        log.info("Releasing {} expired holds", expired.size());
        for (HoldEntity e : expired) {
            try {
                holdService.expireAndRelease(e);
            } catch (Exception ex) {
                // Continue with others; log to console for local troubleshooting
                log.warn("Failed to expire/release hold id={}: {}", e.getId(), ex.getMessage());
            }
        }
    }
}
