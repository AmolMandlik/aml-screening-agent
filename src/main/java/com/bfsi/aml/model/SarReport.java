package com.bfsi.aml.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ─────────────────────────────────────────────────────
// SAR Report (generated output)
// ─────────────────────────────────────────────────────
@Entity @Table(name = "sar_report")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SarReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private String customerName;
    private String sarReferenceNumber;
    @Enumerated(EnumType.STRING)
    private SarStatus status;
    @Lob private String findings;          // full agent reasoning
    @Lob private String reportContent;     // formatted SAR text
    private String s3Key;                  // where SAR PDF is stored in S3
    private LocalDateTime generatedAt;
    private String triggeredBySkills;      // comma-separated skill names that fired

    public enum SarStatus { DRAFT, PENDING_REVIEW, FILED, CLOSED }
}
