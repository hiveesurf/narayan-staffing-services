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
@Table(name = "industries")
public class Industry extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, unique = true, length = 140)
    private String slug;
    @Column(columnDefinition = "text")
    private String description;
    @Column(length = 60)
    private String icon;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active = true;
}
