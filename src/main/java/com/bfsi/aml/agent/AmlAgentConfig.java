package com.bfsi.aml.agent;

import com.bfsi.aml.tools.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.skills.ClassPathSkillLoader;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.Skills;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class AmlAgentConfig {

    // ─────────────────────────────────────────────────────────────────────
    // AML Agent AI Service interface
    // Spring will inject this as a @Service-like bean. Think of it as a
    // standard service interface whose implementation is the LLM.
    // ─────────────────────────────────────────────────────────────────────
    public interface AmlScreeningAgent {
        // System Message provided dynamically
        String screenCustomer(String userMessage);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Build and register the AML Screening Agent bean
    // ─────────────────────────────────────────────────────────────────────
    @Bean
    public AmlScreeningAgent amlScreeningAgent(
            ChatModel chatModel,
            KycParsingTools kycParsingTools,
            SanctionsLookupTools sanctionsLookupTools,
            PepScreeningTools pepScreeningTools,
            GraphAnalysisTools graphAnalysisTools,
            SarGenerationTools sarGenerationTools) {

        // ── Load SKILL.md files from classpath (src/main/resources/skills/) ──
        List<FileSystemSkill> rawSkills = ClassPathSkillLoader.loadSkills("skills");
        log.info("Loaded {} skills from classpath: {}",
                rawSkills.size(),
                rawSkills.stream().map(FileSystemSkill::name).toList());

        // ── Attach scoped tools to each skill ─────────────────────────────
        // Tools are ONLY visible to the LLM AFTER it activates the skill.
        // This keeps the tool list small and focused at each step.
        List<FileSystemSkill> skillsWithTools = rawSkills.stream()
                .map(skill -> {
                    Object toolBean = switch (skill.name()) {
                        case "kyc-parsing" -> kycParsingTools;
                        case "sanctions-lookup" -> sanctionsLookupTools;
                        case "pep-screening" -> pepScreeningTools;
                        case "graph-analysis" -> graphAnalysisTools;
                        case "sar-generation" -> sarGenerationTools;
                        default -> null;
                    };
                    if (toolBean == null) {
                        log.warn("Unknown skill: {} - no tools attached ", skill.name());
                        return skill;
                    }
                    return FileSystemSkill.builder()
                            .name(skill.name())
                            .description(skill.description())
                            .content(skill.content())
                            .basePath(skill.basePath())
                            .tools(toolBean)
                            .build();
                })
                .toList();

        // ── Build the Skills wrapper ───────────────────────────────────────
        Skills skills = Skills.from(skillsWithTools);

        // ── Wire it all into an AiService ─────────────────────────────────
        return AiServices.builder(AmlScreeningAgent.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(100))
                .toolProvider(skills.toolProvider())
                .systemMessageProvider(memoryId ->
                        // Inject the available skills catalogue into the system message
                        buildSystemMessage(skills.formatAvailableSkills()))
                .build();
    }

    private String buildSystemMessage(String availableSkills) {
        return """
            You are an AML (Anti-Money Laundering) Compliance Agent for a BFSI institution.
            Your job is to perform a complete AML screening for a customer and produce findings.

            You have access to the following skills:
            """ + availableSkills + """

            MANDATORY WORKFLOW — execute ALL steps in order:
            1. Activate `kyc-parsing` skill first — parse and validate the customer's identity document.
            2. Activate `sanctions-lookup` skill — screen customer and counterparties against sanctions lists.
            3. Activate `pep-screening` skill — check if the customer is a Politically Exposed Person.
            4. Activate `graph-analysis` skill — analyse transaction patterns for money laundering typologies.
            5. IF any of steps 2, 3, or 4 produce HIGH risk OR graph risk_score >= 60,
               THEN activate `sar-generation` skill to produce a Suspicious Activity Report.

            Always call activate_skill before calling any skill-scoped tool.
            Be thorough, precise, and cite specific tool results in your findings.
            """;
    }
}
