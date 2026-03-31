package com.backend.NoBrokerApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyFilterRequest {
    private String city;
    private String type;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer bedrooms;
    private Integer page = 0;
    private Integer size = 10;
}
