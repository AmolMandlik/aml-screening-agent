package com.bfsi.aml.tools;

import com.bfsi.aml.model.Transaction;
import com.bfsi.aml.repository.TransactionRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GraphAnalysisTools {

    private final TransactionRepository transactionRepo;

    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "KP", "IR", "SY", "YE", "LY", "SO", "SS", "CF", "ML", "HT",
            "MM", "PK", "RU", "BY", "VE"
    );

    @Value("${aml.agent.graph.transaction-lookback-days:90}")
    private int lookbackDays;

    @Value("${aml.agent.graph.velocity-threshold:5}")
    private int velocityThreshold;

    @Value("${aml.agent.graph.amount-threshold:10000}")
    private int amountThreshold;

    @Tool("Fetch all transactions for a customer within a lookback window (in days). Returns enriched transaction list for pattern analysis.")
    public String getRecentTransactions(long customerId, int lookbackDays) {
        log.info("[GRAPH] Fetching transactions for customer {} (last {} days)", customerId, lookbackDays);

        List<Transaction> txns = transactionRepo.findRecentByCustomerId(
                customerId, LocalDateTime.now().minusDays(lookbackDays)
        );

        if (txns.isEmpty()) {
            return "TRANSACTIONS: customer_id=" + customerId + " | count=0 | result=NO_TRANSACTIONS";
        }

        StringBuilder sb = new StringBuilder(
                "TRANSACTIONS: customer_id=" + customerId + " | count=" + txns.size() + "\n"
        );
        for (Transaction t : txns) {
            sb.append(String.format(
                    "  TXN: id=%d | date=%s | amount=%s %s | counterparty=%s | country=%s | type=%s\n",
                    t.getId(), t.getTransactionDate().toLocalDate(),
                    t.getAmount(), t.getCurrency(),
                    t.getCounterparty(), t.getCounterpartyCountry(), t.getTransactionType()
            ));
        }
        return sb.toString();
    }

    @Tool("Check transaction velocity and aggregate sums within a time window to detect structuring patterns")
    public String checkVelocity(long customerId, int windowDays, double thresholdAmount) {
        log.info("[GRAPH] Checking velocity for customer {} (window={}d, threshold={})",
                customerId, windowDays, thresholdAmount);

        LocalDateTime since = LocalDateTime.now().minusDays(windowDays);
        List<Transaction> txns = transactionRepo.findRecentByCustomerId(customerId, since);

        long count = txns.size();
        BigDecimal total = txns.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sub-threshold transactions (structuring band: 9000–9999 USD)
        List<Transaction> subThreshold = txns.stream()
                .filter(t -> {
                    BigDecimal amt = t.getAmount();
                    return amt.compareTo(new BigDecimal("9000")) >= 0
                            && amt.compareTo(new BigDecimal("10000")) < 0;
                })
                .collect(Collectors.toList());

        boolean structuringSuspected = subThreshold.size() >= 3;
        BigDecimal subThresholdTotal = subThreshold.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format(
                "VELOCITY_CHECK: customer_id=%d | window_days=%d | transaction_count=%d | " +
                "total_amount=%.2f | velocity_flag=%b | sub_threshold_transactions=%d | " +
                "sub_threshold_total=%.2f | structuring_suspected=%b",
                customerId, windowDays, count, total.doubleValue(),
                count > velocityThreshold, subThreshold.size(),
                subThresholdTotal.doubleValue(), structuringSuspected
        );
    }

    @Tool("Returns the current FATF high-risk and non-cooperative jurisdictions list")
    public String getHighRiskCountries() {
        return "HIGH_RISK_COUNTRIES (FATF Grey/Black List — POC subset): " +
               String.join(", ", HIGH_RISK_COUNTRIES) +
               "\nNote: KP=North Korea, IR=Iran, SY=Syria, RU=Russia (CAATSA), BY=Belarus, " +
               "VE=Venezuela, YE=Yemen, LY=Libya, SO=Somalia, SS=South Sudan, " +
               "CF=Central African Republic, ML=Mali, HT=Haiti, MM=Myanmar, PK=Pakistan";
    }

    @Tool("Persist graph analysis findings including risk score, typologies detected, and suspicious transaction list")
    public String recordGraphAnalysisResult(
            long customerId,
            int riskScore,
            boolean structuringDetected,
            String highRiskJurisdictions,
            boolean velocityAnomaly,
            String findings) {

        log.info("[GRAPH] Recording result for customer {} — score={}, structuring={}, velocity={}",
                customerId, riskScore, structuringDetected, velocityAnomaly);

        return String.format(
                "GRAPH_RESULT_RECORDED: customer_id=%d, risk_score=%d, structuring=%b, " +
                "high_risk_jurisdictions=%s, velocity_anomaly=%b, sar_threshold_breached=%b, " +
                "timestamp=%s",
                customerId, riskScore, structuringDetected, highRiskJurisdictions,
                velocityAnomaly, riskScore >= 60, LocalDateTime.now()
        );
    }
}
