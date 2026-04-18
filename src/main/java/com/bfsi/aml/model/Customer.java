package com.bfsi.aml.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────────────
// Customer
// ─────────────────────────────────────────────────────
@Entity
@Table(name = "customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private LocalDate dateOfBirth;
    private String nationality;
    private String address;
    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;
    private String kycDocumentS3Key;   // S3 key for KYC PDF
    private LocalDateTime createdAt;

    public enum KycStatus {PENDING, VERIFIED, UNDER_REVIEW, REJECTED}
}
