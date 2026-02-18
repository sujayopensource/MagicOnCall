package com.magiconcall.api.webhook;

import com.magiconcall.application.webhook.WebhookIngestionService;
import com.magiconcall.application.webhook.WebhookIngestionService.WebhookResult;
import com.magiconcall.domain.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/{tenantId}/pagerduty")
public class PagerDutyWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PagerDutyWebhookController.class);

    private final PagerDutyNormalizer normalizer;
    private final WebhookIngestionService ingestionService;

    public PagerDutyWebhookController(PagerDutyNormalizer normalizer,
                                       WebhookIngestionService ingestionService) {
        this.normalizer = normalizer;
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String tenantId,
            @RequestBody PagerDutyPayload payload) {

        if (payload.messages() == null || payload.messages().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "INVALID_PAYLOAD", "message", "No messages in payload"));
        }

        log.info("Received PagerDuty webhook: tenantId={}, messages={}",
            tenantId, payload.messages().size());

        // Set tenant context for this request (webhooks bypass TenantFilter)
        TenantContext.setTenantId(tenantId);
        try {
            List<Map<String, Object>> results = new ArrayList<>();
            for (var message : payload.messages()) {
                if (message.incident() == null) {
                    log.warn("Skipping PD message with null incident: event={}", message.event());
                    continue;
                }

                var normalized = normalizer.normalize(message);
                WebhookResult result = ingestionService.ingest(tenantId, normalized);

                results.add(Map.of(
                    "alertId", result.alertId().toString(),
                    "incidentId", result.incidentId().toString(),
                    "newIncident", result.newIncident(),
                    "deduplicated", result.deduplicated()
                ));
            }

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                    "status", "accepted",
                    "processed", results.size(),
                    "results", results,
                    "timestamp", Instant.now().toString()
                ));
        } finally {
            TenantContext.clear();
        }
    }
}
