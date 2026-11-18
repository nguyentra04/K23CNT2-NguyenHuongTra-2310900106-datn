package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Voucher> findAllByOrderByIdDesc();

    /** [P6] Trừ lượt dùng nguyên tử: trả 0 nếu đã hết lượt (tránh vượt usage_limit khi đồng thời). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Voucher v set v.usedCount = v.usedCount + 1 where v.id = :id and (v.usageLimit is null or v.usedCount < v.usageLimit)")
    int tryConsume(@Param("id") Long id);
}
