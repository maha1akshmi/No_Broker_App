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
}
