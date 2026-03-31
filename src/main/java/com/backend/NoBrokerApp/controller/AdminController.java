package com.backend.NoBrokerApp.controller;

import com.backend.NoBrokerApp.dto.PropertyResponse;
import com.backend.NoBrokerApp.exception.ApiResponse;
import com.backend.NoBrokerApp.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
}
