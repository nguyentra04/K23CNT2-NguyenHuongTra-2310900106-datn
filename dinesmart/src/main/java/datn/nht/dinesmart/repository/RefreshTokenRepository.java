package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Thu hồi nguyên tử token còn hiệu lực. Trả 0 nếu token đã bị dùng/thu hồi/hết hạn. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revokedAt = :now where r.tokenHash = :hash and r.revokedAt is null and r.expiresAt > :now")
    int revokeIfActive(@Param("hash") String hash, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revokedAt = :now where r.userId = :userId and r.revokedAt is null")
    int revokeAllByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
