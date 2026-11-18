package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.MenuStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Tìm món bằng SQL động (OPENJSON cho tags/allergens, search_key cho tên không dấu).
 * Dựng câu lệnh theo từng điều kiện có giá trị để tránh tham số NULL (SQL Server báo lỗi kiểu dữ liệu với "? IS NULL").
 * Trả về danh sách id đã sắp xếp: danh mục -> món signature -> tên.
 */
@Repository
public class MenuItemQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Long> searchIds(MenuSearchCriteria c) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.id FROM menu_item m JOIN category cat ON cat.id = m.category_id
                WHERE cat.is_active = 1
                """);
        List<Object[]> params = new ArrayList<>();   // {name, value}

        List<MenuStatus> statuses = (c.statuses() == null || c.statuses().isEmpty())
                ? List.of(MenuStatus.AVAILABLE, MenuStatus.SOLD_OUT) : c.statuses();
        List<String> st = statuses.stream().map(Enum::name).toList();
        sql.append(" AND m.status IN (:statuses)");
        params.add(new Object[]{"statuses", st});

        if (c.categoryId() != null) {
            sql.append(" AND m.category_id = :categoryId");
            params.add(new Object[]{"categoryId", c.categoryId()});
        }
        if (c.categoryType() != null) {
            sql.append(" AND cat.type = :categoryType");
            params.add(new Object[]{"categoryType", c.categoryType().name()});
        }
        if (c.keyword() != null && !c.keyword().isBlank()) {
            sql.append(" AND m.search_key LIKE :kw");
            params.add(new Object[]{"kw", "%" + escapeLike(c.keyword().trim()) + "%"});
        }
        if (c.maxPrice() != null) {
            sql.append(" AND m.price <= :maxPrice");
            params.add(new Object[]{"maxPrice", c.maxPrice()});
        }
        if (Boolean.TRUE.equals(c.signatureOnly())) {
            sql.append(" AND m.is_signature = 1");
        }
        if (c.tags() != null) {
            int i = 0;
            for (String tag : c.tags()) {
                if (tag == null || tag.isBlank()) continue;
                String p = "tag" + (i++);
                sql.append(" AND EXISTS (SELECT 1 FROM OPENJSON(m.tags) t WHERE t.[value] = :").append(p).append(")");
                params.add(new Object[]{p, tag.trim().toLowerCase()});
            }
        }
        if (c.excludeAllergens() != null && !c.excludeAllergens().isEmpty()) {
            sql.append(" AND NOT EXISTS (SELECT 1 FROM OPENJSON(m.allergens) a WHERE a.[value] IN (:allergens))");
            params.add(new Object[]{"allergens", c.excludeAllergens().stream().map(s -> s.trim().toLowerCase()).toList()});
        }
        sql.append(" ORDER BY cat.sort_order, m.is_signature DESC, m.name");

        Query q = em.createNativeQuery(sql.toString());
        for (Object[] p : params) q.setParameter((String) p[0], p[1]);
        List<Object> rows = q.getResultList();
        return rows.stream().map(o -> ((Number) o).longValue()).toList();
    }

    private static String escapeLike(String s) {
        return s.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }
}
