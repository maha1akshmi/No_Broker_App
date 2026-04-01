package com.backend.NoBrokerApp.service;

import com.backend.NoBrokerApp.dto.PropertyFilterRequest;
import com.backend.NoBrokerApp.dto.PropertyRequest;
import com.backend.NoBrokerApp.dto.PropertyResponse;
import com.backend.NoBrokerApp.model.Booking;
import com.backend.NoBrokerApp.model.Booking.BookingStatus;
import com.backend.NoBrokerApp.model.Property;
import com.backend.NoBrokerApp.model.Property.PropertyStatus;
import com.backend.NoBrokerApp.model.Property.PropertyType;
import com.backend.NoBrokerApp.model.PropertyImage;
import com.backend.NoBrokerApp.model.User;
import com.backend.NoBrokerApp.repository.BookingRepository;
import com.backend.NoBrokerApp.repository.PropertyImageRepository;
import com.backend.NoBrokerApp.repository.PropertyRepository;
import com.backend.NoBrokerApp.repository.UserRepository;
import com.backend.NoBrokerApp.upload.CloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public PropertyService(PropertyRepository propertyRepository,
                           PropertyImageRepository propertyImageRepository,
                           CloudinaryService cloudinaryService,
                           UserRepository userRepository,
                           BookingRepository bookingRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.cloudinaryService = cloudinaryService;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Property createProperty(PropertyRequest request, List<MultipartFile> images, Long ownerId) {
        Property property = Property.builder()
                .ownerId(ownerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(PropertyType.valueOf(request.getType().toUpperCase()))
                .price(request.getPrice())
                .location(request.getLocation())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .areaSqft(request.getAreaSqft())
                .isFurnished(request.getIsFurnished() != null ? request.getIsFurnished() : false)
                .status(PropertyStatus.PENDING)
                .build();

        Property savedProperty = propertyRepository.save(property);

        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                String imageUrl = cloudinaryService.uploadImage(images.get(i));
                PropertyImage propertyImage = PropertyImage.builder()
                        .property(savedProperty)
                        .imageUrl(imageUrl)
                        .isPrimary(i == 0)
                        .sortOrder(i)
                        .build();
                propertyImageRepository.save(propertyImage);
            }
        }

        return savedProperty;
    }

    public Map<String, Object> getApprovedProperties(PropertyFilterRequest filter) {
        Pageable pageable = PageRequest.of(
                filter.getPage() != null ? filter.getPage() : 0,
                filter.getSize() != null ? filter.getSize() : 10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        PropertyType type = null;
        if (filter.getType() != null && !filter.getType().isBlank()) {
            type = PropertyType.valueOf(filter.getType().toUpperCase());
        }

        Page<Property> page = propertyRepository.findByFilters(
                PropertyStatus.APPROVED,
                filter.getCity(),
                type,
                filter.getMinPrice(),
                filter.getMaxPrice(),
                filter.getBedrooms(),
                pageable
        );

        List<PropertyResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("currentPage", page.getNumber());

        return response;
    }

    public PropertyResponse getPropertyById(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + id));
        return toResponse(property);
    }

    /**
     * Soft-delete a property. Only the property owner can delete.
     * Sets status to DELETED. Cancels PENDING bookings with a message.
     * Returns true if property had active (CONFIRMED) bookings.
     */
    @Transactional
    public boolean deleteProperty(Long propertyId, Long ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + propertyId));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You can only delete your own properties");
        }

        if (property.getStatus() == PropertyStatus.DELETED) {
            throw new IllegalArgumentException("Property is already deleted");
        }

        // Check for active bookings
        List<Booking> bookings = bookingRepository.findByPropertyId(propertyId);
        boolean hasActiveBookings = bookings.stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING);

        // Cancel all PENDING bookings
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setRejectionReason("Property has been deleted by the owner");
                bookingRepository.save(booking);
            }
        }

        // Soft-delete the property
        property.setStatus(PropertyStatus.DELETED);
        propertyRepository.save(property);

        return hasActiveBookings;
    }

    public List<PropertyResponse> getMyProperties(Long ownerId) {
        return propertyRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PropertyResponse toResponse(Property property) {
        List<String> imageUrls = propertyImageRepository.findByPropertyId(property.getId()).stream()
                .map(PropertyImage::getImageUrl)
                .collect(Collectors.toList());

        // Lookup owner info
        PropertyResponse.OwnerInfo ownerInfo = null;
        if (property.getOwnerId() != null) {
            User owner = userRepository.findById(property.getOwnerId()).orElse(null);
            if (owner != null) {
                ownerInfo = PropertyResponse.OwnerInfo.builder()
                        .name(owner.getName())
                        .email(owner.getEmail())
                        .phone(owner.getPhone())
                        .build();
            }
        }

        return PropertyResponse.builder()
                .id(property.getId())
                .title(property.getTitle())
                .description(property.getDescription())
                .type(property.getType().name())
                .price(property.getPrice())
                .location(property.getLocation())
                .city(property.getCity())
                .state(property.getState())
                .pincode(property.getPincode())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .areaSqft(property.getAreaSqft())
                .isFurnished(property.getIsFurnished())
                .status(property.getStatus().name())
                .rejectionReason(property.getRejectionReason())
                .images(imageUrls)
                .owner(ownerInfo)
                .createdAt(property.getCreatedAt())
                .build();
    }
}
