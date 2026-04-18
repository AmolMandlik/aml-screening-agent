package com.bfsi.aml.controller;

import com.bfsi.aml.model.Customer;
import com.bfsi.aml.model.SarReport;
import com.bfsi.aml.repository.CustomerRepository;
import com.bfsi.aml.service.AmlScreeningService;
import com.bfsi.aml.service.AmlScreeningService.AmlScreeningResponse;
import com.bfsi.aml.service.S3DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/aml")
@RequiredArgsConstructor
public class AmlController {

    private final AmlScreeningService amlScreeningService;
    private final CustomerRepository customerRepository;
    private final S3DocumentService s3DocumentService;

    // ── POST /api/v1/aml/screen/{customerId}
    // Triggers the full AML agent screening for a customer.
    // The agent activates each skill in sequence and returns consolidated findings.
    @PostMapping("/screen/{customerId}")
    public ResponseEntity<AmlScreeningResponse> screenCustomer(@PathVariable Long customerId) {
        log.info("Received AML screening request for customer {}", customerId);
        AmlScreeningResponse response = amlScreeningService.screenCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    // ── GET /api/v1/aml/customers
    // List all customers (for testing convenience)
    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> listCustomers() {
        return ResponseEntity.ok(customerRepository.findAll());
    }

    // ── POST /api/v1/aml/customers/{customerId}/kyc-document
    // Upload a KYC PDF document for a customer to S3.
    @PostMapping(value = "/customers/{customerId}/kyc-document",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadKycDocument(
            @PathVariable Long customerId,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Uploaded file is empty"));
        }
        if (!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only PDF files are accepted for KYC documents"));
        }

        String s3Key = s3DocumentService.uploadKycDocument(file, customerId.toString());

        // Update customer record with S3 key
        customerRepository.findById(customerId).ifPresent(customer -> {
            customer.setKycDocumentS3Key(s3Key);
            customer.setKycStatus(Customer.KycStatus.PENDING);
            customerRepository.save(customer);
        });

        log.info("KYC document uploaded for customer {} → {}", customerId, s3Key);
        return ResponseEntity.ok(Map.of(
                "customerId", customerId.toString(),
                "s3Key", s3Key,
                "message", "KYC document uploaded successfully. Run /screen/{customerId} to analyse."
        ));
    }

    // ── GET /api/v1/aml/sar/{customerId}
    // List all SAR reports generated for a customer.
    @GetMapping("/sar/{customerId}")
    public ResponseEntity<List<SarReport>> getSarReports(@PathVariable Long customerId) {
        List<SarReport> sars = amlScreeningService.getSarReportsForCustomer(customerId);
        return ResponseEntity.ok(sars);
    }

    // ── GET /api/v1/aml/sar/reference/{sarReference}
    // Retrieve a specific SAR by its reference number.
    @GetMapping("/sar/reference/{sarReference}")
    public ResponseEntity<SarReport> getSarByReference(@PathVariable String sarReference) {
        return ResponseEntity.ok(amlScreeningService.getSarByReference(sarReference));
    }

    // ── GET /api/v1/aml/sar/download/{sarReference}
    // Generate a pre-signed S3 URL to download the SAR report text.
    @GetMapping("/sar/download/{sarReference}")
    public ResponseEntity<Map<String, String>> downloadSar(@PathVariable String sarReference) {
        SarReport sar = amlScreeningService.getSarByReference(sarReference);
        String presignedUrl = s3DocumentService.generatePresignedUrl(sar.getS3Key());
        return ResponseEntity.ok(Map.of(
                "sarReference", sarReference,
                "downloadUrl", presignedUrl,
                "expiresInMinutes", "15"
        ));
    }
}
