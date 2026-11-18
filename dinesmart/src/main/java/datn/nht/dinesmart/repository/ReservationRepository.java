package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.Reservation;
import datn.nht.dinesmart.entity.ReservationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = "table")
    Optional<Reservation> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "table")
    List<Reservation> findByReservationDateOrderByStartTimeAsc(LocalDate date);

    @EntityGraph(attributePaths = "table")
    List<Reservation> findByReservationDateAndStatusOrderByStartTimeAsc(LocalDate date, ReservationStatus status);

    @EntityGraph(attributePaths = "table")
    List<Reservation> findByStatusOrderByReservationDateAscStartTimeAsc(ReservationStatus status, Pageable page);

    @EntityGraph(attributePaths = "table")
    List<Reservation> findByUserIdOrderByReservationDateDescStartTimeDesc(Long userId);

    /** Kiểm tra trùng giờ (sau khi đã khoá dòng bàn). excludeId = 0 nếu không loại trừ. */
    @Query("""
            select count(r) > 0 from Reservation r
            where r.table.id = :tableId and r.reservationDate = :date
              and r.status in :busy
              and r.startTime < :endLimit and r.endTime > :startLimit
              and r.id <> :excludeId
            """)
    boolean existsOverlap(@Param("tableId") Long tableId,
                          @Param("date") LocalDate date,
                          @Param("busy") Collection<ReservationStatus> busy,
                          @Param("startLimit") LocalTime startLimit,
                          @Param("endLimit") LocalTime endLimit,
                          @Param("excludeId") Long excludeId);

    /** Bàn còn đặt chỗ khác chưa kết thúc trong ngày (dùng để quyết định có nhả trạng thái RESERVED không). */
    @Query("""
            select count(r) > 0 from Reservation r
            where r.table.id = :tableId and r.reservationDate = :date
              and r.status in :statuses and r.endTime > :now and r.id <> :excludeId
            """)
    boolean existsOngoing(@Param("tableId") Long tableId, @Param("date") LocalDate date,
                          @Param("statuses") Collection<ReservationStatus> statuses,
                          @Param("now") LocalTime now, @Param("excludeId") Long excludeId);

    @Query("""
            select r.status, count(r) from Reservation r
            where r.reservationDate >= :from and r.reservationDate < :to
              and r.status in :finals
            group by r.status
            """)
    List<Object[]> countFinalByStatus(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                      @Param("finals") Collection<ReservationStatus> finals);

    @Query("""
            select e.action, count(e) from ReservationEvent e, Reservation r
            where r.id = e.reservationId
              and r.reservationDate >= :from and r.reservationDate < :to
              and r.status in :finals
              and e.action in :actions
            group by e.action
            """)
    List<Object[]> countCancelActions(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                      @Param("finals") Collection<ReservationStatus> finals,
                                      @Param("actions") Collection<datn.nht.dinesmart.entity.ReservationAction> actions);
}
