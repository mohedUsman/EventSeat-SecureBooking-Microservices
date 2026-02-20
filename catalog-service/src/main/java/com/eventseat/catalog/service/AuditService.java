package com.eventseat.catalog.service;

import com.eventseat.catalog.domain.AuditLogEntity;
import com.eventseat.catalog.repository.AuditLogRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void log(String action, String resourceType, Long resourceId, Jwt jwt, String details) {
        Long uid = null;
        String email = null;
        if (jwt != null) {
            Object uidClaim = jwt.getClaim("uid");
            if (uidClaim instanceof Number n) {
                uid = n.longValue();
            } else if (uidClaim instanceof String s) {
                try {
                    uid = Long.parseLong(s);
                } catch (NumberFormatException ignore) {
                    // leave uid null
                }
            }
            email = jwt.getClaimAsString("email");
        }
        AuditLogEntity e = new AuditLogEntity(action,
                uid == null ? -1L : uid,
                email == null ? "unknown" : email,
                resourceType,
                resourceId,
                details);
        repo.save(e);
    }

    @Transactional
    public void logEventPublishApproved(Long eventId, Jwt jwt) {
        log("EVENT_PUBLISH_APPROVED", "EVENT", eventId, jwt, "Event was approved for publishing.");
    }
}
