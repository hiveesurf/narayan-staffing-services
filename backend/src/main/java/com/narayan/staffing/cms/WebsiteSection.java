package com.narayan.staffing.cms;

import com.narayan.staffing.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "website_sections", uniqueConstraints = @UniqueConstraint(columnNames = {"page_key", "section_key"}))
public class WebsiteSection extends BaseEntity {
    @Column(nullable = false, length = 40)
    private String pageKey;
    @Column(nullable = false, length = 60)
    private String sectionKey;
    @Column(length = 300)
    private String title;
    @Column(columnDefinition = "text")
    private String subtitle;
    @Column(columnDefinition = "text")
    private String content;
    @Column(length = 500)
    private String imagePath;
    @Column(length = 80)
    private String ctaLabel;
    @Column(length = 300)
    private String ctaUrl;
    @Column(length = 80)
    private String secondaryCtaLabel;
    @Column(length = 300)
    private String secondaryCtaUrl;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> extraJson;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active = true;
}
