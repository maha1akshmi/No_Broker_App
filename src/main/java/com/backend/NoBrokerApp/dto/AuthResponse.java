package com.backend.NoBrokerApp.dto;

public record AuthResponse(
    String token,
    String role,
    String name,
    String email,
    Long userId
) {}
