package datn.nht.dinesmart.controller;

import datn.nht.dinesmart.dto.MenuDtos.MenuItemDto;
import datn.nht.dinesmart.dto.MenuDtos.MenuStatusRequest;
import datn.nht.dinesmart.dto.OrderDtos.ItemStatusRequest;
import datn.nht.dinesmart.dto.OrderDtos.KitchenItemDto;
import datn.nht.dinesmart.entity.MenuStatus;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.security.CurrentUser;
import datn.nht.dinesmart.service.MenuService;
import datn.nht.dinesmart.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Màn hình bếp (KITCHEN, CASHIER, ADMIN). */
@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class KitchenController {
    private final OrderService orders;
    private final MenuService menu;

    @GetMapping("/queue")
    public List<KitchenItemDto> queue() {
        return orders.kitchenQueue();
    }

    @PostMapping("/items/{id}/status")
    public KitchenItemDto advance(@PathVariable Long id, @Valid @RequestBody ItemStatusRequest r) {
        return orders.advanceItem(id, r.status(), CurrentUser.require().id());
    }

    /** Bếp báo hết món / có lại món. Chỉ AVAILABLE <-> SOLD_OUT; ẩn món là việc của quản trị. */
    @PatchMapping("/menu/{id}/status")
    public MenuItemDto menuStatus(@PathVariable Long id, @Valid @RequestBody MenuStatusRequest r) {
        if (r.status() == MenuStatus.HIDDEN) throw ApiException.forbidden("Chỉ quản trị viên được ẩn món");
        return menu.setStatus(id, r.status(), CurrentUser.require().id());
    }
}
