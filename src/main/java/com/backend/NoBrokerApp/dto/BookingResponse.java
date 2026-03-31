package com.backend.NoBrokerApp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private PropertySummary property;
    private String bookingType;
    private String status;
    private LocalDateTime preferredDate;
    private String message;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertySummary {
        private Long id;
        private String title;
        private String city;
    }
}
