package edu.cit.antolijao.channel;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tiangge_orders")
public class TiangeOrder {

    @Id
    @Column(name = "tiangge_order_id")
    private String tiangeOrderId;

    @Column(name = "shop_order_id")
    private Long shopOrderId;

    @Column(name = "decision")
    private String decision;

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
    public void setShopOrderId(Long shopOrderId) { this.shopOrderId = shopOrderId; touch(); }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; touch(); }
    public boolean isCancelConfirmed() { return cancelConfirmed; }
    public void setCancelConfirmed(boolean cancelConfirmed) { this.cancelConfirmed = cancelConfirmed; touch(); }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    private void touch() { this.updatedAt = OffsetDateTime.now(); }
}