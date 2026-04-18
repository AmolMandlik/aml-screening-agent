package com.bfsi.aml.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// ─────────────────────────────────────────────────────
// SanctionsEntity
// ─────────────────────────────────────────────────────
@Entity
@Table(name = "sanctions_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanctionsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String aliases;      // pipe-separated
    private String country;
    private String entityType;   // INDIVIDUAL | ENTITY
    private String program;      // e.g. IRAN-SDN
    private LocalDate listedDate;
    private boolean active;
}
