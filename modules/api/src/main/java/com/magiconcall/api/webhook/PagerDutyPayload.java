package com.magiconcall.api.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * PagerDuty V2 webhook payload DTOs.
 * See: https://developer.pagerduty.com/docs/webhooks/v2-overview/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagerDutyPayload(
    @JsonProperty("messages") List<Message> messages
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
        @JsonProperty("event") String event,
        @JsonProperty("incident") Incident incident
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Incident(
        @JsonProperty("id") String id,
        @JsonProperty("incident_number") int incidentNumber,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("status") String status,
        @JsonProperty("urgency") String urgency,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("service") Service service,
        @JsonProperty("alert_counts") AlertCounts alertCounts,
        @JsonProperty("incident_key") String incidentKey
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Service(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AlertCounts(
        @JsonProperty("triggered") int triggered,
        @JsonProperty("acknowledged") int acknowledged,
        @JsonProperty("resolved") int resolved
    ) {}
}
