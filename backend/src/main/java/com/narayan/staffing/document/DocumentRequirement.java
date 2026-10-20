package com.narayan.staffing.document;

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
@Table(name = "document_requirements")
public class DocumentRequirement extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false, length = 120)
    private String label;
    @Column(length = 300)
    private String description;
    @Column(nullable = false)
    private boolean required;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private int displayOrder;
}
