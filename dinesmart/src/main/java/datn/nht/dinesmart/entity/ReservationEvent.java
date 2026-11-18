package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Dấu vết xử lý đặt bàn. Chỉ INSERT + SELECT (xem phân quyền app_rw). */
@Entity
@Table(name = "reservation_event")
@Getter @Setter
public class ReservationEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false, updatable = false)
    private Long reservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12, updatable = false)
    private ReservationAction action;

    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Column(length = 255, updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ReservationEvent() {}

    public ReservationEvent(Long reservationId, ReservationAction action, Long userId, String reason) {
        this.reservationId = reservationId;
        this.action = action;
        this.userId = userId;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
