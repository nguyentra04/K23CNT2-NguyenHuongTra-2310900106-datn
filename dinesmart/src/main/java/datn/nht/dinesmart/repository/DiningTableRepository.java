package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.DiningTable;
import datn.nht.dinesmart.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    List<DiningTable> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    /**
     * Khoá dòng bàn (UPDLOCK, ROWLOCK trên SQL Server) — phải chạy TRƯỚC câu kiểm tra trùng giờ
     * (xem ghi chú [P1] trong file SQL: DB bật READ_COMMITTED_SNAPSHOT).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.id = :id")
    Optional<DiningTable> lockById(@Param("id") Long id);

    /** Bàn trống theo best-fit (sức chứa nhỏ nhất đủ chỗ). Tham số "limit" đã cộng/trừ buffer ở tầng service. */
    @Query("""
            select t from DiningTable t
            where t.active = true and t.capacity >= :guests
              and not exists (
                  select 1 from Reservation r
                  where r.table = t and r.reservationDate = :date
                    and r.status in :busy
                    and r.startTime < :endPlusBuffer
                    and r.endTime > :startMinusBuffer)
            order by t.capacity asc, t.id asc
            """)
    List<DiningTable> findFreeTables(@Param("date") LocalDate date,
                                     @Param("guests") int guests,
                                     @Param("busy") Collection<ReservationStatus> busy,
                                     @Param("startMinusBuffer") LocalTime startMinusBuffer,
                                     @Param("endPlusBuffer") LocalTime endPlusBuffer);
}
