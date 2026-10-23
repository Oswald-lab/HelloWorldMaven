package com.example.user;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final Environment environment;

    public EmailService(ObjectProvider<JavaMailSender> mailSender, Environment environment) {
        this.mailSender = mailSender;
        this.environment = environment;
    }

    public void sendOtp(String email, String code, Duration ttl) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("Your HashBuddy sign-in code");
                message.setText("Your sign-in code is " + code + ". It expires in "
                        + ttl.toMinutes() + " minutes.");
                String from = environment.getProperty("spring.mail.username");
                if (from != null && !from.isBlank()) {
                    message.setFrom(from);
                }
                sender.send(message);
                log.info("Sent sign-in code to {}", email);
                return;
            } catch (MailException ex) {
                log.warn("Could not send sign-in code to {}: {}", email, ex.getMessage());
            }
        }
        log.info("Sign-in code for {}: {} (expires in {} minutes)", email, code, ttl.toMinutes());
    }
}
