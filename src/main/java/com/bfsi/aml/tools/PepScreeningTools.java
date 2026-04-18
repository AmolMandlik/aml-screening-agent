package com.bfsi.aml.tools;

import com.bfsi.aml.model.PepEntity;
import com.bfsi.aml.model.Transaction;
import com.bfsi.aml.repository.PepEntityRepository;
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
public class PepScreeningTools {

    private final PepEntityRepository pepRepo;
    private final TransactionRepository transactionRepo;

    // FATF grey/black list — simplified for POC
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "KP", "IR", "SY", "YE", "LY", "SO", "SS", "CF", "ML", "HT",
            "MM", "PK", "RU", "BY", "VE"
    );

    @Value("${aml.agent.pep.enabled:true}")
    private boolean pepEnabled;

    @Tool("Search the PEP (Politically Exposed Persons) registry for a customer name. Returns PEP status, position, country and risk level.")
    public String searchPepRegistry(String customerName) {
        log.info("[PEP] Searching PEP registry for: {}", customerName);
        if (!pepEnabled) return "PEP screening is disabled in configuration.";

        List<PepEntity> results = pepRepo.searchByName(customerName);
        if (results.isEmpty()) {
            return "PEP_SEARCH: name=" + customerName + " | result=NO_MATCH";
        }

        StringBuilder sb = new StringBuilder("PEP_SEARCH: name=" + customerName + "\n");
        for (PepEntity pep : results) {
            sb.append(String.format(
                    "  PEP_MATCH: name=%s | country=%s | position=%s | risk_level=%s | active_since=%s\n",
                    pep.getFullName(), pep.getCountry(), pep.getPosition(),
                    pep.getRiskLevel(), pep.getActiveSince()
            ));
        }
        return sb.toString();
    }

    @Tool("Check Enhanced Due Diligence triggers for a customer — high-value transactions, high-risk jurisdiction flows, cash intensity")
    public String checkEddTriggers(long customerId) {
        log.info("[PEP] Checking EDD triggers for customer {}", customerId);

        List<Transaction> recent = transactionRepo
                .findRecentByCustomerId(customerId, LocalDateTime.now().minusDays(90));

        if (recent.isEmpty()) {
            return "EDD_TRIGGERS: customer_id=" + customerId + " | no_recent_transactions=true | edd_required=false";
        }

        long highValueCount = recent.stream()
                .filter(t -> t.getAmount().compareTo(new BigDecimal("10000")) > 0)
                .count();

        List<String> highRiskCountryTxns = recent.stream()
                .filter(t -> HIGH_RISK_COUNTRIES.contains(t.getCounterpartyCountry()))
                .map(t -> t.getCounterparty() + " (" + t.getCounterpartyCountry() + ") — "
                          + t.getAmount() + " " + t.getCurrency())
                .collect(Collectors.toList());

        long cashTxns = recent.stream()
                .filter(t -> Transaction.TransactionType.CASH_DEPOSIT.equals(t.getTransactionType()))
                .count();

        boolean eddRequired = highValueCount > 0 || !highRiskCountryTxns.isEmpty() || cashTxns > 2;

        return String.format(
                "EDD_TRIGGERS: customer_id=%d | high_value_transactions=%d | " +
                "high_risk_country_transactions=%s | cash_transactions=%d | edd_required=%b",
                customerId, highValueCount,
                highRiskCountryTxns.isEmpty() ? "NONE" : String.join("; ", highRiskCountryTxns),
                cashTxns, eddRequired
        );
    }

    @Tool("Persist the PEP screening result for a customer")
    public String recordPepResult(
            long customerId,
            boolean isPep,
            String pepCategory,
            String riskLevel,
            boolean eddRequired) {

        log.info("[PEP] Recording result for customer {} — isPep={}, risk={}, edd={}",
                customerId, isPep, riskLevel, eddRequired);

        return String.format(
                "PEP_RESULT_RECORDED: customer_id=%d, is_pep=%b, pep_category=%s, " +
                "risk_level=%s, edd_required=%b, timestamp=%s",
                customerId, isPep, pepCategory, riskLevel, eddRequired, LocalDateTime.now()
        );
    }
}
