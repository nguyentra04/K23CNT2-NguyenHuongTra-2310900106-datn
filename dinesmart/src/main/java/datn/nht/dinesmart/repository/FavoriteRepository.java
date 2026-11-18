package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.MenuItemFavorite;
import datn.nht.dinesmart.entity.MenuItemFavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<MenuItemFavorite, MenuItemFavoriteId> {
    List<MenuItemFavorite> findByIdUserId(Long userId);
}
