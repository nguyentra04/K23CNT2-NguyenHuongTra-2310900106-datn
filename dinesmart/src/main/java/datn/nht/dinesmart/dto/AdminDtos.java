package datn.nht.dinesmart.dto;

import datn.nht.dinesmart.entity.ChatRole;
import datn.nht.dinesmart.entity.ChatStatus;
import datn.nht.dinesmart.entity.Role;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {
        private AdminDtos() {
        }

        public record CreateUserRequest(
                        @NotBlank @Email @Size(max = 100) String email,
                        @NotBlank @Pattern(regexp = AuthDtos.PASSWORD_RULE, message = AuthDtos.PASSWORD_MSG) String password,
                        @NotBlank @Size(max = 100) String fullName,
                        @Pattern(regexp = "^$|" + AuthDtos.PHONE_RULE) String phone,
                        @NotNull Role role) {
        }

        public record UpdateUserRequest(
                        @NotBlank @Size(max = 100) String fullName,
                        @Pattern(regexp = "^$|" + AuthDtos.PHONE_RULE) String phone,
                        @NotNull Role role,
                        boolean active) {
        }

        public record ResetPasswordByAdminRequest(
                        @NotBlank @Pattern(regexp = AuthDtos.PASSWORD_RULE, message = AuthDtos.PASSWORD_MSG) String newPassword) {
        }

        public record AuditLogDto(Long id, Long userId, String action, String entityType, Long entityId,
                        String oldValue, String newValue, LocalDateTime createdAt) {
        }

        public record ChatLogDto(Long id, String sessionToken, Long userId, ChatRole role, String content,
                        String toolName, String toolArgs, String model, ChatStatus status,
                        Integer durationMs, LocalDateTime createdAt) {
        }

        // ---------- báo cáo ----------
        public record RevenuePoint(String period, long invoices, BigDecimal grossRevenue, BigDecimal netRevenue) {
        }

        public record PaymentMethodStat(String method, long invoices, BigDecimal amount) {
        }

        public record PopularItem(Long id, String name, long orderedQty, long favorites, long popularity) {
        }

        public record ReservationStats(LocalDate from, LocalDate to, long closedTotal, long completed, long noShow,
                        long cancelledByCustomer, long rejectedByStaff, long expiredBySystem,
                        BigDecimal completedPct, BigDecimal noShowPct) {
        }

        public record DashboardDto(BigDecimal todayRevenue, long todayInvoices, long pendingReservations,
                        long todayReservations, long openOrders, List<RevenuePoint> last7Days) {
        }
}
