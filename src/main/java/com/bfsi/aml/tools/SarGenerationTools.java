package com.bfsi.aml.tools;

import com.bfsi.aml.model.SarReport;
import com.bfsi.aml.repository.CustomerRepository;
import com.bfsi.aml.repository.SarReportRepository;
import com.bfsi.aml.service.S3DocumentService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class SarGenerationTools {

    private final SarReportRepository sarReportRepo;
    private final CustomerRepository customerRepository;
    private final S3DocumentService s3DocumentService;

    // Simple SAR sequence counter — use DB sequence in production
    private static final AtomicInteger sarSequence = new AtomicInteger(1);

    @Tool("Generate a unique SAR reference number in the format SAR-YYYY-NNNNNN")
    public String generateSarReferenceNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String seq = String.format("%06d", sarSequence.getAndIncrement());
        String ref = "SAR-" + year + "-" + seq;
        log.info("[SAR] Generated SAR reference: {}", ref);
        return ref;
    }

    @Tool("Store the completed SAR report in the database and upload it to S3. Returns the S3 key of the stored report.")
    public String storeSarReport(
            long customerId,
            String sarReferenceNumber,
            String reportContent,
            String triggeredBySkills) {

        log.info("[SAR] Storing SAR {} for customer {}", sarReferenceNumber, customerId);

        // Upload full report text to S3
        String s3Key = s3DocumentService.uploadSarReport(sarReferenceNumber, reportContent);

        // Extract customer name for the record
        String customerName = customerRepository.findById(customerId)
                .map(c -> c.getFullName())
                .orElse("UNKNOWN");

        // Persist SAR record to database
        SarReport sar = SarReport.builder()
                .customerId(customerId)
                .customerName(customerName)
                .sarReferenceNumber(sarReferenceNumber)
                .status(SarReport.SarStatus.PENDING_REVIEW)
                .reportContent(reportContent)
                .findings(extractFindingsSection(reportContent))
                .s3Key(s3Key)
                .generatedAt(LocalDateTime.now())
                .triggeredBySkills(triggeredBySkills)
                .build();

        sarReportRepo.save(sar);
        log.info("[SAR] SAR {} persisted. DB id={}, S3 key={}", sarReferenceNumber, sar.getId(), s3Key);

        return String.format(
                "SAR_STORED: sar_reference=%s, customer_id=%d, customer_name=%s, " +
                "db_id=%d, s3_key=%s, status=PENDING_REVIEW, timestamp=%s",
                sarReferenceNumber, customerId, customerName,
                sar.getId(), s3Key,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    @Tool("Retrieve an existing SAR report by SAR reference number")
    public String getSarReport(String sarReferenceNumber) {
        return sarReportRepo.findBySarReferenceNumber(sarReferenceNumber)
                .map(sar -> String.format(
                        "SAR_FOUND: reference=%s, customer_id=%d, customer_name=%s, " +
                        "status=%s, generated_at=%s, s3_key=%s",
                        sar.getSarReferenceNumber(), sar.getCustomerId(), sar.getCustomerName(),
                        sar.getStatus(), sar.getGeneratedAt(), sar.getS3Key()
                ))
                .orElse("SAR_NOT_FOUND: reference=" + sarReferenceNumber);
    }

    private String extractFindingsSection(String report) {
        int start = report.indexOf("3. DETAILED FINDINGS");
        int end   = report.indexOf("4. TYPOLOGIES");
        if (start >= 0 && end > start) {
            return report.substring(start, end).trim();
        }
        return report.length() > 1000 ? report.substring(0, 1000) : report;
    }
}
