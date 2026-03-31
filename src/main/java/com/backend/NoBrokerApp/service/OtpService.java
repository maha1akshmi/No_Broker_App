package com.backend.NoBrokerApp.service;

import com.backend.NoBrokerApp.model.OtpVerification;
import com.backend.NoBrokerApp.model.User;
import com.backend.NoBrokerApp.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 10;

    @Transactional
    public void generateAndSendOtp(User user) {
        String otp = generateOtp();

        OtpVerification otpVerification = OtpVerification.builder()
                .user(user)
                .otpCode(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .isUsed(false)
                .purpose(OtpVerification.Purpose.REGISTER)
                .build();

        otpRepository.save(otpVerification);
        emailService.sendOtpEmail(user.getEmail(), otp);
        log.info("OTP generated and sent for user: {}", user.getEmail());
    }

    @Transactional
    public boolean verifyOtp(User user, String otpCode) {
        OtpVerification otpVerification = otpRepository
                .findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
                .orElse(null);

        if (otpVerification == null) {
            log.warn("No active OTP found for user: {}", user.getEmail());
            return false;
        }

        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for user: {}", user.getEmail());
            return false;
        }

        if (!otpVerification.getOtpCode().equals(otpCode)) {
            log.warn("Invalid OTP for user: {}", user.getEmail());
            return false;
        }

        otpVerification.setIsUsed(true);
        otpRepository.save(otpVerification);
        log.info("OTP verified successfully for user: {}", user.getEmail());
        return true;
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
