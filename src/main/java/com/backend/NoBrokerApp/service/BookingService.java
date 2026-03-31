package com.backend.NoBrokerApp.service;

import com.backend.NoBrokerApp.dto.BookingRequest;
import com.backend.NoBrokerApp.dto.BookingResponse;
import com.backend.NoBrokerApp.model.Booking;
import com.backend.NoBrokerApp.model.Booking.BookingStatus;
import com.backend.NoBrokerApp.model.Booking.BookingType;
import com.backend.NoBrokerApp.model.Property;
import com.backend.NoBrokerApp.model.User;
import com.backend.NoBrokerApp.repository.BookingRepository;
import com.backend.NoBrokerApp.repository.PropertyRepository;
import com.backend.NoBrokerApp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public BookingService(BookingRepository bookingRepository,
                          PropertyRepository propertyRepository,
                          UserRepository userRepository,
                          EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Booking createBooking(BookingRequest request, Long userId) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + request.getPropertyId()));

        if (property.getStatus() != Property.PropertyStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot book a property that is not approved");
        }

        BookingType bookingType = BookingType.VISIT;
        if (request.getBookingType() != null && !request.getBookingType().isBlank()) {
            bookingType = BookingType.valueOf(request.getBookingType().toUpperCase());
        }

        Booking booking = Booking.builder()
                .propertyId(request.getPropertyId())
                .userId(userId)
                .bookingType(bookingType)
                .status(BookingStatus.PENDING)
                .preferredDate(request.getPreferredDate())
                .message(request.getMessage())
                .build();

        return bookingRepository.save(booking);
    }

    public List<BookingResponse> getMyBookings(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all bookings on properties owned by the given user.
     */
    public List<BookingResponse> getBookingsForOwner(Long ownerId) {
        List<Long> propertyIds = propertyRepository.findByOwnerId(ownerId).stream()
                .map(Property::getId)
                .collect(Collectors.toList());

        if (propertyIds.isEmpty()) {
            return Collections.emptyList();
        }

        return bookingRepository.findByPropertyIdIn(propertyIds).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    /**
     * Confirm a booking — called by the property owner.
     * Validates ownership, updates status, and sends confirmation email to the booker.
     */
    @Transactional
    public void confirmBooking(Long bookingId, Long ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        Property property = propertyRepository.findById(booking.getPropertyId())
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));

        // Validate that the caller is the property owner
        if (!property.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You can only confirm bookings on your own properties");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Send confirmation email to the user who booked
        sendConfirmationEmail(booking, property);
    }

    /**
     * Confirm a booking — called by admin (no ownership check).
     */
    @Transactional
    public void confirmBookingAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING bookings can be confirmed");
        }

        Property property = propertyRepository.findById(booking.getPropertyId()).orElse(null);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Send confirmation email
        if (property != null) {
            sendConfirmationEmail(booking, property);
        }
    }

    /**
     * Reject a booking — called by the property owner.
     */
    @Transactional
    public void rejectBooking(Long bookingId, Long ownerId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        Property property = propertyRepository.findById(booking.getPropertyId())
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You can only reject bookings on your own properties");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(reason);
        bookingRepository.save(booking);

        // Send rejection email to the user who booked
        User booker = userRepository.findById(booking.getUserId()).orElse(null);
        if (booker != null) {
            emailService.sendBookingRejectionEmail(
                    booker.getEmail(),
                    booker.getName(),
                    property.getTitle(),
                    reason
            );
        }
    }

    private void sendConfirmationEmail(Booking booking, Property property) {
        User booker = userRepository.findById(booking.getUserId()).orElse(null);
        User owner = userRepository.findById(property.getOwnerId()).orElse(null);

        if (booker != null) {
            emailService.sendBookingConfirmationEmail(
                    booker.getEmail(),
                    booker.getName(),
                    property.getTitle(),
                    owner != null ? owner.getName() : "Property Owner"
            );
        }
    }

    BookingResponse toResponse(Booking booking) {
        Property property = propertyRepository.findById(booking.getPropertyId()).orElse(null);

        BookingResponse.PropertySummary propertySummary = null;
        String ownerName = null;

        if (property != null) {
            propertySummary = BookingResponse.PropertySummary.builder()
                    .id(property.getId())
                    .title(property.getTitle())
                    .city(property.getCity())
                    .location(property.getLocation())
                    .build();

            // Lookup owner name
            User owner = userRepository.findById(property.getOwnerId()).orElse(null);
            if (owner != null) {
                ownerName = owner.getName();
            }
        }

        // Lookup booking user info
        String userName = null;
        String userEmail = null;
        User booker = userRepository.findById(booking.getUserId()).orElse(null);
        if (booker != null) {
            userName = booker.getName();
            userEmail = booker.getEmail();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .property(propertySummary)
                .bookingType(booking.getBookingType().name())
                .status(booking.getStatus().name())
                .preferredDate(booking.getPreferredDate())
                .message(booking.getMessage())
                .rejectionReason(booking.getRejectionReason())
                .confirmedAt(booking.getConfirmedAt())
                .createdAt(booking.getCreatedAt())
                .userName(userName)
                .userEmail(userEmail)
                .ownerName(ownerName)
                .build();
    }
}
