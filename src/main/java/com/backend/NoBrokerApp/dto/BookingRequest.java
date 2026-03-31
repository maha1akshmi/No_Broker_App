package com.backend.NoBrokerApp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull(message = "Property ID is required")
    private Long propertyId;

    private String bookingType;

    private LocalDateTime preferredDate;

    private String message;
}
