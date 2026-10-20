package com.narayan.staffing.candidate;

import com.narayan.staffing.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidates")
public class Candidate extends BaseEntity {
    @Column(nullable = false, length = 150)
    private String fullName;
    private LocalDate dateOfBirth;
    @Column(length = 30)
    private String gender;
    @Column(nullable = false, length = 20)
    private String mobile;
    @Column(nullable = false, unique = true, length = 180)
    private String email;
    @Column(length = 20)
    private String alternatePhone;
    @Column(columnDefinition = "text")
    private String address;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String state;
    @Column(length = 12)
    private String pincode;
    @Column(length = 80)
    private String country = "India";
    @Column(columnDefinition = "text")
    private String permanentAddress;
    @Column(length = 150)
    private String fatherName;
    @Column(length = 150)
    private String motherName;
    @Column(length = 30)
    private String maritalStatus;
    @Column(length = 150)
    private String emergencyContactName;
    @Column(length = 20)
    private String emergencyContactPhone;
    @Column(length = 150)
    private String highestQualification;
    @Column(length = 150)
    private String specialization;
    @Column(precision = 5, scale = 2)
    private BigDecimal totalExperience;
    @Column(precision = 5, scale = 2)
    private BigDecimal relevantExperience;
    @Column(length = 180)
    private String currentCompany;
    @Column(length = 150)
    private String currentJobTitle;
    @Column(precision = 14, scale = 2)
    private BigDecimal currentSalary;
    @Column(precision = 14, scale = 2)
    private BigDecimal expectedSalary;
    @Column(length = 60)
    private String noticePeriod;
    @Column(length = 500)
    private String photoPath;
    @Column(columnDefinition = "text")
    private String aadhaarEncrypted;
    @Column(columnDefinition = "text")
    private String panEncrypted;
    @Column(length = 80)
    private String otherId;
}
