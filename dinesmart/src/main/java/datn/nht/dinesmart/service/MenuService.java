package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.MenuDtos.*;
import datn.nht.dinesmart.entity.*;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.*;
import datn.nht.dinesmart.util.TextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final CategoryRepository categories;
    private final MenuItemRepository items;
    private final MenuItemQueryRepository query;
    private final AuditService audit;

    // ---------------- danh mục ----------------
    @Transactional(readOnly = true)
    public List<CategoryDto> publicCategories() {
        return categories.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(Mappers::category).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> allCategories() {
        return categories.findAllByOrderBySortOrderAscIdAsc().stream().map(Mappers::category).toList();
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest r, Long actorId) {
        if (categories.existsByNameIgnoreCase(r.name().trim())) throw ApiException.conflict("Tên danh mục đã tồn tại");
        Category c = new Category();
        apply(c, r);
        categories.save(c);
        audit.log(actorId, "CATEGORY_CREATED", "category", c.getId(), null, AuditService.kv("name", c.getName()));
        return Mappers.category(c);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest r, Long actorId) {
        Category c = categories.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy danh mục"));
        if (!c.getName().equalsIgnoreCase(r.name().trim()) && categories.existsByNameIgnoreCase(r.name().trim())) {
            throw ApiException.conflict("Tên danh mục đã tồn tại");
        }
        apply(c, r);
        audit.log(actorId, "CATEGORY_UPDATED", "category", id, null, AuditService.kv("name", c.getName(), "active", c.isActive()));
        return Mappers.category(categories.save(c));
    }

    private void apply(Category c, CategoryRequest r) {
        c.setName(r.name().trim());
        c.setType(r.type());
        c.setSortOrder(r.sortOrder());
        c.setActive(r.active());
    }

    // ---------------- món ----------------
    /** Tìm món cho khách / chatbot. Mặc định chỉ AVAILABLE + SOLD_OUT (không bao giờ trả HIDDEN). */
    @Transactional(readOnly = true)
    public List<MenuItemDto> search(MenuSearchCriteria c) {
        return load(query.searchIds(c));
    }

    /** Danh sách cho quản trị: gồm cả món ẩn. */
    @Transactional(readOnly = true)
    public List<MenuItemDto> adminList(Long categoryId, String keyword) {
        MenuSearchCriteria c = new MenuSearchCriteria(categoryId, null, TextUtil.normalize(keyword), null, null, null, null,
                List.of(MenuStatus.AVAILABLE, MenuStatus.SOLD_OUT, MenuStatus.HIDDEN));
        return load(query.searchIds(c));
    }

    @Transactional(readOnly = true)
    public MenuItemDto get(Long id, boolean includeHidden) {
        MenuItem m = items.findWithCategory(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy món"));
        if (!includeHidden && m.getStatus() == MenuStatus.HIDDEN) throw ApiException.notFound("Không tìm thấy món");
        return Mappers.menuItem(m);
    }

    @Transactional
    public MenuItemDto create(MenuItemRequest r, Long actorId) {
        Category cat = categories.findById(r.categoryId()).orElseThrow(() -> ApiException.badRequest("Danh mục không tồn tại"));
        if (items.existsByCategoryIdAndNameIgnoreCase(cat.getId(), r.name().trim())) {
            throw ApiException.conflict("Món này đã có trong danh mục");
        }
        MenuItem m = new MenuItem();
        m.setCategory(cat);
        apply(m, r);
        items.save(m);
        audit.log(actorId, "MENU_CREATED", "menu_item", m.getId(), null, AuditService.kv("name", m.getName(), "price", m.getPrice()));
        return Mappers.menuItem(m);
    }

    @Transactional
    public MenuItemDto update(Long id, MenuItemRequest r, Long actorId) {
        MenuItem m = items.findWithCategory(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy món"));
        Category cat = categories.findById(r.categoryId()).orElseThrow(() -> ApiException.badRequest("Danh mục không tồn tại"));
        boolean renamedOrMoved = !m.getName().equalsIgnoreCase(r.name().trim()) || !m.getCategory().getId().equals(cat.getId());
        if (renamedOrMoved && items.existsByCategoryIdAndNameIgnoreCase(cat.getId(), r.name().trim())) {
            throw ApiException.conflict("Món này đã có trong danh mục");
        }
        Object before = AuditService.kv("name", m.getName(), "price", m.getPrice(), "status", m.getStatus().name());
        m.setCategory(cat);
        apply(m, r);
        items.save(m);
        audit.log(actorId, "MENU_UPDATED", "menu_item", id, before,
                AuditService.kv("name", m.getName(), "price", m.getPrice(), "status", m.getStatus().name()));
        return Mappers.menuItem(m);
    }

    /** Đổi trạng thái (bếp/thu ngân: hết món / còn món; quản trị: ẩn). */
    @Transactional
    public MenuItemDto setStatus(Long id, MenuStatus status, Long actorId) {
        MenuItem m = items.findWithCategory(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy món"));
        MenuStatus old = m.getStatus();
        m.setStatus(status);
        items.save(m);
        audit.log(actorId, "MENU_STATUS", "menu_item", id, AuditService.kv("status", old.name()), AuditService.kv("status", status.name()));
        return Mappers.menuItem(m);
    }

    private void apply(MenuItem m, MenuItemRequest r) {
        m.setName(r.name().trim());
        m.setSearchKey(TextUtil.normalize(r.name()));      // [F05] ứng dụng phải tự điền
        m.setDescription(TextUtil.blankToNull(r.description()));
        m.setAiDescription(TextUtil.blankToNull(r.aiDescription()));
        m.setPrice(r.price().setScale(0, java.math.RoundingMode.HALF_UP));
        m.setImageUrl(TextUtil.blankToNull(r.imageUrl()));
        m.setServes(r.serves());
        m.setSignature(r.signature());
        m.setTags(cleanCodes(r.tags()));
        m.setAllergens(cleanCodes(r.allergens()));
        m.setSauceSuggestion(TextUtil.blankToNull(r.sauceSuggestion()));
        m.setStatus(r.status());
    }

    private static List<String> cleanCodes(List<String> in) {
        if (in == null) return new ArrayList<>();
        return new ArrayList<>(new LinkedHashSet<>(in.stream()
                .filter(Objects::nonNull).map(s -> s.trim().toLowerCase()).filter(s -> !s.isEmpty()).toList()));
    }

    /** Nạp theo thứ tự id đã sắp xếp từ SQL. */
    private List<MenuItemDto> load(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        Map<Long, MenuItem> byId = new HashMap<>();
        items.findAllWithCategory(ids).forEach(m -> byId.put(m.getId(), m));
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(Mappers::menuItem).toList();
    }

    public static BigDecimal parsePrice(String s) {
        try {
            return s == null || s.isBlank() ? null : new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Giá không hợp lệ");
        }
    }
}
