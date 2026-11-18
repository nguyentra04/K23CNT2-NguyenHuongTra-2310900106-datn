package datn.nht.dinesmart.dto;

import datn.nht.dinesmart.entity.OrderItemStatus;
import datn.nht.dinesmart.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record CreateOrderRequest(Long tableId, Long reservationId,
                                     @Size(max = 100) String customerName, @Size(max = 255) String note) {}

    public record ItemLine(@NotNull Long menuItemId, @Min(1) @Max(99) int quantity, @Size(max = 255) String note) {}

    public record AddItemsRequest(@NotEmpty @Size(max = 50) List<@Valid ItemLine> items) {}

    public record CancelRequest(@NotBlank @Size(max = 255) String reason) {}

    public record ItemStatusRequest(@NotNull OrderItemStatus status) {}

    public record OrderItemDto(Long id, Long menuItemId, String name, int quantity, BigDecimal unitPrice,
                               BigDecimal lineTotal, OrderItemStatus status, String note, String cancelReason,
                               LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime readyAt,
                               LocalDateTime servedAt) {}

    public record OrderDto(Long id, Long reservationId, Long tableId, String tableName, OrderStatus status,
                           String customerName, String note, String cancelReason, LocalDateTime createdAt,
                           List<OrderItemDto> items, BigDecimal subtotal, BillingDtos.InvoiceDto invoice) {}

    /** Một dòng trên màn hình bếp (FR 13). */
    public record KitchenItemDto(Long id, Long orderId, String tableName, String itemName, int quantity,
                                 String note, OrderItemStatus status, LocalDateTime createdAt,
                                 LocalDateTime startedAt, LocalDateTime readyAt) {}
}
