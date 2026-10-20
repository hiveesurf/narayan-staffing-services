package com.narayan.staffing.workforce;

import com.narayan.staffing.domain.PlacementStatus;
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
@Table(name = "placement_status_history")
public class PlacementStatusEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_id", nullable = false)
    private Placement placement;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PlacementStatus oldStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlacementStatus newStatus;
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
