package com.magiconcall.application.triage;

import com.magiconcall.domain.incident.Evidence;
import com.magiconcall.domain.incident.EvidenceRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class EvidenceSummarizer {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final EvidenceRepository evidenceRepository;

    public EvidenceSummarizer(EvidenceRepository evidenceRepository) {
        this.evidenceRepository = evidenceRepository;
    }

    public EvidenceSummary summarize(UUID incidentId) {
        var evidenceList = evidenceRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId);

        var items = evidenceList.stream()
            .map(e -> new EvidenceSummary.EvidenceItem(
                e.getEvidenceType().name(),
                e.getTitle(),
                truncate(e.getContent())
            ))
            .toList();

        var formatted = formatForLlm(items);
        var hash = sha256(formatted);

        return new EvidenceSummary(items, formatted, hash);
    }

    private String formatForLlm(List<EvidenceSummary.EvidenceItem> items) {
        if (items.isEmpty()) {
            return "No evidence collected yet.";
        }

        // Group by type for structured presentation
        var grouped = items.stream()
            .collect(Collectors.groupingBy(
                EvidenceSummary.EvidenceItem::type,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        var sb = new StringBuilder();
        for (var entry : grouped.entrySet()) {
            sb.append("## ").append(entry.getKey()).append("\n");
            for (var item : entry.getValue()) {
                sb.append("- **").append(item.title()).append("**: ");
                sb.append(item.content() != null ? item.content() : "(no content)");
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String truncate(String content) {
        if (content == null) return null;
        if (content.length() <= MAX_CONTENT_LENGTH) return content;
        return content.substring(0, MAX_CONTENT_LENGTH) + "... [truncated]";
    }

    static String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
