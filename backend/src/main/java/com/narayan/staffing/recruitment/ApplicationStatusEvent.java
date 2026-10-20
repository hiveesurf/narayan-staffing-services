package com.narayan.staffing.recruitment;

import com.narayan.staffing.domain.ApplicationStatus;
import com.narayan.staffing.identity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "application_status_history")
public class ApplicationStatusEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ApplicationStatus oldStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ApplicationStatus newStatus;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;
    @Column(length = 500)
    private String reason;
    @Column(columnDefinition = "text")
    private String note;
    @Column(nullable = false)
    private Instant changedAt = Instant.now();
}
