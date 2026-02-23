package com.eventseat.order.web.dto;

import jakarta.validation.constraints.NotNull;

public class OrderStateUpdateRequest {

    @NotNull
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
