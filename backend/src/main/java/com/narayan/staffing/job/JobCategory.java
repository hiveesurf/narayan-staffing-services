package com.narayan.staffing.job;

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
@Table(name = "job_categories")
public class JobCategory extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, unique = true, length = 140)
    private String slug;
    @Column(columnDefinition = "text")
    private String description;
    @Column(length = 60)
    private String icon;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private int displayOrder;
}
