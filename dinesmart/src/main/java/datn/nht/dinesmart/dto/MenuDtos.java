package datn.nht.dinesmart.dto;

import datn.nht.dinesmart.entity.CategoryType;
import datn.nht.dinesmart.entity.MenuStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class MenuDtos {
    private MenuDtos() {}

    public record CategoryDto(Long id, String name, CategoryType type, int sortOrder, boolean active) {}

    public record CategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull CategoryType type,
            int sortOrder,
            boolean active) {}

    public record MenuItemDto(
            Long id, Long categoryId, String categoryName, CategoryType categoryType,
            String name, String description, String aiDescription, BigDecimal price, String imageUrl,
            int serves, boolean signature, List<String> tags, List<String> allergens,
            String sauceSuggestion, MenuStatus status) {}

    public record MenuItemRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 4000) String description,
            @Size(max = 4000) String aiDescription,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0, message = "Giá phải là số nguyên (VND)") BigDecimal price,
            @Size(max = 255) String imageUrl,
            @Min(1) int serves,
            boolean signature,
            @Size(max = 20) List<@Size(max = 30) String> tags,
            @Size(max = 20) List<@Size(max = 30) String> allergens,
            @Size(max = 255) String sauceSuggestion,
            @NotNull MenuStatus status) {}

    public record MenuStatusRequest(@NotNull MenuStatus status) {}

    public record UploadResponse(String url) {}
}
