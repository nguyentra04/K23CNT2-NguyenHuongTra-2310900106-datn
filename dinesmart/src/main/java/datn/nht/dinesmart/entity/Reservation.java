package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservation")
@Getter @Setter
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12, unique = true)
    private String code;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "guest_count", nullable = false)
    private int guestCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20)
    private EventType eventType;

    @Column(name = "special_request", length = 500)
    private String specialRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReservationSource source = ReservationSource.ONLINE;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
