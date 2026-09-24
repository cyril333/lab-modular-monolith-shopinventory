package edu.cit.antolijao.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long> {
    Optional<SupplierOrder> findByBuyerRef(String buyerRef);
    Optional<SupplierOrder> findByRequestId(String requestId);
    List<SupplierOrder> findByStatus(SupplierOrderStatus status);
    List<SupplierOrder> findByStatusIn(List<SupplierOrderStatus> statuses);
}