package com.bfsi.aml.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// ─────────────────────────────────────────────────────
// PepEntity
// ─────────────────────────────────────────────────────
@Entity
@Table(name = "pep_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String country;
    private String position;
    private String riskLevel;    // HIGH | MEDIUM | LOW
    private LocalDate activeSince;
}
