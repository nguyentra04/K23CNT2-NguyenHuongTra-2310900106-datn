package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Chỉ ghi thêm (trigger trg_audit_log_immutable cấm sửa). */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Column(nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    @Column(name = "old_value", updatable = false)
    private String oldValue;

    @Column(name = "new_value", updatable = false)
    private String newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
