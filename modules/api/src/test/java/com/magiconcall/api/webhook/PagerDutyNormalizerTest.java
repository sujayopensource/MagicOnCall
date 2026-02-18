package com.magiconcall.api.webhook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PagerDutyNormalizerTest {

    private final PagerDutyNormalizer normalizer = new PagerDutyNormalizer();

    @Test
    @DisplayName("normalizes high-urgency PD incident to CRITICAL / SEV1")
    void highUrgency() {
        var message = buildMessage("incident.trigger", "high", "P12345", "SVC001", "DB down", "db-dedup-key");

        var result = normalizer.normalize(message);

        assertThat(result.dedupKey()).isEqualTo("pd:SVC001:db-dedup-key");
        assertThat(result.title()).isEqualTo("DB down");
        assertThat(result.source()).isEqualTo("pagerduty");
        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.incidentExternalId()).isEqualTo("pd-incident:P12345");
        assertThat(result.incidentTitle()).isEqualTo("[PD] DB down");
        assertThat(result.incidentSeverity()).isEqualTo("SEV1");
        assertThat(result.labels()).contains("\"pd_service_id\":\"SVC001\"");
    }

    @Test
    @DisplayName("normalizes low-urgency PD incident to WARNING / SEV3")
    void lowUrgency() {
        var message = buildMessage("incident.trigger", "low", "P99999", "SVC002", "Disk warning", "disk-key");

        var result = normalizer.normalize(message);

        assertThat(result.severity()).isEqualTo("WARNING");
        assertThat(result.incidentSeverity()).isEqualTo("SEV3");
    }

    @Test
    @DisplayName("uses incident ID as dedup key fallback when incidentKey is null")
    void nullIncidentKey() {
        var service = new PagerDutyPayload.Service("SVC003", "payments", null);
        var incident = new PagerDutyPayload.Incident(
            "P55555", 55, "Payment timeout", null, "triggered", "high",
            null, service, null, null  // incidentKey = null
        );
        var message = new PagerDutyPayload.Message("incident.trigger", incident);

        var result = normalizer.normalize(message);

        assertThat(result.dedupKey()).isEqualTo("pd:SVC003:P55555");
    }

    @Test
    @DisplayName("handles null urgency gracefully")
    void nullUrgency() {
        var message = buildMessage("incident.trigger", null, "P11111", "SVC004", "Unknown urgency", "key-1");

        var result = normalizer.normalize(message);

        assertThat(result.severity()).isEqualTo("WARNING");
        assertThat(result.incidentSeverity()).isEqualTo("SEV3");
    }

    @Test
    @DisplayName("handles null service gracefully")
    void nullService() {
        var incident = new PagerDutyPayload.Incident(
            "P77777", 77, "No service", null, "triggered", "high",
            null, null, null, "some-key"
        );
        var message = new PagerDutyPayload.Message("incident.trigger", incident);

        var result = normalizer.normalize(message);

        assertThat(result.dedupKey()).isEqualTo("pd:unknown:some-key");
        assertThat(result.labels()).contains("\"source\":\"pagerduty\"");
    }

    private PagerDutyPayload.Message buildMessage(String event, String urgency,
                                                    String incidentId, String serviceId,
                                                    String title, String incidentKey) {
        var service = new PagerDutyPayload.Service(serviceId, "test-service", null);
        var incident = new PagerDutyPayload.Incident(
            incidentId, 1, title, "Test description", "triggered",
            urgency, "https://pd.com/incidents/" + incidentId,
            service, new PagerDutyPayload.AlertCounts(1, 0, 0), incidentKey
        );
        return new PagerDutyPayload.Message(event, incident);
    }
}
