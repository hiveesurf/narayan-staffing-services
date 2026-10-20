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
@Table(name = "site_settings")
public class SiteSetting extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String settingKey;
    @Column(columnDefinition = "text")
    private String settingValue;
}
