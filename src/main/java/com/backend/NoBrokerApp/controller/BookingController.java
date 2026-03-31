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

    /**
     * GET /api/bookings/owner — Get bookings on properties owned by current user
     */
    @GetMapping("/owner")
    public ResponseEntity<ApiResponse<?>> getOwnerBookings(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        List<BookingResponse> bookings = bookingService.getBookingsForOwner(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Owner bookings retrieved", bookings));
    }

    /**
     * PATCH /api/bookings/{id}/confirm — Property owner confirms a pending booking
     */
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<?>> confirmBooking(
            @PathVariable Long id,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        bookingService.confirmBooking(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed. Confirmation email sent to the user.", null));
    }

    /**
     * PATCH /api/bookings/{id}/reject — Property owner rejects a pending booking
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<?>> rejectBooking(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        String reason = (body != null) ? body.get("reason") : "No reason provided";
        bookingService.rejectBooking(id, user.getId(), reason);
        return ResponseEntity.ok(ApiResponse.success("Booking rejected. The user has been notified.", null));
    }

    /**
     * PUT /api/bookings/{id}/cancel — Cancel a booking (USER auth required)
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<?>> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        bookingService.cancelBooking(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", null));
    }
}
