package com.bfsi.aml.repository;

import com.bfsi.aml.model.SarReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SarReportRepository extends JpaRepository<SarReport, Long> {
    List<SarReport> findByCustomerId(Long customerId);

    Optional<SarReport> findBySarReferenceNumber(String sarReferenceNumber);
}
