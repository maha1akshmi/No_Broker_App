package com.backend.NoBrokerApp.service;

import com.backend.NoBrokerApp.dto.PropertyFilterRequest;
import com.backend.NoBrokerApp.dto.PropertyRequest;
import com.backend.NoBrokerApp.dto.PropertyResponse;
import com.backend.NoBrokerApp.model.Property;
import com.backend.NoBrokerApp.model.Property.PropertyStatus;
import com.backend.NoBrokerApp.model.Property.PropertyType;
import com.backend.NoBrokerApp.model.PropertyImage;
import com.backend.NoBrokerApp.repository.PropertyImageRepository;
import com.backend.NoBrokerApp.repository.PropertyRepository;
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

    public PropertyService(PropertyRepository propertyRepository,
                           PropertyImageRepository propertyImageRepository,
                           CloudinaryService cloudinaryService) {
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.cloudinaryService = cloudinaryService;
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

    public List<PropertyResponse> getMyProperties(Long ownerId) {
        return propertyRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PropertyResponse toResponse(Property property) {
        List<String> imageUrls = propertyImageRepository.findByPropertyId(property.getId()).stream()
                .map(PropertyImage::getImageUrl)
                .collect(Collectors.toList());

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
                .createdAt(property.getCreatedAt())
                .build();
    }
}
