package edu.cit.antolijao.channel;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TiangeOrderRepository extends JpaRepository<TiangeOrder, String> {
    Optional<TiangeOrder> findByShopOrderId(Long shopOrderId);
}