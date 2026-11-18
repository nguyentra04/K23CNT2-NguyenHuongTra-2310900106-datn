package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.OrderDtos.*;
import datn.nht.dinesmart.entity.*;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** Gọi món theo bàn + màn hình bếp. Không chứa tiền: hóa đơn do BillingService lập. */
@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Set<OrderStatus> ACTIVE = EnumSet.of(OrderStatus.OPEN, OrderStatus.BILLED);

    private final DiningOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final DiningTableRepository tables;
    private final MenuItemRepository menu;
    private final ReservationService reservationService;
    private final BillingService billing;
    private final AuditService audit;

    // =============================== ĐƠN ===============================

    @Transactional
    public OrderDto create(CreateOrderRequest r, Long staffId) {
        DiningTable table;
        Long reservationId = null;
        String customer = r.customerName();

        if (r.reservationId() != null) {
            Reservation resv = reservationService.findEntity(r.reservationId());
            orders.findFirstByReservationIdAndStatusNot(resv.getId(), OrderStatus.CANCELLED).ifPresent(o -> {
                throw ApiException.conflict("Đặt bàn này đã có đơn gọi món (#" + o.getId() + ")");
            });
            if (resv.getStatus() == ReservationStatus.CONFIRMED) {
                reservationService.checkInInternal(resv, staffId);               // khách đã tới -> tự check-in
            } else if (resv.getStatus() != ReservationStatus.CHECKED_IN) {
                throw ApiException.conflict("Chỉ mở đơn cho đặt bàn đã xác nhận hoặc đã check-in");
            }
            table = resv.getTable();
            reservationId = resv.getId();
            if (customer == null || customer.isBlank()) customer = resv.getCustomerName();
        } else if (r.tableId() != null) {
            table = tables.findById(r.tableId()).orElseThrow(() -> ApiException.notFound("Không tìm thấy bàn"));
        } else {
            throw ApiException.badRequest("Cần chọn bàn hoặc đặt bàn để mở đơn");
        }
        if (!table.isActive() || table.getStatus() == TableStatus.MAINTENANCE) {
            throw ApiException.badRequest("Bàn " + table.getName() + " hiện không sử dụng được");
        }
        orders.findFirstByTableIdAndStatusIn(table.getId(), ACTIVE).ifPresent(o -> {
            throw ApiException.conflict("Bàn " + table.getName() + " đang có đơn chưa thanh toán (#" + o.getId() + ")");
        });

        DiningOrder o = new DiningOrder();
        o.setReservationId(reservationId);
        o.setTable(table);
        o.setCustomerName(customer == null || customer.isBlank() ? null : customer.trim());
        o.setNote(r.note());
        o.setCreatedBy(staffId);
        orders.saveAndFlush(o);

        table.setStatus(TableStatus.OCCUPIED);
        tables.save(table);
        audit.log(staffId, "ORDER_OPENED", "orders", o.getId(), null, AuditService.kv("table", table.getName()));
        return view(o);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> list(Collection<OrderStatus> statuses) {
        Collection<OrderStatus> st = (statuses == null || statuses.isEmpty()) ? ACTIVE : statuses;
        return orders.findByStatusInOrderByCreatedAtAsc(st).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long id) {
        return view(orders.findWithTableById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn")));
    }

    @Transactional
    public OrderDto addItems(Long orderId, AddItemsRequest req, Long staffId) {
        DiningOrder o = orders.findWithTableById(orderId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        if (o.getStatus() != OrderStatus.OPEN) throw ApiException.conflict("Đơn đã lập hóa đơn hoặc đã đóng, không gọi thêm món được");

        Set<Long> ids = new HashSet<>();
        req.items().forEach(l -> ids.add(l.menuItemId()));
        Map<Long, MenuItem> byId = new HashMap<>();
        menu.findAllWithCategory(ids).forEach(m -> byId.put(m.getId(), m));

        List<OrderItem> created = new ArrayList<>();
        for (ItemLine line : req.items()) {
            MenuItem m = byId.get(line.menuItemId());
            if (m == null || m.getStatus() == MenuStatus.HIDDEN) throw ApiException.notFound("Món không tồn tại");
            if (m.getStatus() != MenuStatus.AVAILABLE) throw ApiException.conflict("Món \"" + m.getName() + "\" hiện đã hết");
            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setMenuItem(m);
            oi.setQuantity(line.quantity());
            oi.setUnitPrice(m.getPrice());              // chốt giá tại lúc gọi
            oi.setNote(line.note() == null || line.note().isBlank() ? null : line.note().trim());
            oi.setCreatedBy(staffId);
            created.add(oi);
        }
        orderItems.saveAll(created);
        audit.log(staffId, "ORDER_ITEMS_ADDED", "orders", orderId, null, AuditService.kv("count", created.size()));
        return view(o);
    }

    /** Huỷ một món (FR 11: bắt buộc lý do). Món đã phục vụ thì không huỷ được. */
    @Transactional
    public OrderDto cancelItem(Long itemId, String reason, Long staffId) {
        OrderItem i = orderItems.findDetailById(itemId).orElseThrow(() -> ApiException.notFound("Không tìm thấy món trong đơn"));
        DiningOrder o = i.getOrder();
        if (o.getStatus() != OrderStatus.OPEN) throw ApiException.conflict("Đơn đã lập hóa đơn, không huỷ món được");
        if (i.getStatus() == OrderItemStatus.SERVED || i.getStatus() == OrderItemStatus.CANCELLED) {
            throw ApiException.conflict("Món đã phục vụ hoặc đã huỷ");
        }
        cancel(i, reason, staffId);
        audit.log(staffId, "ORDER_ITEM_CANCELLED", "order_item", itemId, null, AuditService.kv("reason", reason));
        return view(o);
    }

    @Transactional
    public OrderDto cancelOrder(Long orderId, String reason, Long staffId) {
        DiningOrder o = orders.findWithTableById(orderId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        if (o.getStatus() != OrderStatus.OPEN) throw ApiException.conflict("Chỉ huỷ được đơn đang gọi món (chưa lập hóa đơn)");
        List<OrderItem> items = orderItems.findByOrderIdOrderByIdAsc(orderId);
        if (items.stream().anyMatch(i -> i.getStatus() == OrderItemStatus.SERVED)) {
            throw ApiException.conflict("Đơn đã có món được phục vụ, không thể huỷ cả đơn");
        }
        items.stream().filter(i -> i.getStatus() != OrderItemStatus.CANCELLED).forEach(i -> cancel(i, reason, staffId));
        o.setStatus(OrderStatus.CANCELLED);
        o.setCancelReason(reason);
        orders.saveAndFlush(o);
        DiningTable t = o.getTable();
        if (t != null && t.getStatus() == TableStatus.OCCUPIED) {
            t.setStatus(TableStatus.AVAILABLE);
            tables.save(t);
        }
        audit.log(staffId, "ORDER_CANCELLED", "orders", orderId, null, AuditService.kv("reason", reason));
        return view(o);
    }

    private void cancel(OrderItem i, String reason, Long staffId) {
        i.setStatus(OrderItemStatus.CANCELLED);
        i.setCancelReason(reason);
        i.setCancelledBy(staffId);
        i.setCancelledAt(LocalDateTime.now());
        orderItems.save(i);
    }

    // =============================== BẾP ===============================

    /** Hàng đợi bếp (FR 13): món chờ + đang làm + đã xong chờ phục vụ, cũ nhất trước. */
    @Transactional(readOnly = true)
    public List<KitchenItemDto> kitchenQueue() {
        return orderItems.findQueue(List.of(OrderItemStatus.PENDING, OrderItemStatus.PREPARING, OrderItemStatus.READY))
                .stream().map(i -> new KitchenItemDto(i.getId(), i.getOrder().getId(),
                        i.getOrder().getTable() == null ? "—" : i.getOrder().getTable().getName(),
                        i.getMenuItem().getName(), i.getQuantity(), i.getNote(), i.getStatus(),
                        i.getCreatedAt(), i.getStartedAt(), i.getReadyAt())).toList();
    }

    /** PENDING -> PREPARING -> READY -> SERVED (không nhảy cóc, không quay lại). */
    @Transactional
    public KitchenItemDto advanceItem(Long itemId, OrderItemStatus target, Long actorId) {
        OrderItem i = orderItems.findDetailById(itemId).orElseThrow(() -> ApiException.notFound("Không tìm thấy món"));
        OrderItemStatus cur = i.getStatus();
        boolean ok = (cur == OrderItemStatus.PENDING && target == OrderItemStatus.PREPARING)
                || (cur == OrderItemStatus.PREPARING && target == OrderItemStatus.READY)
                || (cur == OrderItemStatus.READY && target == OrderItemStatus.SERVED);
        if (!ok) throw ApiException.conflict("Không thể chuyển món từ " + cur + " sang " + target);
        if (i.getOrder().getStatus() == OrderStatus.CANCELLED || i.getOrder().getStatus() == OrderStatus.CLOSED) {
            throw ApiException.conflict("Đơn đã đóng hoặc đã huỷ");
        }
        LocalDateTime now = LocalDateTime.now();
        i.setStatus(target);
        switch (target) {
            case PREPARING -> i.setStartedAt(now);
            case READY -> i.setReadyAt(now);
            case SERVED -> i.setServedAt(now);
            default -> { }
        }
        orderItems.save(i);
        return new KitchenItemDto(i.getId(), i.getOrder().getId(),
                i.getOrder().getTable() == null ? "—" : i.getOrder().getTable().getName(),
                i.getMenuItem().getName(), i.getQuantity(), i.getNote(), i.getStatus(),
                i.getCreatedAt(), i.getStartedAt(), i.getReadyAt());
    }

    // =============================== MAP ===============================

    private OrderDto view(DiningOrder o) {
        List<OrderItemDto> items = orderItems.findByOrderIdOrderByIdAsc(o.getId()).stream().map(Mappers::orderItem).toList();
        var subtotal = items.stream().filter(i -> i.status() != OrderItemStatus.CANCELLED)
                .map(OrderItemDto::lineTotal).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        DiningTable t = o.getTable();
        return new OrderDto(o.getId(), o.getReservationId(), t == null ? null : t.getId(), t == null ? null : t.getName(),
                o.getStatus(), o.getCustomerName(), o.getNote(), o.getCancelReason(), o.getCreatedAt(), items, subtotal,
                billing.getByOrder(o.getId()));
    }
}
