package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.CategoryType;
import datn.nht.dinesmart.entity.MenuStatus;

import java.math.BigDecimal;
import java.util.List;

/** Tiêu chí tìm món. Trường null = không lọc. */
public record MenuSearchCriteria(
        Long categoryId,
        CategoryType categoryType,
        String keyword,                 // đã chuẩn hoá không dấu, chữ thường
        List<String> tags,              // món phải có TẤT CẢ thẻ
        List<String> excludeAllergens,  // loại món chứa BẤT KỲ chất này
        BigDecimal maxPrice,
        Boolean signatureOnly,
        List<MenuStatus> statuses       // null/rỗng = AVAILABLE + SOLD_OUT
) {}
