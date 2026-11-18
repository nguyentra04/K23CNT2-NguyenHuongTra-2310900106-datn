package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.ReservationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationEventRepository extends JpaRepository<ReservationEvent, Long> {
    List<ReservationEvent> findByReservationIdOrderByCreatedAtAsc(Long reservationId);
}
