package com.example.user;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration TTL = Duration.ofMinutes(10);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final EmailService emailService;

    public OtpService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public void requestCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty() || !userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("No account found for that email address.");
        }
        String code = generateCode();
        challenges.put(normalizedEmail, new Challenge(code, Instant.now().plus(TTL)));
        emailService.sendOtp(normalizedEmail, code, TTL);
    }

    public User verify(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        Challenge challenge = challenges.get(normalizedEmail);
        if (challenge == null) {
            throw new IllegalArgumentException("No active code. Please request a new one.");
        }
        synchronized (challenge) {
            if (Instant.now().isAfter(challenge.expiresAt)) {
                challenges.remove(normalizedEmail);
                throw new IllegalArgumentException("That code has expired. Please request a new one.");
            }
            if (challenge.attemptsRemaining <= 0) {
                challenges.remove(normalizedEmail);
                throw new IllegalArgumentException("Too many incorrect attempts. Please request a new code.");
            }
            String submitted = code == null ? "" : code.trim();
            if (!challenge.code.equals(submitted)) {
                challenge.attemptsRemaining--;
                if (challenge.attemptsRemaining <= 0) {
                    challenges.remove(normalizedEmail);
                    throw new IllegalArgumentException(
                            "Too many incorrect attempts. Please request a new code.");
                }
                throw new IllegalArgumentException("Incorrect code. "
                        + challenge.attemptsRemaining + " attempts remaining.");
            }
            challenges.remove(normalizedEmail);
        }
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("No account found for that email address."));
    }

    public Duration getTtl() {
        return TTL;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Challenge {

        private final String code;
        private final Instant expiresAt;
        private int attemptsRemaining = MAX_ATTEMPTS;

        private Challenge(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
}
