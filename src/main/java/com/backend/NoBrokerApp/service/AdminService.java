package com.backend.NoBrokerApp.service;

import com.backend.NoBrokerApp.dto.PropertyResponse;
import com.backend.NoBrokerApp.model.Property;
import com.backend.NoBrokerApp.model.Property.PropertyStatus;
import com.backend.NoBrokerApp.model.User;
import com.backend.NoBrokerApp.repository.PropertyRepository;
import com.backend.NoBrokerApp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final PropertyRepository propertyRepository;
    private final PropertyService propertyService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public AdminService(PropertyRepository propertyRepository,
                        PropertyService propertyService,
                        EmailService emailService,
                        UserRepository userRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyService = propertyService;
        this.emailService = emailService;
        this.userRepository = userRepository;
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

        // Send approval email to property owner
        User owner = userRepository.findById(property.getOwnerId()).orElse(null);
        if (owner != null) {
            emailService.sendApprovalEmail(owner.getEmail(), property.getTitle());
        }
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

        // Send rejection email to property owner
        User owner = userRepository.findById(property.getOwnerId()).orElse(null);
        if (owner != null) {
            emailService.sendRejectionEmail(owner.getEmail(), property.getTitle(), reason);
        }
    }
}
