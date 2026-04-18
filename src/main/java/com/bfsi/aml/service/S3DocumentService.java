package com.bfsi.aml.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3DocumentService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.kyc-prefix}")
    private String kycPrefix;

    @Value("${aws.s3.sar-prefix}")
    private String sarPrefix;

    // ── Upload KYC document ────────────────────────────────────────────────
    public String uploadKycDocument(MultipartFile file, String customerId) throws IOException {
        String key = kycPrefix + customerId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        log.info("Uploaded KYC document for customer {} → s3://{}/{}", customerId, bucketName, key);
        return key;
    }

    // ── Extract text from KYC PDF stored in S3 ───────────────────────────
    // Used by the kyc-parsing skill tool to feed document content to the LLM.
    public String extractTextFromKycDocument(String s3Key) {
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(s3Key).build())) {
            RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(response);
            try (PDDocument doc = Loader.loadPDF(buffer)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);
                log.debug("Extracted {} chars from KYC doc: {}", text.length(), s3Key);
                // Truncate to avoid overflowing LLM context window
                return text.length() > 8000 ? text.substring(0, 8000) + "\n[...truncated]" : text;
            }
        } catch (Exception e) {
            log.error("Failed to extract text from S3 key: {}", s3Key, e);
            return "ERROR: Could not extract text from document at " + s3Key + ". Reason: " + e.getMessage();
        }
    }

    // ── Store SAR report text in S3 ────────────────────────────────────────
    public String uploadSarReport(String sarReferenceNumber, String reportContent) {
        String key = sarPrefix + sarReferenceNumber + ".txt";
        byte[] bytes = reportContent.getBytes();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("text/plain")
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .build(),
                RequestBody.fromBytes(bytes)
        );
        log.info("SAR report {} stored at s3://{}/{}", sarReferenceNumber, bucketName, key);
        return key;
    }

    // ── Generate pre-signed URL (15 min expiry) for secure download ────────
    public String generatePresignedUrl(String s3Key) {
        return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(15))
                        .getObjectRequest(r -> r.bucket(bucketName).key(s3Key))
                        .build()
        ).url().toString();
    }

    // ── Check if a key exists in S3 ───────────────────────────────────────
    public boolean documentExists(String s3Key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(s3Key).build());
            return true;
        } catch (NoSuchKeyException e) {
            log.error("Failed to head object for s3://{}/{}", s3Key, bucketName, e);
            return false;
        }
    }
}
