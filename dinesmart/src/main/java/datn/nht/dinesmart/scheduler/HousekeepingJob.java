package datn.nht.dinesmart.scheduler;

import datn.nht.dinesmart.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.housekeeping", name = "enabled", havingValue = "true")
public class HousekeepingJob {
    private final JdbcTemplate jdbc;
    private final RefreshTokenRepository refreshTokens;

    /** FR 21: tự huỷ đặt bàn quá hạn chờ duyệt, đánh dấu không đến, nhả bàn. Thủ tục tự khoá (UPDLOCK) nên chạy nhiều instance vẫn an toàn. */
    @Scheduled(fixedDelayString = "${app.housekeeping.interval-ms:60000}", initialDelay = 15000)
    public void reservations() {
        try {
            Map<String, Object> r = jdbc.queryForMap("EXEC usp_reservation_housekeeping");
            Number a = (Number) r.get("auto_cancelled");
            Number n = (Number) r.get("marked_no_show");
            if ((a != null && a.intValue() > 0) || (n != null && n.intValue() > 0)) {
                log.info("Housekeeping: tự huỷ {} đặt bàn quá hạn, {} khách không đến", a, n);
            }
        } catch (Exception e) {
            log.error("Housekeeping đặt bàn thất bại", e);
        }
    }

    /** Dọn refresh token đã hết hạn quá 7 ngày (app_rw có quyền DELETE trên bảng này). */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeTokens() {
        int n = refreshTokens.deleteExpiredBefore(LocalDateTime.now().minusDays(7));
        if (n > 0) log.info("Đã dọn {} refresh token hết hạn", n);
    }
}
