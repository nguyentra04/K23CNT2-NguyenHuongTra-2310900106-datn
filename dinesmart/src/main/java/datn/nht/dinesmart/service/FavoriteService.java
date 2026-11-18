package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.MenuDtos.MenuItemDto;
import datn.nht.dinesmart.entity.MenuItem;
import datn.nht.dinesmart.entity.MenuItemFavorite;
import datn.nht.dinesmart.entity.MenuItemFavoriteId;
import datn.nht.dinesmart.entity.MenuStatus;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.FavoriteRepository;
import datn.nht.dinesmart.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favorites;
    private final MenuItemRepository items;

    @Transactional(readOnly = true)
    public List<MenuItemDto> list(Long userId) {
        List<Long> ids = favorites.findByIdUserId(userId).stream().map(f -> f.getId().getMenuItemId()).toList();
        if (ids.isEmpty()) return List.of();
        return items.findAllWithCategory(ids).stream()
                .filter(m -> m.getStatus() != MenuStatus.HIDDEN).map(Mappers::menuItem).toList();
    }

    @Transactional
    public void add(Long userId, Long itemId) {
        MenuItem m = items.findById(itemId).orElseThrow(() -> ApiException.notFound("Không tìm thấy món"));
        if (m.getStatus() == MenuStatus.HIDDEN) throw ApiException.notFound("Không tìm thấy món");
        if (!favorites.existsById(new MenuItemFavoriteId(userId, itemId))) {
            favorites.save(new MenuItemFavorite(userId, itemId));
        }
    }

    @Transactional
    public void remove(Long userId, Long itemId) {
        MenuItemFavoriteId id = new MenuItemFavoriteId(userId, itemId);
        if (favorites.existsById(id)) favorites.deleteById(id);
    }
}
