package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.DiningOrder;
import datn.nht.dinesmart.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DiningOrderRepository extends JpaRepository<DiningOrder, Long> {

    @EntityGraph(attributePaths = "table")
    List<DiningOrder> findByStatusInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

    @EntityGraph(attributePaths = "table")
    Optional<DiningOrder> findWithTableById(Long id);

    Optional<DiningOrder> findFirstByTableIdAndStatusIn(Long tableId, Collection<OrderStatus> statuses);

    Optional<DiningOrder> findFirstByReservationIdAndStatusNot(Long reservationId, OrderStatus status);
}
