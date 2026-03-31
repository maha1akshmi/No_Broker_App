package com.backend.NoBrokerApp.service;

import com.backend.NoBrokerApp.dto.BookingRequest;
import com.backend.NoBrokerApp.dto.BookingResponse;
import com.backend.NoBrokerApp.model.Booking;
import com.backend.NoBrokerApp.model.Booking.BookingStatus;
import com.backend.NoBrokerApp.model.Booking.BookingType;
import com.backend.NoBrokerApp.model.Property;
import com.backend.NoBrokerApp.repository.BookingRepository;
import com.backend.NoBrokerApp.repository.PropertyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;

    public BookingService(BookingRepository bookingRepository,
                          PropertyRepository propertyRepository) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
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

    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        Property property = propertyRepository.findById(booking.getPropertyId()).orElse(null);

        BookingResponse.PropertySummary propertySummary = null;
        if (property != null) {
            propertySummary = BookingResponse.PropertySummary.builder()
                    .id(property.getId())
                    .title(property.getTitle())
                    .city(property.getCity())
                    .build();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .property(propertySummary)
                .bookingType(booking.getBookingType().name())
                .status(booking.getStatus().name())
                .preferredDate(booking.getPreferredDate())
                .message(booking.getMessage())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
