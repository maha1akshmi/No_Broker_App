package com.backend.NoBrokerApp.controller;

import com.backend.NoBrokerApp.dto.PropertyFilterRequest;
import com.backend.NoBrokerApp.dto.PropertyRequest;
import com.backend.NoBrokerApp.dto.PropertyResponse;
import com.backend.NoBrokerApp.exception.ApiResponse;
import com.backend.NoBrokerApp.model.Property;
import com.backend.NoBrokerApp.security.UserPrincipal;
import com.backend.NoBrokerApp.service.PropertyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    /**
     * POST /api/properties — Create a new property listing (USER auth required)
     * Accepts multipart/form-data with individual fields + image files
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> createProperty(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String type,
            @RequestParam BigDecimal price,
            @RequestParam String location,
            @RequestParam String city,
            @RequestParam(required = false, defaultValue = "") String state,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(required = false) BigDecimal areaSqft,
            @RequestParam(required = false, defaultValue = "false") Boolean isFurnished,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {

        PropertyRequest request = new PropertyRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setType(type);
        request.setPrice(price);
        request.setLocation(location);
        request.setCity(city);
        request.setState(state);
        request.setPincode(pincode);
        request.setBedrooms(bedrooms);
        request.setBathrooms(bathrooms);
        request.setAreaSqft(areaSqft);
        request.setIsFurnished(isFurnished);

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        Property property = propertyService.createProperty(request, images, user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("id", property.getId());
        data.put("status", property.getStatus().name());

        return ResponseEntity.ok(ApiResponse.success("Property submitted for approval", data));
    }

    /**
     * GET /api/properties — Public endpoint, returns APPROVED properties with filters
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getApprovedProperties(
            @ModelAttribute PropertyFilterRequest filter) {
        Map<String, Object> result = propertyService.getApprovedProperties(filter);
        return ResponseEntity.ok(ApiResponse.success("Properties retrieved", result));
    }

    /**
     * GET /api/properties/{id} — Public endpoint, single property details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getPropertyById(@PathVariable Long id) {
        PropertyResponse response = propertyService.getPropertyById(id);
        return ResponseEntity.ok(ApiResponse.success("Property retrieved", response));
    }

    /**
     * GET /api/properties/my — Authenticated user's own listings (all statuses)
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyProperties(Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        List<PropertyResponse> properties = propertyService.getMyProperties(user.getId());
        return ResponseEntity.ok(ApiResponse.success("My properties retrieved", properties));
    }

    /**
     * DELETE /api/properties/{id} — Soft-delete a property (owner only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteProperty(@PathVariable Long id,
                                                          Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        boolean hadActiveBookings = propertyService.deleteProperty(id, user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("hadActiveBookings", hadActiveBookings);

        String message = hadActiveBookings
                ? "Property deleted. Users with active bookings have been notified."
                : "Property deleted successfully";

        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
}
