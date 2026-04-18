package com.bfsi.aml.tools;

import com.bfsi.aml.model.Customer;
import com.bfsi.aml.repository.CustomerRepository;
import com.bfsi.aml.service.S3DocumentService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class KycParsingTools {

    private final CustomerRepository customerRepository;
    private final S3DocumentService s3DocumentService;

    @Tool("Fetch and extract text from the KYC PDF document stored in S3 for the given customer ID")
    public String fetchKycDocumentText(long customerId) {
        log.info("[KYC-PARSING] Fetching KYC document for customer {}", customerId);
        return customerRepository.findById(customerId)
                .map(customer -> {
                    String s3Key = customer.getKycDocumentS3Key();
                    if (s3Key == null || s3Key.isBlank()) {
                        return "ERROR: No KYC document on file for customer " + customerId;
                    }
                    if (!s3DocumentService.documentExists(s3Key)) {
                        return "ERROR: KYC document not found in S3 at key: " + s3Key;
                    }
                    String text = s3DocumentService.extractTextFromKycDocument(s3Key);
                    log.info("[KYC-PARSING] Extracted {} chars from S3 key: {}", text.length(), s3Key);
                    return "CUSTOMER_NAME_ON_FILE: " + customer.getFullName() + "\n" +
                           "DOCUMENT_TEXT:\n" + text;
                })
                .orElse("ERROR: Customer not found with ID " + customerId);
    }

    @Tool("Persist the KYC parsing result for a customer including extracted identity fields and validation status")
    public String recordKycParsingResult(
            long customerId,
            String extractedName,
            String nationality,
            String documentType,
            String documentNumber,
            String expiryDate,
            boolean isValid,
            String anomalies) {

        log.info("[KYC-PARSING] Recording result for customer {} — valid={}, anomalies={}",
                customerId, isValid, anomalies);

        customerRepository.findById(customerId).ifPresent(customer -> {
            if (isValid) {
                customer.setKycStatus(Customer.KycStatus.VERIFIED);
            } else if (anomalies != null && !anomalies.equalsIgnoreCase("NONE")) {
                customer.setKycStatus(Customer.KycStatus.UNDER_REVIEW);
            }
            customerRepository.save(customer);
        });

        return String.format(
                "KYC_PARSING_RECORDED: customer_id=%d, extracted_name=%s, nationality=%s, " +
                "doc_type=%s, doc_number=%s, expiry=%s, is_valid=%b, anomalies=%s, timestamp=%s",
                customerId, extractedName, nationality, documentType,
                documentNumber, expiryDate, isValid, anomalies, LocalDateTime.now()
        );
    }
}
