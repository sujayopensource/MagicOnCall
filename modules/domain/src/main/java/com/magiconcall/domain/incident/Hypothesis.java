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

    @Column(columnDefinition = "text")
    private String evidenceFor;

    @Column(columnDefinition = "text")
    private String evidenceAgainst;

    @Column(columnDefinition = "text")
    private String nextBestTest;

    @Column(columnDefinition = "text")
    private String stopCondition;

    @Column(length = 64)
    private String evidenceHash;

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

    public String getEvidenceFor() { return evidenceFor; }
    public void setEvidenceFor(String evidenceFor) { this.evidenceFor = evidenceFor; }

    public String getEvidenceAgainst() { return evidenceAgainst; }
    public void setEvidenceAgainst(String evidenceAgainst) { this.evidenceAgainst = evidenceAgainst; }

    public String getNextBestTest() { return nextBestTest; }
    public void setNextBestTest(String nextBestTest) { this.nextBestTest = nextBestTest; }

    public String getStopCondition() { return stopCondition; }
    public void setStopCondition(String stopCondition) { this.stopCondition = stopCondition; }

    public String getEvidenceHash() { return evidenceHash; }
    public void setEvidenceHash(String evidenceHash) { this.evidenceHash = evidenceHash; }
}
