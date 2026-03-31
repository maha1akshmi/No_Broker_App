package com.backend.NoBrokerApp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your NoBroker Verification Code");
        message.setText("Your OTP is: " + otp + ". This code is valid for 10 minutes. Do not share it with anyone.");

        try {
            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    public void sendApprovalEmail(String toEmail, String propertyTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your property listing is now live!");
        message.setText("Great news! Your property \"" + propertyTitle
                + "\" has been approved and is now visible to buyers/renters on NoBroker.");

        try {
            mailSender.send(message);
            log.info("Approval email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send approval email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendRejectionEmail(String toEmail, String propertyTitle, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Update on your property listing");
        message.setText("Your property \"" + propertyTitle
                + "\" was not approved. Reason: " + reason
                + ". Please make corrections and resubmit.");

        try {
            mailSender.send(message);
            log.info("Rejection email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send rejection email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends a booking confirmation email to the user who booked the visit.
     */
    public void sendBookingConfirmationEmail(String toEmail, String userName, String propertyTitle, String ownerName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your booking has been confirmed! — NoBroker");
        message.setText(
                "Hi " + userName + ",\n\n"
                + "Great news! Your booking for the property \"" + propertyTitle
                + "\" has been confirmed by the property owner (" + ownerName + ").\n\n"
                + "Please be on time for your scheduled visit. You can view booking details in your NoBroker dashboard.\n\n"
                + "Best regards,\n"
                + "Team NoBroker"
        );

        try {
            mailSender.send(message);
            log.info("Booking confirmation email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends a booking rejection email to the user who booked the visit.
     */
    public void sendBookingRejectionEmail(String toEmail, String userName, String propertyTitle, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Update on your booking — NoBroker");
        message.setText(
                "Hi " + userName + ",\n\n"
                + "Unfortunately, your booking for the property \"" + propertyTitle
                + "\" was not accepted by the property owner.\n\n"
                + (reason != null && !reason.isBlank()
                    ? "Reason: " + reason + "\n\n"
                    : "")
                + "You can browse other properties and schedule a new visit from your NoBroker dashboard.\n\n"
                + "Best regards,\n"
                + "Team NoBroker"
        );

        try {
            mailSender.send(message);
            log.info("Booking rejection email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send booking rejection email to {}: {}", toEmail, e.getMessage());
        }
    }
}
