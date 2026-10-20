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
@Table(name = "contact_messages")
public class ContactMessage extends BaseEntity {
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, length = 180)
    private String email;
    @Column(length = 30)
    private String phone;
    @Column(length = 180)
    private String company;
    @Column(nullable = false, length = 200)
    private String subject;
    @Column(nullable = false, columnDefinition = "text")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContactStatus status = ContactStatus.NEW;
}
