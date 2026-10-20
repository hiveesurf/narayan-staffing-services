package com.narayan.staffing.company;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.domain.CompanyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String companyCode;
    @Column(nullable = false, length = 200)
    private String companyName;
    @Column(length = 200)
    private String legalName;
    @Column(length = 120)
    private String industry;
    @Column(length = 80)
    private String companyType;
    @Column(length = 255)
    private String website;
    @Column(length = 180)
    private String email;
    @Column(length = 30)
    private String phone;
    @Column(columnDefinition = "text")
    private String address;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String state;
    @Column(length = 100)
    private String country = "India";
    @Column(length = 12)
    private String pincode;
    @Column(length = 20)
    private String gstNumber;
    @Column(length = 20)
    private String pan;
    @Column(length = 150)
    private String contactPerson;
    @Column(length = 120)
    private String contactPersonDesignation;
    @Column(length = 30)
    private String contactPersonPhone;
    @Column(length = 180)
    private String contactPersonEmail;
    @Column(columnDefinition = "text")
    private String billingAddress;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompanyStatus status = CompanyStatus.PROSPECT;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(length = 500)
    private String logoPath;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> coverPaths = new ArrayList<>();
}
