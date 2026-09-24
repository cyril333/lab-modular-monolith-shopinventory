package edu.cit.antolijao.supplier;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "supplier_orders")
public class SupplierOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "buyer_ref", nullable = false, unique = true)
    private String buyerRef;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "po_number")
    private String poNumber;

    @Column(name = "cases")
    private Integer cases;

    @Column(name = "units", nullable = false)
    private Integer units;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupplierOrderStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public SupplierOrder() {}

    public SupplierOrder(String productId, String buyerRef, String requestId, Integer units, SupplierOrderStatus status) {
        this.productId = productId;
        this.buyerRef = buyerRef;
        this.requestId = requestId;
        this.units = units;
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public String getBuyerRef() { return buyerRef; }
    public String getRequestId() { return requestId; }
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public Integer getCases() { return cases; }
    public void setCases(Integer cases) { this.cases = cases; }
    public Integer getUnits() { return units; }
    public SupplierOrderStatus getStatus() { return status; }
    public void setStatus(SupplierOrderStatus status) { this.status = status; this.updatedAt = OffsetDateTime.now(); }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}