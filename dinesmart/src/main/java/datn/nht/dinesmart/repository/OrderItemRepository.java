package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.OrderItem;
import datn.nht.dinesmart.entity.OrderItemStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = "menuItem")
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    @Query("select oi from OrderItem oi join fetch oi.menuItem join fetch oi.order o left join fetch o.table where oi.id = :id")
    Optional<OrderItem> findDetailById(@Param("id") Long id);

    /** Hàng đợi màn hình bếp (FR 13). */
    @Query("""
            select oi from OrderItem oi
            join fetch oi.menuItem join fetch oi.order o left join fetch o.table
            where oi.status in :statuses
            order by oi.createdAt asc
            """)
    List<OrderItem> findQueue(@Param("statuses") Collection<OrderItemStatus> statuses);

    @Query("select coalesce(sum(oi.quantity * oi.unitPrice), 0) from OrderItem oi where oi.order.id = :orderId and oi.status <> :cancelled")
    BigDecimal sumActive(@Param("orderId") Long orderId, @Param("cancelled") OrderItemStatus cancelled);

    long countByOrderIdAndStatusNot(Long orderId, OrderItemStatus status);
}
