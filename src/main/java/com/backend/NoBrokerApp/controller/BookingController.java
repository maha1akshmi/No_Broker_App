package com.backend.NoBrokerApp.controller;

import com.backend.NoBrokerApp.dto.BookingRequest;
import com.backend.NoBrokerApp.dto.BookingResponse;
import com.backend.NoBrokerApp.exception.ApiResponse;
import com.backend.NoBrokerApp.model.Booking;
import com.backend.NoBrokerApp.security.UserPrincipal;
import com.backend.NoBrokerApp.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /api/bookings — Create a booking request (USER auth required)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        Booking booking = bookingService.createBooking(request, user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("id", booking.getId());
        data.put("status", booking.getStatus().name());

        return ResponseEntity.ok(ApiResponse.success("Booking request sent", data));
    }

    /**
     * GET /api/bookings/my — Get current user's bookings (USER auth required)
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyBookings(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        List<BookingResponse> bookings = bookingService.getMyBookings(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved", bookings));
    }
}
