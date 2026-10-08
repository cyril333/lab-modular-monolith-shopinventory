package edu.cit.antolijao.channel;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

@Entity
@DynamicUpdate
@Table(name = "tiangge_orders")
public class TiangeOrder {

    @Id
    @Column(name = "tiangge_order_id")
    private String tiangeOrderId;

    @Column(name = "shop_order_id")
    private Long shopOrderId;

    @Column(name = "decision")
    private String decision;

    @Column(name = "decision_sent", nullable = false)
    private boolean decisionSent = false;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "resolution_sent", nullable = false)
    private boolean resolutionSent = false;

    @Column(name = "cancel_requested", nullable = false)
    private boolean cancelRequested = false;

    @Column(name = "cancel_confirmed", nullable = false)
    private boolean cancelConfirmed = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public TiangeOrder() {}

    public TiangeOrder(String tiangeOrderId) {
        this.tiangeOrderId = tiangeOrderId;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getTiangeOrderId() { return tiangeOrderId; }
    public Long getShopOrderId() { return shopOrderId; }
    public void setShopOrderId(Long v) { this.shopOrderId = v; touch(); }
    public String getDecision() { return decision; }
    public void setDecision(String v) { this.decision = v; touch(); }
    public boolean isDecisionSent() { return decisionSent; }
    public void setDecisionSent(boolean v) { this.decisionSent = v; touch(); }
    public String getResolution() { return resolution; }
    public void setResolution(String v) { this.resolution = v; touch(); }
    public boolean isResolutionSent() { return resolutionSent; }
    public boolean isCancelRequested() { return cancelRequested; }
    public void setCancelRequested(boolean v) { this.cancelRequested = v; touch(); }
    public boolean isCancelConfirmed() { return cancelConfirmed; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    private void touch() { this.updatedAt = OffsetDateTime.now(); }
}