package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_item")
@Getter @Setter
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private DiningOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private int quantity;

    /** Giá chốt tại lúc gọi. */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderItemStatus status = OrderItemStatus.PENDING;

    @Column(length = 255)
    private String note;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "served_at")
    private LocalDateTime servedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
