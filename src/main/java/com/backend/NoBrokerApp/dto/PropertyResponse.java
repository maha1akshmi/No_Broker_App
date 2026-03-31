package com.backend.NoBrokerApp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponse {

    private Long id;
    private String title;
    private String description;
    private String type;
    private BigDecimal price;
    private String location;
    private String city;
    private String state;
    private String pincode;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal areaSqft;
    private Boolean isFurnished;
    private String status;
    private String rejectionReason;
    private List<String> images;
    private OwnerInfo owner;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfo {
        private String name;
        private String phone;
        private String email;
    }
}
