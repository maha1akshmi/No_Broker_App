package com.backend.NoBrokerApp.service;

import com.backend.NoBrokerApp.dto.PropertyResponse;
import com.backend.NoBrokerApp.model.Property;
import com.backend.NoBrokerApp.model.Property.PropertyStatus;
import com.backend.NoBrokerApp.repository.PropertyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final PropertyRepository propertyRepository;
    private final PropertyService propertyService;

    // EmailService from Maha's module — injected via Spring DI
    // Will be autowired once Maha's email package is available
    // private final EmailService emailService;

    public AdminService(PropertyRepository propertyRepository,
                        PropertyService propertyService) {
        this.propertyRepository = propertyRepository;
        this.propertyService = propertyService;
    }

    public List<PropertyResponse> getPendingProperties() {
        return propertyRepository.findByStatus(PropertyStatus.PENDING).stream()
                .map(propertyService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveProperty(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + propertyId));

        if (property.getStatus() != PropertyStatus.PENDING) {
            throw new IllegalArgumentException("Property is not in PENDING status");
        }

        property.setStatus(PropertyStatus.APPROVED);
        propertyRepository.save(property);

        // TODO: Uncomment after Maha's EmailService is integrated
        // emailService.sendApprovalEmail(ownerEmail, property.getTitle());
    }

    @Transactional
    public void rejectProperty(Long propertyId, String reason) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + propertyId));

        if (property.getStatus() != PropertyStatus.PENDING) {
            throw new IllegalArgumentException("Property is not in PENDING status");
        }

        property.setStatus(PropertyStatus.REJECTED);
        property.setRejectionReason(reason);
        propertyRepository.save(property);

        // TODO: Uncomment after Maha's EmailService is integrated
        // emailService.sendRejectionEmail(ownerEmail, property.getTitle(), reason);
    }
}
