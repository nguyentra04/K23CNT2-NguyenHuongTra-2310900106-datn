package datn.nht.dinesmart.controller;

import datn.nht.dinesmart.dto.ChatDtos.ChatRequest;
import datn.nht.dinesmart.dto.ChatDtos.ChatResponse;
import datn.nht.dinesmart.dto.MenuDtos.CategoryDto;
import datn.nht.dinesmart.dto.MenuDtos.MenuItemDto;
import datn.nht.dinesmart.dto.ReservationDtos.*;
import datn.nht.dinesmart.dto.SettingDtos.PublicSettingDto;
import datn.nht.dinesmart.entity.CategoryType;
import datn.nht.dinesmart.entity.MenuStatus;
import datn.nht.dinesmart.entity.ReservationSource;
import datn.nht.dinesmart.entity.Role;
import datn.nht.dinesmart.repository.MenuSearchCriteria;
import datn.nht.dinesmart.security.AuthUser;
import datn.nht.dinesmart.security.CurrentUser;
import datn.nht.dinesmart.service.*;
import datn.nht.dinesmart.util.TextUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Endpoint công khai (không cần đăng nhập): thực đơn, cài đặt, bàn trống, đặt bàn, chatbot. */
@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicController {
    private final MenuService menu;
    private final SettingService settings;
    private final TableService tables;
    private final ReservationService reservations;
    private final ChatbotService chatbot;

    // ---------- thực đơn ----------
    @GetMapping("/menu/categories")
    public List<CategoryDto> categories() {
        return menu.publicCategories();
    }

    @GetMapping("/menu/items")
    public List<MenuItemDto> items(@RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) CategoryType type,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) String cuisine,
                                   @RequestParam(required = false) List<String> tag,
                                   @RequestParam(required = false) List<String> excludeAllergen,
                                   @RequestParam(required = false) BigDecimal maxPrice,
                                   @RequestParam(required = false) Boolean signature) {
        List<String> tags = new ArrayList<>(tag == null ? List.of() : tag);
        if (cuisine != null && !cuisine.isBlank()) tags.add(cuisine);
        return menu.search(new MenuSearchCriteria(categoryId, type, TextUtil.normalize(q), tags, excludeAllergen,
                maxPrice, signature, List.of(MenuStatus.AVAILABLE, MenuStatus.SOLD_OUT)));
    }

    @GetMapping("/menu/items/{id}")
    public MenuItemDto item(@PathVariable Long id) {
        return menu.get(id, false);
    }

    // ---------- cài đặt, bàn trống ----------
    @GetMapping("/settings/public")
    public PublicSettingDto publicSettings() {
        return settings.publicView();
    }

    @GetMapping("/tables/availability")
    public AvailabilityResponse availability(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                                             @RequestParam int guests) {
        return tables.availability(date, time, guests);
    }

    // ---------- đặt bàn ----------
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDto reserve(@Valid @RequestBody ReservationRequest r) {
        Long userId = CurrentUser.get().filter(u -> u.role() == Role.CUSTOMER).map(AuthUser::id).orElse(null);
        return reservations.createForCustomer(r, ReservationSource.ONLINE, userId);
    }

    @GetMapping("/reservations/lookup")
    public ReservationDto lookup(@RequestParam @NotBlank String code,
                                 @RequestParam @Pattern(regexp = datn.nht.dinesmart.dto.AuthDtos.PHONE_RULE) String phone) {
        return reservations.lookup(code, phone);
    }

    @PostMapping("/reservations/cancel")
    public ReservationDto guestCancel(@Valid @RequestBody GuestCancelRequest r) {
        return reservations.cancelByGuest(r.code(), r.phone());
    }

    // ---------- chatbot ----------
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest r) {
        return chatbot.chat(r, CurrentUser.idOrNull());
    }
}
