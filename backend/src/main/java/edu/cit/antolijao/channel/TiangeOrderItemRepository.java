package edu.cit.antolijao.channel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TiangeOrderItemRepository extends JpaRepository<TiangeOrderItem, Long> {
    List<TiangeOrderItem> findByTiangeOrderId(String tiangeOrderId);
    List<TiangeOrderItem> findByProductId(String productId);
}