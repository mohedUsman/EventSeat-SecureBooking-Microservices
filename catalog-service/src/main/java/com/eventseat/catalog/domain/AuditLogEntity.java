package com.eventseat.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String action; // e.g., EVENT_PUBLISH_APPROVED

    @Column(nullable = false)
    private Long actorUserId;

    @Column(nullable = false, length = 180)
    private String actorEmail;

    @Column(nullable = false, length = 64)
    private String resourceType; // e.g., EVENT

    @Column(nullable = false)
    private Long resourceId;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public AuditLogEntity() {
    }

    public AuditLogEntity(String action, Long actorUserId, String actorEmail,
            String resourceType, Long resourceId, String details) {
        this.action = action;
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.details = details;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
