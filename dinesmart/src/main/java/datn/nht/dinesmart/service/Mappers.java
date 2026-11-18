package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.AdminDtos.AuditLogDto;
import datn.nht.dinesmart.dto.AdminDtos.ChatLogDto;
import datn.nht.dinesmart.dto.AuthDtos.UserDto;
import datn.nht.dinesmart.dto.BillingDtos.PaymentDto;
import datn.nht.dinesmart.dto.BillingDtos.VoucherDto;
import datn.nht.dinesmart.dto.MenuDtos.CategoryDto;
import datn.nht.dinesmart.dto.MenuDtos.MenuItemDto;
import datn.nht.dinesmart.dto.OrderDtos.OrderItemDto;
import datn.nht.dinesmart.dto.ReservationDtos.ReservationDto;
import datn.nht.dinesmart.dto.ReservationDtos.ReservationEventDto;
import datn.nht.dinesmart.dto.ReservationDtos.TableDto;
import datn.nht.dinesmart.entity.*;

import java.math.BigDecimal;
import java.util.List;

/** Chuyển entity -> DTO. Phải gọi trong giao dịch nếu DTO cần quan hệ LAZY (category, table, menuItem). */
public final class Mappers {
    private Mappers() {}

    public static UserDto user(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(), u.isActive(),
                u.isLocked() ? u.getLockedUntil() : null, u.getCreatedAt());
    }

    public static CategoryDto category(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getType(), c.getSortOrder(), c.isActive());
    }

    public static MenuItemDto menuItem(MenuItem m) {
        Category c = m.getCategory();
        return new MenuItemDto(m.getId(), c.getId(), c.getName(), c.getType(), m.getName(), m.getDescription(),
                m.getAiDescription(), m.getPrice(), m.getImageUrl(), m.getServes(), m.isSignature(),
                List.copyOf(m.getTags()), List.copyOf(m.getAllergens()), m.getSauceSuggestion(), m.getStatus());
    }

    public static TableDto table(DiningTable t) {
        return new TableDto(t.getId(), t.getName(), t.getCapacity(), t.getArea(), t.getPosX(), t.getPosY(),
                t.isActive(), t.getStatus());
    }

    public static ReservationDto reservation(Reservation r, List<ReservationEvent> events) {
        return new ReservationDto(r.getId(), r.getCode(), r.getUserId(), r.getCustomerName(), r.getPhone(),
                r.getEmail(), r.getTable().getId(), r.getTable().getName(), r.getReservationDate(),
                r.getStartTime(), r.getEndTime(), r.getGuestCount(), r.getEventType(), r.getSpecialRequest(),
                r.getStatus(), r.getSource(), r.getCreatedAt(),
                events == null ? null : events.stream().map(Mappers::event).toList());
    }

    public static ReservationEventDto event(ReservationEvent e) {
        return new ReservationEventDto(e.getAction(), e.getUserId(), e.getReason(), e.getCreatedAt());
    }

    public static VoucherDto voucher(Voucher v) {
        return new VoucherDto(v.getId(), v.getCode(), v.getDescription(), v.getDiscountType(), v.getDiscountValue(),
                v.getMaxDiscount(), v.getMinOrderAmount(), v.getValidFrom(), v.getValidTo(), v.getUsageLimit(),
                v.getUsedCount(), v.isActive());
    }

    public static OrderItemDto orderItem(OrderItem i) {
        BigDecimal line = i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
        return new OrderItemDto(i.getId(), i.getMenuItem().getId(), i.getMenuItem().getName(), i.getQuantity(),
                i.getUnitPrice(), line, i.getStatus(), i.getNote(), i.getCancelReason(), i.getCreatedAt(),
                i.getStartedAt(), i.getReadyAt(), i.getServedAt());
    }

    public static PaymentDto payment(Payment p) {
        return new PaymentDto(p.getId(), p.getMethod(), p.getAmount(), p.getStatus(), p.getTransactionRef(),
                p.getPaidAt(), p.getCreatedAt());
    }

    public static AuditLogDto audit(AuditLog a) {
        return new AuditLogDto(a.getId(), a.getUserId(), a.getAction(), a.getEntityType(), a.getEntityId(),
                a.getOldValue(), a.getNewValue(), a.getCreatedAt());
    }

    public static ChatLogDto chat(ChatMessage m) {
        return new ChatLogDto(m.getId(), m.getSessionToken(), m.getUserId(), m.getRole(), m.getContent(),
                m.getToolName(), m.getToolArgs(), m.getModel(), m.getStatus(), m.getDurationMs(), m.getCreatedAt());
    }
}
