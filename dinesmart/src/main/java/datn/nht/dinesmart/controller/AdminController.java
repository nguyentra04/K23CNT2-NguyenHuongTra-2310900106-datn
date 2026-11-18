package datn.nht.dinesmart.controller;

import datn.nht.dinesmart.dto.AdminDtos.*;
import datn.nht.dinesmart.dto.AuthDtos.UserDto;
import datn.nht.dinesmart.dto.BillingDtos.VoucherDto;
import datn.nht.dinesmart.dto.BillingDtos.VoucherRequest;
import datn.nht.dinesmart.dto.MenuDtos.*;
import datn.nht.dinesmart.dto.ReservationDtos.TableDto;
import datn.nht.dinesmart.dto.ReservationDtos.TableRequest;
import datn.nht.dinesmart.dto.SettingDtos.SettingDto;
import datn.nht.dinesmart.dto.SettingDtos.SettingRequest;
import datn.nht.dinesmart.entity.ChatRole;
import datn.nht.dinesmart.entity.MenuStatus;
import datn.nht.dinesmart.entity.Role;
import datn.nht.dinesmart.repository.AuditLogRepository;
import datn.nht.dinesmart.repository.ChatMessageRepository;
import datn.nht.dinesmart.security.CurrentUser;
import datn.nht.dinesmart.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** Chỉ ADMIN (xem SecurityConfig): thực đơn, bàn, voucher, cài đặt, nhân viên, báo cáo, nhật ký. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final MenuService menu;
    private final ImageStorageService images;
    private final TableService tables;
    private final VoucherService vouchers;
    private final SettingService settings;
    private final UserService users;
    private final ReportService reports;
    private final AuditLogRepository auditRepo;
    private final ChatMessageRepository chatRepo;

    private static Long me() {
        return CurrentUser.require().id();
    }

    // ---------------- danh mục & món ----------------
    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return menu.allCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest r) {
        return menu.createCategory(r, me());
    }

    @PutMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r) {
        return menu.updateCategory(id, r, me());
    }

    @GetMapping("/menu/items")
    public List<MenuItemDto> items(@RequestParam(required = false) Long categoryId, @RequestParam(required = false) String q) {
        return menu.adminList(categoryId, q);
    }

    @PostMapping("/menu/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemDto createItem(@Valid @RequestBody MenuItemRequest r) {
        return menu.create(r, me());
    }

    @PutMapping("/menu/items/{id}")
    public MenuItemDto updateItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest r) {
        return menu.update(id, r, me());
    }

    @PatchMapping("/menu/items/{id}/status")
    public MenuItemDto itemStatus(@PathVariable Long id, @Valid @RequestBody MenuStatusRequest r) {
        return menu.setStatus(id, r.status(), me());
    }

    /** "Xoá" món = ẩn (HIDDEN): món đã có trong hóa đơn cũ không được xoá vật lý (app_rw không có quyền DELETE). */
    @DeleteMapping("/menu/items/{id}")
    public ResponseEntity<Void> hideItem(@PathVariable Long id) {
        menu.setStatus(id, MenuStatus.HIDDEN, me());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/menu/images", consumes = "multipart/form-data")
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return new UploadResponse(images.saveMenuImage(file));
    }

    // ---------------- bàn ----------------
    @GetMapping("/tables")
    public List<TableDto> tables() {
        return tables.list();
    }

    @PostMapping("/tables")
    @ResponseStatus(HttpStatus.CREATED)
    public TableDto createTable(@Valid @RequestBody TableRequest r) {
        return tables.create(r, me());
    }

    @PutMapping("/tables/{id}")
    public TableDto updateTable(@PathVariable Long id, @Valid @RequestBody TableRequest r) {
        return tables.update(id, r, me());
    }

    // ---------------- voucher ----------------
    @GetMapping("/vouchers")
    public List<VoucherDto> vouchers() {
        return vouchers.list();
    }

    @PostMapping("/vouchers")
    @ResponseStatus(HttpStatus.CREATED)
    public VoucherDto createVoucher(@Valid @RequestBody VoucherRequest r) {
        return vouchers.create(r, me());
    }

    @PutMapping("/vouchers/{id}")
    public VoucherDto updateVoucher(@PathVariable Long id, @Valid @RequestBody VoucherRequest r) {
        return vouchers.update(id, r, me());
    }

    // ---------------- cài đặt ----------------
    @GetMapping("/settings")
    public SettingDto settings() {
        return settings.adminView();
    }

    @PutMapping("/settings")
    public SettingDto updateSettings(@Valid @RequestBody SettingRequest r) {
        return settings.update(r, me());
    }

    // ---------------- người dùng ----------------
    @GetMapping("/users")
    public List<UserDto> users(@RequestParam(required = false) Role role) {
        return users.list(role);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserRequest r) {
        return users.create(r, me());
    }

    @PutMapping("/users/{id}")
    public UserDto updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest r) {
        return users.update(id, r, me());
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordByAdminRequest r) {
        users.resetPassword(id, r.newPassword(), me());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/unlock")
    public UserDto unlock(@PathVariable Long id) {
        return users.unlock(id, me());
    }

    // ---------------- báo cáo ----------------
    @GetMapping("/reports/dashboard")
    public DashboardDto dashboard() {
        return reports.dashboard();
    }

    @GetMapping("/reports/revenue")
    public List<RevenuePoint> revenue(@RequestParam(defaultValue = "day") String granularity,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.revenue(granularity, from, to);
    }

    @GetMapping("/reports/payment-methods")
    public List<PaymentMethodStat> methods(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.byMethod(from, to);
    }

    @GetMapping("/reports/popular-items")
    public List<PopularItem> popular(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(defaultValue = "10") int limit) {
        return reports.popular(from, to, limit);
    }

    @GetMapping("/reports/reservations")
    public ReservationStats reservationStats(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.reservationStats(from, to.plusDays(1));
    }

    // ---------------- nhật ký ----------------
    @GetMapping("/audit-logs")
    @Transactional(readOnly = true)
    public List<AuditLogDto> audit(@RequestParam(required = false) String entityType,
                                   @RequestParam(defaultValue = "100") int limit) {
        var page = PageRequest.of(0, Math.max(1, Math.min(limit, 500)));
        var rows = entityType == null || entityType.isBlank() ? auditRepo.findAllByOrderByIdDesc(page)
                : auditRepo.findByEntityTypeOrderByIdDesc(entityType, page);
        return rows.stream().map(Mappers::audit).toList();
    }

    @GetMapping("/chat-logs")
    @Transactional(readOnly = true)
    public List<ChatLogDto> chatLogs(@RequestParam(required = false) ChatRole role, @RequestParam(defaultValue = "100") int limit) {
        var page = PageRequest.of(0, Math.max(1, Math.min(limit, 500)));
        var rows = role == null ? chatRepo.findAllByOrderByIdDesc(page) : chatRepo.findByRoleOrderByIdDesc(role, page);
        return rows.stream().map(Mappers::chat).toList();
    }
}
