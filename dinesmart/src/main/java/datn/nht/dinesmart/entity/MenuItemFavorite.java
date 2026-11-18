package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "menu_item_favorite")
@Getter @Setter
public class MenuItemFavorite {
    @EmbeddedId
    private MenuItemFavoriteId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MenuItemFavorite() {}

    public MenuItemFavorite(Long userId, Long menuItemId) {
        this.id = new MenuItemFavoriteId(userId, menuItemId);
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
