package com.bfsi.aml.tools;

import com.bfsi.aml.model.SanctionsEntity;
import com.bfsi.aml.repository.SanctionsEntityRepository;
import com.bfsi.aml.repository.TransactionRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SanctionsLookupTools {

    private final SanctionsEntityRepository sanctionsRepo;
    private final TransactionRepository transactionRepo;

    @Tool("Search the sanctions list (OFAC/UN) for a given name. Returns potential matches with similarity scores (0-100).")
    public String searchSanctionsList(String nameToSearch) {
        log.info("[SANCTIONS] Searching sanctions list for: {}", nameToSearch);
        List<SanctionsEntity> hits = sanctionsRepo.searchByName(nameToSearch);

        if (hits.isEmpty()) {
            return "SANCTIONS_SEARCH: name=" + nameToSearch + " | matches=NONE";
        }

        StringBuilder sb = new StringBuilder("SANCTIONS_SEARCH: name=" + nameToSearch + "\n");
        for (SanctionsEntity hit : hits) {
            int score = calculateFuzzyScore(nameToSearch, hit.getFullName(), hit.getAliases());
            sb.append(String.format(
                    "  MATCH: entity=%s | aliases=%s | country=%s | program=%s | " +
                    "entity_type=%s | listed_date=%s | match_score=%d\n",
                    hit.getFullName(), hit.getAliases(), hit.getCountry(),
                    hit.getProgram(), hit.getEntityType(), hit.getListedDate(), score
            ));
        }
        return sb.toString();
    }

    @Tool("Retrieve all counterparty names from recent transactions for a customer, so they can each be screened against sanctions")
    public String getTransactionCounterparties(long customerId) {
        log.info("[SANCTIONS] Fetching counterparties for customer {}", customerId);
        List<String> counterparties = transactionRepo
                .findRecentByCustomerId(customerId, LocalDateTime.now().minusDays(90))
                .stream()
                .map(t -> t.getCounterparty() + " (country=" + t.getCounterpartyCountry() + ")")
                .distinct()
                .collect(Collectors.toList());

        return counterparties.isEmpty()
                ? "No recent transactions found for customer " + customerId
                : "COUNTERPARTIES:\n" + String.join("\n", counterparties);
    }

    @Tool("Persist the sanctions screening result for a customer")
    public String recordSanctionsResult(
            long customerId,
            boolean matchFound,
            String matchedEntityName,
            int matchScore,
            String sanctionsProgram) {

        log.info("[SANCTIONS] Recording result for customer {} — match={}, score={}, program={}",
                customerId, matchFound, matchScore, sanctionsProgram);

        return String.format(
                "SANCTIONS_RESULT_RECORDED: customer_id=%d, match_found=%b, matched_entity=%s, " +
                "match_score=%d, program=%s, timestamp=%s",
                customerId, matchFound, matchedEntityName, matchScore, sanctionsProgram, LocalDateTime.now()
        );
    }

    // Simple fuzzy scoring: exact = 100, contains = 85, partial token overlap proportional
    private int calculateFuzzyScore(String query, String target, String aliases) {
        String q = query.toUpperCase().trim();
        String t = target.toUpperCase().trim();
        if (q.equals(t)) return 100;
        if (t.contains(q) || q.contains(t)) return 90;
        if (aliases != null) {
            for (String alias : aliases.split("\\|")) {
                String a = alias.toUpperCase().trim();
                if (q.equals(a)) return 95;
                if (a.contains(q) || q.contains(a)) return 85;
            }
        }
        // Token overlap
        String[] qTokens = q.split("\\s+");
        String[] tTokens = t.split("\\s+");
        long overlap = 0;
        for (String qt : qTokens) {
            for (String tt : tTokens) {
                if (qt.equals(tt)) overlap++;
            }
        }
        int maxTokens = Math.max(qTokens.length, tTokens.length);
        return maxTokens == 0 ? 0 : (int) ((overlap * 100.0) / maxTokens);
    }
}
