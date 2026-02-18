package com.magiconcall.domain.incident;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "evidence")
public class Evidence extends BaseEntity {

    @Column(nullable = false)
    private UUID incidentId;

    private UUID hypothesisId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceType evidenceType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    private String sourceUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    protected Evidence() {}

    public Evidence(UUID incidentId, UUID hypothesisId, EvidenceType evidenceType,
                    String title, String content, String sourceUrl, String metadata) {
        this.incidentId = incidentId;
        this.hypothesisId = hypothesisId;
        this.evidenceType = evidenceType;
        this.title = title;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.metadata = metadata;
    }

    public UUID getIncidentId() { return incidentId; }
    public UUID getHypothesisId() { return hypothesisId; }
    public EvidenceType getEvidenceType() { return evidenceType; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSourceUrl() { return sourceUrl; }
    public String getMetadata() { return metadata; }
}
