package com.magiconcall.domain.incident;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "hypotheses")
public class Hypothesis extends BaseEntity {

    @Column(nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HypothesisStatus status;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false)
    private String source;

    protected Hypothesis() {}

    public Hypothesis(UUID incidentId, String title, String description,
                      double confidence, String source) {
        this.incidentId = incidentId;
        this.title = title;
        this.description = description;
        this.status = HypothesisStatus.PROPOSED;
        this.confidence = confidence;
        this.source = source;
    }

    public void startInvestigating() { this.status = HypothesisStatus.INVESTIGATING; }
    public void confirm() { this.status = HypothesisStatus.CONFIRMED; }
    public void reject() { this.status = HypothesisStatus.REJECTED; }

    public UUID getIncidentId() { return incidentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public HypothesisStatus getStatus() { return status; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getSource() { return source; }
}
