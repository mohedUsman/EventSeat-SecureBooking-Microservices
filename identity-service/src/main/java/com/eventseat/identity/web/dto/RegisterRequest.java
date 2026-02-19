package com.eventseat.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration payload.
 * Default role will be ATTENDEE unless provided explicitly.
 */
public class RegisterRequest {

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    @NotBlank
    @Size(min = 8, max = 120)
    private String password;

    // Optional: comma separated roles like "ATTENDEE" or "ATTENDEE,ORGANIZER"
    @Size(max = 120)
    private String rolesCsv;

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String password, String rolesCsv) {
        this.email = email;
        this.password = password;
        this.rolesCsv = rolesCsv;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRolesCsv() {
        return rolesCsv;
    }

    public void setRolesCsv(String rolesCsv) {
        this.rolesCsv = rolesCsv;
    }
}
