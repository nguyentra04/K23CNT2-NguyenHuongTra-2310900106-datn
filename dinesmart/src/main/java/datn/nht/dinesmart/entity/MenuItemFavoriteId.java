package datn.nht.dinesmart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MenuItemFavoriteId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "menu_item_id")
    private Long menuItemId;
}
