package com.magiconcall.application.triage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.application.incident.AddHypothesisCommand;
import com.magiconcall.application.incident.HypothesisResult;
import com.magiconcall.application.incident.IncidentService;
import com.magiconcall.domain.incident.IncidentEvent;
import com.magiconcall.domain.incident.IncidentEventRepository;
import com.magiconcall.domain.incident.IncidentRepository;
import com.magiconcall.domain.llm.LlmClient;
import com.magiconcall.domain.llm.LlmRequest;
import com.magiconcall.domain.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private static final String SYSTEM_PROMPT = """
        You are an expert incident responder. Given the evidence collected so far for an incident,
        generate a JSON array of hypothesis objects. Each hypothesis should have:
        - title: concise name for the hypothesis
        - description: detailed explanation
        - confidence: a number between 0.0 and 1.0
        - evidenceFor: evidence supporting this hypothesis
        - evidenceAgainst: evidence contradicting this hypothesis
        - nextBestTest: the single best action to confirm or refute this hypothesis
        - stopCondition: the measurable condition that confirms the issue is resolved

        Return ONLY a valid JSON array, no markdown fences or extra text.
        """;

    private static final int CHARS_PER_TOKEN = 4;

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final IncidentService incidentService;
    private final EvidenceSummarizer evidenceSummarizer;
    private final LlmClient llmClient;
    private final TriageMetrics triageMetrics;
    private final ObjectMapper objectMapper;
    private final int tokenBudget;

    // In-memory cache: evidenceHash → list of TriageResult
    private final ConcurrentHashMap<String, TriageResult> cache = new ConcurrentHashMap<>();

    public TriageService(IncidentRepository incidentRepository,
                         IncidentEventRepository incidentEventRepository,
                         IncidentService incidentService,
                         EvidenceSummarizer evidenceSummarizer,
                         LlmClient llmClient,
                         TriageMetrics triageMetrics,
                         ObjectMapper objectMapper,
                         @Value("${magiconcall.llm.token-budget:4096}") int tokenBudget) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.incidentService = incidentService;
        this.evidenceSummarizer = evidenceSummarizer;
        this.llmClient = llmClient;
        this.triageMetrics = triageMetrics;
        this.objectMapper = objectMapper;
        this.tokenBudget = tokenBudget;
    }

    @Transactional
    public TriageResult triage(UUID incidentId) {
        var incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentService.IncidentNotFoundException(incidentId));

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            // 1. Summarize evidence
            var summary = evidenceSummarizer.summarize(incidentId);
            log.info("Evidence summarized: {} items, hash={}", summary.items().size(), summary.evidenceHash());

            // 2. Cache check
            var cached = cache.get(summary.evidenceHash());
            if (cached != null) {
                log.info("Cache hit for evidence hash={}", summary.evidenceHash());
                triageMetrics.recordTriageRun(true);
                return new TriageResult(
                    incidentId, cached.hypotheses(), cached.evidenceHash(),
                    0, true, Instant.now()
                );
            }

            // 3. Budget check
            int estimatedTokens = (SYSTEM_PROMPT.length() + summary.formattedSummary().length()) / CHARS_PER_TOKEN;
            if (estimatedTokens > tokenBudget) {
                log.warn("Token budget exceeded: estimated={}, budget={}", estimatedTokens, tokenBudget);
                triageMetrics.recordBudgetExceeded();
                throw new TokenBudgetExceededException(estimatedTokens, tokenBudget);
            }

            // 4. LLM call
            var request = new LlmRequest(SYSTEM_PROMPT, summary.formattedSummary(), tokenBudget, null);
            var response = llmClient.complete(request);
            log.info("LLM response: {} tokens used", response.totalTokens());

            // 5. Parse hypotheses from JSON
            var parsedHypotheses = parseHypotheses(response.content());

            // 6. Persist hypotheses via IncidentService
            var results = new ArrayList<HypothesisResult>();
            for (var h : parsedHypotheses) {
                var command = new AddHypothesisCommand(
                    h.title(), h.description(), h.confidence(), "AI",
                    h.evidenceFor(), h.evidenceAgainst(),
                    h.nextBestTest(), h.stopCondition()
                );
                results.add(incidentService.addHypothesis(incidentId, command));
            }

            // 7. Timeline event
            var timelineEvent = IncidentEvent.triageCompleted(incidentId, results.size());
            timelineEvent.setTenantId(incident.getTenantId());
            incidentEventRepository.save(timelineEvent);

            // 8. Metrics
            triageMetrics.recordTriageRun(false);
            triageMetrics.recordTokensUsed(response.totalTokens());

            // 9. Cache result
            var result = new TriageResult(
                incidentId, results, summary.evidenceHash(),
                response.totalTokens(), false, Instant.now()
            );
            cache.put(summary.evidenceHash(), result);

            log.info("Triage completed: {} hypotheses generated", results.size());
            return result;
        }
    }

    private List<ParsedHypothesis> parseHypotheses(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response as hypothesis JSON", e);
            return List.of();
        }
    }

    record ParsedHypothesis(
        String title,
        String description,
        double confidence,
        String evidenceFor,
        String evidenceAgainst,
        String nextBestTest,
        String stopCondition
    ) {}
}
