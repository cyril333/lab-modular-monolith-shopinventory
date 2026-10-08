package edu.cit.antolijao.channel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TiangeOrderRepository extends JpaRepository<TiangeOrder, String> {

    Optional<TiangeOrder> findByShopOrderId(Long shopOrderId);

    List<TiangeOrder> findByDecision(String decision);

    @Query("select o from TiangeOrder o where o.decisionSent = false "
            + "or (o.resolution is not null and o.resolutionSent = false) "
            + "or (o.cancelRequested = true and o.cancelConfirmed = false)")
    List<TiangeOrder> findPendingSends();

    @Modifying
    @Transactional
    @Query("update TiangeOrder o set o.decisionSent = true where o.tiangeOrderId = :id")
    void markDecisionSent(@Param("id") String id);

    @Modifying
    @Transactional
    @Query("update TiangeOrder o set o.resolutionSent = true where o.tiangeOrderId = :id")
    void markResolutionSent(@Param("id") String id);

    @Modifying
    @Transactional
    @Query("update TiangeOrder o set o.cancelConfirmed = true where o.tiangeOrderId = :id")
    void markCancelConfirmed(@Param("id") String id);

    @Modifying
    @Transactional
    @Query("update TiangeOrder o set o.decisionSent = true, o.resolutionSent = true, "
            + "o.cancelConfirmed = o.cancelRequested where o.tiangeOrderId = :id")
    void markAllSent(@Param("id") String id);
}