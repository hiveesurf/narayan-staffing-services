package com.narayan.staffing.cms;

import com.narayan.staffing.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "services")
public class ServiceOffering extends BaseEntity {
    @Column(nullable = false, length = 180)
    private String title;
    @Column(nullable = false, unique = true, length = 180)
    private String slug;
    @Column(length = 500)
    private String shortDescription;
    @Column(columnDefinition = "text")
    private String detailedDescription;
    @Column(length = 60)
    private String icon;
    @Column(length = 500)
    private String bannerImage;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private int displayOrder;
    @Column(length = 180)
    private String seoTitle;
    @Column(length = 300)
    private String seoDescription;
}
