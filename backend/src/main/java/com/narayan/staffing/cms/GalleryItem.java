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
@Table(name = "gallery_items")
public class GalleryItem extends BaseEntity {
    @Column(nullable = false, length = 180)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @Column(length = 80)
    private String category;
    @Column(nullable = false, length = 500)
    private String imagePath;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private int displayOrder;
}
