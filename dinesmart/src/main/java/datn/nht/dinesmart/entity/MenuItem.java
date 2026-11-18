package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_item")
@Getter @Setter
public class MenuItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    /** Tên không dấu, chữ thường — ỨNG DỤNG điền khi lưu (xem TextUtil). */
    @Column(name = "search_key", nullable = false, length = 200)
    private String searchKey = "";

    private String description;

    @Column(name = "ai_description")
    private String aiDescription;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private int serves = 1;

    @Column(name = "is_signature", nullable = false)
    private boolean signature;

    @Convert(converter = JsonListConverter.class)
    @Column(nullable = false, length = 500)
    private List<String> tags = new ArrayList<>();

    @Convert(converter = JsonListConverter.class)
    @Column(nullable = false, length = 500)
    private List<String> allergens = new ArrayList<>();

    @Column(name = "sauce_suggestion", length = 255)
    private String sauceSuggestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MenuStatus status = MenuStatus.AVAILABLE;

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
