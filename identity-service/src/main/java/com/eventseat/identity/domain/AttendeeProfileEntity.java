package com.eventseat.identity.domain;

import com.eventseat.identity.jpa.AesGcmStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Attendee PII profile stored encrypted at rest using AES-GCM.
 * name, phone, address are converted via AesGcmStringConverter.
 *
 * Relationship: one profile per user (userId is unique).
 */
@Entity
@Table(name = "attendee_profiles")
public class AttendeeProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "name_enc", length = 2048)
    private String name;

    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "phone_enc", length = 2048)
    private String phone;

    @Convert(converter = AesGcmStringConverter.class)
    @Column(name = "address_enc", length = 4096)
    private String address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Masked toString to avoid leaking PII into logs.
     */
    @Override
    public String toString() {
        return "AttendeeProfileEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", name=" + mask(name) +
                ", phone=" + maskPhone(phone) +
                ", address=" + mask(address) +
                '}';
    }

    private String mask(String s) {
        if (s == null || s.isBlank())
            return "****";
        int show = Math.min(2, s.length());
        return s.substring(0, show) + "****";
    }

    private String maskPhone(String p) {
        if (p == null || p.isBlank())
            return "****";
        String digits = p.replaceAll("\\D", "");
        if (digits.length() <= 4)
            return "****";
        return "****" + digits.substring(digits.length() - 4);
    }
}
