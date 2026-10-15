package com.medisense.config;

import com.medisense.domain.entity.UserEntity;
import com.medisense.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * DemoDataInitializer — Seeds pre-configured test and demo user accounts on startup.
 */
@Component
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUserIfNotExists(
                "demo@medisense.health",
                "DemoUser@2026!",
                "Dr. Aarav Sharma (Demo Patient)",
                "en"
        );

        seedUserIfNotExists(
                "tamil.demo@medisense.health",
                "DemoUser@2026!",
                "கார்த்திகேயன் (Demo Tamil User)",
                "ta"
        );

        seedUserIfNotExists(
                "admin@medisense.health",
                "AdminSecure@2026!",
                "MediSense Chief Admin",
                "en"
        );
    }

    private void seedUserIfNotExists(String email, String rawPassword, String fullName, String languagePref) {
        Optional<UserEntity> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            UserEntity user = UserEntity.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(fullName)
                    .languagePreference(languagePref)
                    .failedAttempts(0)
                    .build();

            userRepository.save(user);
            log.info("Initialized test demo account: email={} (lang={})", email, languagePref);
        } else {
            log.debug("Demo account already present: {}", email);
        }
    }
}
