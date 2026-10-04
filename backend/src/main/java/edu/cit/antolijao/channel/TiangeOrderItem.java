package edu.cit.antolijao.channel;

import jakarta.persistence.*;

@Entity
@Table(name = "tiangge_order_items")
public class TiangeOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tiangge_order_id", nullable = false)
    private String tiangeOrderId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    public TiangeOrderItem() {}

    public TiangeOrderItem(String tiangeOrderId, String productId, Integer qty) {
        this.tiangeOrderId = tiangeOrderId;
        this.productId = productId;
        this.qty = qty;
    }

    public Long getId() { return id; }
    public String getTiangeOrderId() { return tiangeOrderId; }
    public String getProductId() { return productId; }
    public Integer getQty() { return qty; }
}