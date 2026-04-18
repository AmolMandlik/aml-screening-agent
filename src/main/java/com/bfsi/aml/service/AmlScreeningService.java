package com.bfsi.aml.service;

import com.bfsi.aml.agent.AmlAgentConfig.AmlScreeningAgent;
import com.bfsi.aml.model.Customer;
import com.bfsi.aml.model.SarReport;
import com.bfsi.aml.repository.CustomerRepository;
import com.bfsi.aml.repository.SarReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmlScreeningService {

    private final AmlScreeningAgent amlScreeningAgent;
    private final CustomerRepository customerRepository;
    private final SarReportRepository sarReportRepository;

    // ── DTO for the API response ───────────────────────────────────────────
    public record AmlScreeningResponse(
            Long customerId,
            String customerName,
            String screeningStatus,      // CLEAR | REVIEW_REQUIRED | BLOCKED
            String agentFindings,        // full agent reasoning text
            String sarReference,         // null if no SAR generated
            String sarS3Key,             // null if no SAR generated
            LocalDateTime screenedAt,
            Map<String, String> skillSummary
    ) {}

    // ── Run full AML screening for a customer by ID ────────────────────────
    public AmlScreeningResponse screenCustomer(Long customerId) {
        log.info("=== Starting AML screening for customer {} ===", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        // Build the prompt — the agent will orchestrate all skills from here
        String prompt = String.format(
                """
                        Perform a complete AML screening for the following customer. \
                        Execute all required skills in order.
                        
                        Customer ID: %d
                        Registered Name: %s
                        Nationality: %s
                        KYC Status: %s
                        Address: %s
                        
                        Begin with kyc-parsing, then sanctions-lookup, pep-screening, graph-analysis, \
                        and sar-generation if thresholds are breached.""",
                customer.getId(),
                customer.getFullName(),
                customer.getNationality(),
                customer.getKycStatus(),
                customer.getAddress()
        );

        log.info("Invoking AML agent for customer: {}", customer.getFullName());
        String agentFindings = amlScreeningAgent.screenCustomer(prompt);
        log.info("Agent completed screening for customer {}", customerId);

        // Derive screening status from agent output
        String screeningStatus = deriveStatus(agentFindings);

        // Look up any SAR generated during this screening
        List<SarReport> sars = sarReportRepository.findByCustomerId(customerId);
        SarReport latestSar = sars.isEmpty() ? null : sars.getLast();

        return new AmlScreeningResponse(
                customerId,
                customer.getFullName(),
                screeningStatus,
                agentFindings,
                latestSar != null ? latestSar.getSarReferenceNumber() : null,
                latestSar != null ? latestSar.getS3Key() : null,
                LocalDateTime.now(),
                extractSkillSummary(agentFindings)
        );
    }

    // ── Retrieve all SARs for a customer ──────────────────────────────────
    public List<SarReport> getSarReportsForCustomer(Long customerId) {
        return sarReportRepository.findByCustomerId(customerId);
    }

    // ── Retrieve a specific SAR by reference ──────────────────────────────
    public SarReport getSarByReference(String reference) {
        return sarReportRepository.findBySarReferenceNumber(reference)
                .orElseThrow(() -> new IllegalArgumentException("SAR not found: " + reference));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private String deriveStatus(String findings) {
        if (findings == null) return "UNKNOWN";
        String upper = findings.toUpperCase();
        if (upper.contains("BLOCK") || upper.contains("CONFIRMED MATCH")) return "BLOCKED";
        if (upper.contains("SAR_GENERATION_RESULT") || upper.contains("REVIEW")
                || upper.contains("HIGH RISK") || upper.contains("EDD_REQUIRED")) return "REVIEW_REQUIRED";
        return "CLEAR";
    }

    private Map<String, String> extractSkillSummary(String findings) {
        // Lightweight keyword extraction — in production, use structured output from agent
        return Map.of(
                "kyc_parsing",      findings.contains("KYC_PARSING_RESULT") ? "COMPLETED" : "PENDING",
                "sanctions_lookup", findings.contains("SANCTIONS_RESULT")   ? "COMPLETED" : "PENDING",
                "pep_screening",    findings.contains("PEP_RESULT")          ? "COMPLETED" : "PENDING",
                "graph_analysis",   findings.contains("GRAPH_ANALYSIS_RESULT") ? "COMPLETED" : "PENDING",
                "sar_generation",   findings.contains("SAR_GENERATION_RESULT") ? "COMPLETED" : "NOT_TRIGGERED"
        );
    }
}
