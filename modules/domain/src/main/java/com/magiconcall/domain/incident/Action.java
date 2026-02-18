package com.magiconcall.domain.incident;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "actions")
public class Action extends BaseEntity {

    public enum ActionStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED }
    public enum ActionType { MANUAL, AUTOMATED, AI_SUGGESTED }

    @Column(nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    private String assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    protected Action() {}

    public Action(UUID incidentId, String title, String description,
                  ActionType actionType, String assignee) {
        this.incidentId = incidentId;
        this.title = title;
        this.description = description;
        this.actionType = actionType;
        this.status = ActionStatus.PENDING;
        this.assignee = assignee;
    }

    public void start() { this.status = ActionStatus.IN_PROGRESS; }
    public void complete() { this.status = ActionStatus.COMPLETED; }
    public void fail() { this.status = ActionStatus.FAILED; }
    public void skip() { this.status = ActionStatus.SKIPPED; }

    public UUID getIncidentId() { return incidentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ActionStatus getStatus() { return status; }
    public String getAssignee() { return assignee; }
    public ActionType getActionType() { return actionType; }
}
