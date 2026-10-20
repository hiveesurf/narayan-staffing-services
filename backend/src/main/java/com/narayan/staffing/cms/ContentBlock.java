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
@Table(name = "content_blocks")
public class ContentBlock extends BaseEntity {
    @Column(nullable = false, length = 60)
    private String groupKey;
    @Column(length = 200)
    private String title;
    @Column(length = 300)
    private String subtitle;
    @Column(columnDefinition = "text")
    private String body;
    @Column(length = 60)
    private String icon;
    @Column(length = 500)
    private String imagePath;
    @Column(length = 300)
    private String linkUrl;
    @Column(length = 40)
    private String metricValue;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active = true;
}
