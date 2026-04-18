package com.bfsi.aml.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────────────
// Transaction
// ─────────────────────────────────────────────────────
@Entity
@Table(name = "transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private BigDecimal amount;
    private String currency;
    private String counterparty;
    private String counterpartyCountry;
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    private LocalDateTime transactionDate;
    private boolean flagged;

    public enum TransactionType {WIRE_TRANSFER, CASH_DEPOSIT, PAYMENT, WITHDRAWAL}
}
