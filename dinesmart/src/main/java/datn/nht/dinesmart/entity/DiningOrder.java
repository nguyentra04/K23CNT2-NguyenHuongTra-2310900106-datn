package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Bảng `orders` — đặt tên lớp DiningOrder để tránh trùng từ khoá ORDER của JPQL. */
@Entity
@Table(name = "orders")
@Getter @Setter
public class DiningOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private DiningTable table;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(length = 255)
    private String note;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(nullable = false)
    private int version;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
