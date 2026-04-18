package com.bfsi.aml.repository;

import com.bfsi.aml.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCustomerId(Long customerId);

    @Query("SELECT t FROM Transaction t WHERE t.customerId = :customerId AND t.transactionDate >= :since ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentByCustomerId(@Param("customerId") Long customerId,
                                             @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.customerId = :customerId AND t.transactionDate >= :since")
    long countRecentTransactions(@Param("customerId") Long customerId,
                                 @Param("since") LocalDateTime since);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.customerId = :customerId AND t.transactionDate >= :since")
    java.math.BigDecimal sumRecentTransactions(@Param("customerId") Long customerId,
                                               @Param("since") LocalDateTime since);
}

