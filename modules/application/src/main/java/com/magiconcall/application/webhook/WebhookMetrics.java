package com.magiconcall.application.webhook;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class WebhookMetrics {

    private final Counter alertsIngested;
    private final Counter incidentsCreated;
    private final Counter dedupHit;
    private final Counter dedupMiss;

    public WebhookMetrics(MeterRegistry meterRegistry) {
        this.alertsIngested = Counter.builder("moc_alerts_ingested_total")
            .description("Total alerts ingested via webhooks")
            .tag("source", "pagerduty")
            .register(meterRegistry);

        this.incidentsCreated = Counter.builder("moc_incidents_created_total")
            .description("Total incidents auto-created from webhook alerts")
            .tag("source", "pagerduty")
            .register(meterRegistry);

        this.dedupHit = Counter.builder("moc_webhook_dedup_total")
            .description("Webhook deduplication outcomes")
            .tag("outcome", "hit")
            .register(meterRegistry);

        this.dedupMiss = Counter.builder("moc_webhook_dedup_total")
            .description("Webhook deduplication outcomes")
            .tag("outcome", "miss")
            .register(meterRegistry);
    }

    public void recordAlertIngested() { alertsIngested.increment(); }
    public void recordIncidentCreated() { incidentsCreated.increment(); }
    public void recordDedupHit() { dedupHit.increment(); }
    public void recordDedupMiss() { dedupMiss.increment(); }
}
