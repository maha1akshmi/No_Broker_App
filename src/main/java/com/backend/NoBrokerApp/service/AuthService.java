package com.backend.NoBrokerApp.service;

import com.backend.NoBrokerApp.dto.*;
import com.backend.NoBrokerApp.model.User;
import com.backend.NoBrokerApp.repository.UserRepository;
import com.backend.NoBrokerApp.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final GoogleOAuthService googleOAuthService;

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.USER)
                .authProvider(User.AuthProvider.LOCAL)
                .isVerified(false)
                .build();

        userRepository.save(user);
        otpService.generateAndSendOtp(user);

        log.info("User registered: {}", request.getEmail());
        return request.getEmail();
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsVerified()) {
            throw new RuntimeException("User is already verified");
        }

        boolean isValid = otpService.verifyOtp(user, request.getOtp());
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        user.setIsVerified(true);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        log.info("User verified via OTP: {}", request.getEmail());
        return new AuthResponse(token, user.getRole().name(), user.getName(), user.getEmail(), user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (user.getAuthProvider() == User.AuthProvider.GOOGLE) {
            throw new RuntimeException("This account uses Google Sign-In. Please login with Google.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!user.getIsVerified()) {
            // Resend OTP for unverified users
            otpService.generateAndSendOtp(user);
            throw new RuntimeException("Account not verified. A new OTP has been sent to your email.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        log.info("User logged in: {}", request.getEmail());
        return new AuthResponse(token, user.getRole().name(), user.getName(), user.getEmail(), user.getId());
    }

    @Transactional
    public AuthResponse googleLogin(String idToken) {
        GoogleOAuthService.GoogleUserInfo googleUser = googleOAuthService.verifyIdToken(idToken);

        User user = userRepository.findByEmail(googleUser.email())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .name(googleUser.name())
                            .email(googleUser.email())
                            .googleId(googleUser.googleId())
                            .role(User.Role.USER)
                            .authProvider(User.AuthProvider.GOOGLE)
                            .isVerified(true)
                            .build();
                    return userRepository.save(newUser);
                });

        // If existing user logged in via Google for the first time, update their Google ID
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleUser.googleId());
            user.setAuthProvider(User.AuthProvider.GOOGLE);
            user.setIsVerified(true);
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        log.info("Google login for: {}", user.getEmail());
        return new AuthResponse(token, user.getRole().name(), user.getName(), user.getEmail(), user.getId());
    }
}
