package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter @Setter
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_token", nullable = false, length = 64)
    private String sessionToken;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatRole role;

    @Column(nullable = false)
    private String content;

    @Column(name = "tool_name", length = 50)
    private String toolName;

    @Column(name = "tool_args")
    private String toolArgs;

    @Column(length = 50)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatStatus status = ChatStatus.SUCCESS;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
