package com.eventseat.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

    public enum Status {
        ACTIVE, INACTIVE, LOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    // Comma-separated roles: ATTENDEE,ORGANIZER,ADMIN
    @Column(nullable = false, length = 120)
    private String rolesCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    public UserEntity() {
    }

    public UserEntity(Long id, String email, String passwordHash, String rolesCsv, Status status) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rolesCsv = rolesCsv;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRolesCsv() {
        return rolesCsv;
    }

    public void setRolesCsv(String rolesCsv) {
        this.rolesCsv = rolesCsv;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
