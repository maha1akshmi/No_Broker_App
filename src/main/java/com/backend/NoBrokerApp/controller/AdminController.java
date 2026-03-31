package com.backend.NoBrokerApp.controller;

import com.backend.NoBrokerApp.dto.PropertyResponse;
import com.backend.NoBrokerApp.exception.ApiResponse;
import com.backend.NoBrokerApp.model.Booking;
import com.backend.NoBrokerApp.model.Property;
import com.backend.NoBrokerApp.model.User;
import com.backend.NoBrokerApp.repository.BookingRepository;
import com.backend.NoBrokerApp.repository.PropertyRepository;
import com.backend.NoBrokerApp.repository.UserRepository;
import com.backend.NoBrokerApp.service.AdminService;
import com.backend.NoBrokerApp.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public AdminController(AdminService adminService,
                           PropertyRepository propertyRepository,
                           UserRepository userRepository,
                           BookingRepository bookingRepository,
                           BookingService bookingService) {
        this.adminService = adminService;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    /**
     * GET /api/admin/stats — Dashboard statistics (ADMIN auth)
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<?>> getAdminStats() {
        long totalProperties = propertyRepository.count();
        long pendingProperties = propertyRepository.findByStatus(Property.PropertyStatus.PENDING).size();
        long approvedProperties = propertyRepository.findByStatus(Property.PropertyStatus.APPROVED).size();
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProperties", totalProperties);
        stats.put("pendingProperties", pendingProperties);
        stats.put("approvedProperties", approvedProperties);
        stats.put("totalUsers", totalUsers);
        stats.put("totalBookings", totalBookings);

        return ResponseEntity.ok(ApiResponse.success("Admin stats retrieved", stats));
    }

    /**
     * GET /api/admin/properties/pending — List all pending properties (ADMIN auth)
     */
    @GetMapping("/properties/pending")
    public ResponseEntity<ApiResponse<?>> getPendingProperties() {
        List<PropertyResponse> pending = adminService.getPendingProperties();
        return ResponseEntity.ok(ApiResponse.success("Pending properties retrieved", pending));
    }

    /**
     * GET /api/admin/properties — List all properties with optional filters (ADMIN auth)
     */
    @GetMapping("/properties")
    public ResponseEntity<ApiResponse<?>> getAllProperties(
            @RequestParam(required = false) String status) {
        List<Property> properties;
        if (status != null && !status.isBlank()) {
            properties = propertyRepository.findByStatus(
                    Property.PropertyStatus.valueOf(status.toUpperCase()));
        } else {
            properties = propertyRepository.findAll();
        }

        List<Map<String, Object>> result = properties.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("title", p.getTitle());
            map.put("type", p.getType().name());
            map.put("price", p.getPrice());
            map.put("city", p.getCity());
            map.put("status", p.getStatus().name());
            map.put("createdAt", p.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("All properties retrieved", result));
    }

    /**
     * GET /api/admin/users — List all users (ADMIN auth)
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<?>> getAllUsers() {
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("role", u.getRole().name());
            map.put("phone", u.getPhone());
            map.put("isVerified", u.getIsVerified());
            map.put("authProvider", u.getAuthProvider().name());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("All users retrieved", result));
    }

    /**
     * GET /api/admin/bookings — List all bookings (ADMIN auth)
     */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<?>> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();

        List<Map<String, Object>> result = bookings.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("propertyId", b.getPropertyId());
            map.put("userId", b.getUserId());
            map.put("bookingType", b.getBookingType().name());
            map.put("status", b.getStatus().name());
            map.put("preferredDate", b.getPreferredDate());
            map.put("message", b.getMessage());
            map.put("createdAt", b.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("All bookings retrieved", result));
    }

    /**
     * PATCH /api/admin/properties/{id}/approve — Approve a pending property (ADMIN auth)
     */
    @PatchMapping("/properties/{id}/approve")
    public ResponseEntity<ApiResponse<?>> approveProperty(@PathVariable Long id) {
        adminService.approveProperty(id);
        return ResponseEntity.ok(ApiResponse.success("Property approved. Owner notified.", null));
    }

    /**
     * PATCH /api/admin/properties/{id}/reject — Reject a pending property (ADMIN auth)
     */
    @PatchMapping("/properties/{id}/reject")
    public ResponseEntity<ApiResponse<?>> rejectProperty(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null) ? body.get("reason") : "No reason provided";
        adminService.rejectProperty(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Property rejected. Owner notified.", null));
    }

    /**
     * PATCH /api/admin/bookings/{id}/confirm — Confirm a pending booking (ADMIN auth)
     */
    @PatchMapping("/bookings/{id}/confirm")
    public ResponseEntity<ApiResponse<?>> confirmBooking(@PathVariable Long id) {
        bookingService.confirmBooking(id);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed", null));
    }

    /**
     * PATCH /api/admin/bookings/{id}/cancel — Cancel a booking (ADMIN auth)
     */
    @PatchMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<?>> cancelBookingAdmin(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", null));
    }
}
