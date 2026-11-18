package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.AdminDtos.*;
import datn.nht.dinesmart.entity.ReservationAction;
import datn.nht.dinesmart.entity.ReservationStatus;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Báo cáo: doanh thu tính trên hóa đơn đã thu đủ (invoice.paid_at có giá trị). Dùng SQL thuần theo file schema. */
@Service
@RequiredArgsConstructor
public class ReportService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ReservationRepository reservations;

    private static String periodExpr(String granularity) {
        return switch (granularity == null ? "day" : granularity.toLowerCase()) {
            case "day" -> "CONVERT(VARCHAR(10), CAST(paid_at AS DATE), 23)";
            // 1900-01-01 là thứ Hai; chia theo NGÀY (không dùng DATEDIFF(WEEK) vì tuần bắt đầu Chủ nhật)
            case "week" -> "CONVERT(VARCHAR(10), DATEADD(DAY, DATEDIFF(DAY, '19000101', paid_at) / 7 * 7, '19000101'), 23)";
            case "month" -> "CONVERT(VARCHAR(7), paid_at, 23)";
            case "quarter" -> "CAST(YEAR(paid_at) AS VARCHAR(4)) + '-Q' + CAST(DATEPART(QUARTER, paid_at) AS VARCHAR(1))";
            default -> throw ApiException.badRequest("granularity phải là day, week, month hoặc quarter");
        };
    }

    @Transactional(readOnly = true)
    public List<RevenuePoint> revenue(String granularity, LocalDate from, LocalDate to) {
        checkRange(from, to);
        String sql = "SELECT period, COUNT(*) AS invoices, SUM(total_amount) AS gross, SUM(total_amount - vat_amount) AS net FROM ("
                + "SELECT " + periodExpr(granularity) + " AS period, total_amount, vat_amount FROM invoice "
                + "WHERE paid_at >= :from AND paid_at < :to) x GROUP BY period ORDER BY period";
        return jdbc.query(sql, range(from, to), (rs, i) -> new RevenuePoint(rs.getString("period"), rs.getLong("invoices"),
                rs.getBigDecimal("gross"), rs.getBigDecimal("net")));
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodStat> byMethod(LocalDate from, LocalDate to) {
        checkRange(from, to);
        return jdbc.query("""
                SELECT p.method, COUNT(DISTINCT p.invoice_id) AS invoices, SUM(p.amount) AS amount
                FROM payment p WHERE p.status = 'SUCCESS' AND p.paid_at >= :from AND p.paid_at < :to
                GROUP BY p.method ORDER BY amount DESC
                """, range(from, to), (rs, i) -> new PaymentMethodStat(rs.getString("method"), rs.getLong("invoices"), rs.getBigDecimal("amount")));
    }

    /** Điểm yêu thích = số lượng gọi + 3 × lượt thả tim (cùng khoảng thời gian). */
    @Transactional(readOnly = true)
    public List<PopularItem> popular(LocalDate from, LocalDate to, int limit) {
        checkRange(from, to);
        int top = Math.max(1, Math.min(limit, 50));
        return jdbc.query("""
                SELECT TOP (""" + top + """
                ) m.id, m.name, ISNULL(o.qty, 0) AS ordered_qty, ISNULL(f.fav, 0) AS favorites,
                       ISNULL(o.qty, 0) + 3 * ISNULL(f.fav, 0) AS popularity
                FROM menu_item m
                LEFT JOIN (SELECT oi.menu_item_id, SUM(oi.quantity) qty FROM order_item oi
                           JOIN invoice iv ON iv.order_id = oi.order_id
                           WHERE oi.status <> 'CANCELLED' AND iv.paid_at >= :from AND iv.paid_at < :to
                           GROUP BY oi.menu_item_id) o ON o.menu_item_id = m.id
                LEFT JOIN (SELECT menu_item_id, COUNT(*) fav FROM menu_item_favorite
                           WHERE created_at >= :from AND created_at < :to GROUP BY menu_item_id) f ON f.menu_item_id = m.id
                ORDER BY popularity DESC, m.name
                """, range(from, to), (rs, i) -> new PopularItem(rs.getLong("id"), rs.getString("name"),
                rs.getLong("ordered_qty"), rs.getLong("favorites"), rs.getLong("popularity")));
    }

    /** FR 19: tỉ lệ hoàn tất / huỷ (theo ai huỷ) / không đến — chỉ tính đặt bàn đã kết thúc. */
    @Transactional(readOnly = true)
    public ReservationStats reservationStats(LocalDate from, LocalDate to) {
        checkRange(from, to);
        List<ReservationStatus> finals = List.of(ReservationStatus.COMPLETED, ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW);
        Map<ReservationStatus, Long> st = new EnumMap<>(ReservationStatus.class);
        reservations.countFinalByStatus(from, to, finals).forEach(r -> st.put((ReservationStatus) r[0], (Long) r[1]));
        Map<ReservationAction, Long> ac = new EnumMap<>(ReservationAction.class);
        reservations.countCancelActions(from, to, finals,
                List.of(ReservationAction.CANCELLED, ReservationAction.REJECTED, ReservationAction.EXPIRED))
                .forEach(r -> ac.put((ReservationAction) r[0], (Long) r[1]));
        long completed = st.getOrDefault(ReservationStatus.COMPLETED, 0L);
        long noShow = st.getOrDefault(ReservationStatus.NO_SHOW, 0L);
        long total = completed + noShow + st.getOrDefault(ReservationStatus.CANCELLED, 0L);
        return new ReservationStats(from, to.minusDays(1), total, completed, noShow,
                ac.getOrDefault(ReservationAction.CANCELLED, 0L), ac.getOrDefault(ReservationAction.REJECTED, 0L),
                ac.getOrDefault(ReservationAction.EXPIRED, 0L), pct(completed, total), pct(noShow, total));
    }

    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        LocalDate today = LocalDate.now();
        MapSqlParameterSource p = range(today, today);
        Map<String, Object> rev = jdbc.queryForMap(
                "SELECT COUNT(*) AS n, ISNULL(SUM(total_amount), 0) AS gross FROM invoice WHERE paid_at >= :from AND paid_at < :to", p);
        long pending = count("SELECT COUNT(*) FROM reservation WHERE status = 'PENDING'", Map.of());
        long todayResv = count("SELECT COUNT(*) FROM reservation WHERE reservation_date = :d AND status IN ('PENDING','CONFIRMED','CHECKED_IN')", Map.of("d", today));
        long openOrders = count("SELECT COUNT(*) FROM orders WHERE status IN ('OPEN','BILLED')", Map.of());
        return new DashboardDto((BigDecimal) rev.get("gross"), ((Number) rev.get("n")).longValue(), pending, todayResv, openOrders,
                revenue("day", today.minusDays(6), today));
    }

    private long count(String sql, Map<String, ?> params) {
        Long n = jdbc.queryForObject(sql, params, Long.class);
        return n == null ? 0 : n;
    }

    /** [from, to] theo NGÀY (gồm cả ngày `to`) -> khoảng nửa mở [from 00:00, to+1 00:00). */
    private static MapSqlParameterSource range(LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("from", LocalDateTime.of(from, java.time.LocalTime.MIN))
                .addValue("to", LocalDateTime.of(to.plusDays(1), java.time.LocalTime.MIN));
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) throw ApiException.badRequest("Khoảng ngày không hợp lệ");
        if (from.plusYears(5).isBefore(to)) throw ApiException.badRequest("Khoảng ngày tối đa 5 năm");
    }

    private static BigDecimal pct(long part, long total) {
        return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(part * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }
}
