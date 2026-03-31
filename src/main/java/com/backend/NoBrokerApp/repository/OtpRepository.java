package com.backend.NoBrokerApp.repository;

import com.backend.NoBrokerApp.model.OtpVerification;
import com.backend.NoBrokerApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);

    void deleteAllByUser(User user);
}
