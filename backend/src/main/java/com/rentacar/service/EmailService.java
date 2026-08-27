package com.rentacar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${rentacar.mail.from}")
    private String fromAddress;

    /**
     * Failures are logged, not propagated: UC1.3 requires the same confirmation
     * message to reach the client regardless of whether the email was deliverable,
     * so a misconfigured mail provider must not surface as a request error.
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Reset your RentaCar password");
            message.setText("""
                    We received a request to reset your RentaCar password.

                    Reset your password here: %s

                    This link expires in 30 minutes. If you didn't request this, you can ignore this email.
                    """.formatted(resetLink));
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}", toEmail, ex);
        }
    }
}
