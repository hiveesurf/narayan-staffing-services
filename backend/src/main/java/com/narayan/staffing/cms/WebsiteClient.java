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
@Table(name = "website_clients")
public class WebsiteClient extends BaseEntity {
    @Column(nullable = false, length = 180)
    private String name;
    @Column(length = 500)
    private String logoPath;
    @Column(length = 255)
    private String website;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active = true;
}
