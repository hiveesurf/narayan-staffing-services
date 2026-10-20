package com.narayan.staffing.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, UUID> {
}
