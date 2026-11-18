package datn.nht.dinesmart.controller;

import datn.nht.dinesmart.dto.AuthDtos.UpdateProfileRequest;
import datn.nht.dinesmart.dto.AuthDtos.UserDto;
import datn.nht.dinesmart.dto.MenuDtos.MenuItemDto;
import datn.nht.dinesmart.dto.ReservationDtos.ReservationDto;
import datn.nht.dinesmart.security.CurrentUser;
import datn.nht.dinesmart.service.FavoriteService;
import datn.nht.dinesmart.service.ReservationService;
import datn.nht.dinesmart.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Khu vực của người dùng đã đăng nhập (mọi vai trò). */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {
    private final ReservationService reservations;
    private final FavoriteService favorites;
    private final UserService users;

    @PutMapping("/profile")
    public UserDto profile(@Valid @RequestBody UpdateProfileRequest r) {
        return users.updateProfile(CurrentUser.require().id(), r);
    }

    @GetMapping("/reservations")
    public List<ReservationDto> myReservations() {
        return reservations.mine(CurrentUser.require().id());
    }

    @PostMapping("/reservations/{id}/cancel")
    public ReservationDto cancel(@PathVariable Long id) {
        return reservations.cancelByCustomer(CurrentUser.require().id(), id);
    }

    @GetMapping("/favorites")
    public List<MenuItemDto> favorites() {
        return favorites.list(CurrentUser.require().id());
    }

    @PutMapping("/favorites/{itemId}")
    public ResponseEntity<Void> addFavorite(@PathVariable Long itemId) {
        favorites.add(CurrentUser.require().id(), itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/favorites/{itemId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long itemId) {
        favorites.remove(CurrentUser.require().id(), itemId);
        return ResponseEntity.noContent().build();
    }
}
