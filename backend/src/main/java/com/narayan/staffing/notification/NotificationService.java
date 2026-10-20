package com.narayan.staffing.notification;

import com.narayan.staffing.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationMessageRepository repository;
    private final AppProperties properties;
    private final ObjectProvider<JavaMailSender> mailSender;

    public NotificationService(NotificationMessageRepository repository, AppProperties properties, ObjectProvider<JavaMailSender> mailSender) {
        this.repository = repository;
        this.properties = properties;
        this.mailSender = mailSender;
    }

    @Transactional
    public void email(String to, String templateKey, String subject, String body, String entityType, UUID entityId) {
        NotificationMessage message = new NotificationMessage();
        message.setChannel("EMAIL");
        message.setRecipient(to);
        message.setTemplateKey(templateKey);
        message.setSubject(subject);
        message.setBody(body);
        message.setRelatedEntityType(entityType);
        message.setRelatedEntityId(entityId);
        message.setStatus("PENDING");
        if (to == null || to.isBlank()) {
            message.setStatus("SKIPPED");
            repository.save(message);
            return;
        }
        if (!properties.getMail().isEnabled()) {
            message.setStatus("SKIPPED");
            repository.save(message);
            log.info("Email provider disabled. Skipped {} for {}", templateKey, to);
            return;
        }
        try {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) {
                message.setStatus("SKIPPED");
                repository.save(message);
                log.info("Mail sender is not configured. Skipped {} for {}", templateKey, to);
                return;
            }
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(properties.getMail().getFrom());
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);
            sender.send(mail);
            message.setStatus("SENT");
            message.setSentAt(Instant.now());
        } catch (Exception ex) {
            message.setStatus("FAILED");
            log.warn("Email {} to {} failed: {}", templateKey, to, ex.getMessage());
        }
        repository.save(message);
    }
}
