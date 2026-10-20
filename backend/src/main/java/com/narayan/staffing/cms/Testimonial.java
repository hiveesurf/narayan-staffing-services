package com.narayan.staffing.cms;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.domain.ContactStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "testimonials")
public class Testimonial extends BaseEntity {
    @Column(nullable = false, length = 150)
    private String authorName;
    @Column(length = 150)
    private String authorRole;
    @Column(length = 180)
    private String companyName;
    @Column(nullable = false, columnDefinition = "text")
    private String quote;
    @Column(length = 500)
    private String photoPath;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active = true;
}
