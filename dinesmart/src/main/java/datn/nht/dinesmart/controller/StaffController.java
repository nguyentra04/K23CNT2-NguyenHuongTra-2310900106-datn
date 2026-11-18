package datn.nht.dinesmart.controller;

import datn.nht.dinesmart.dto.BillingDtos.*;
import datn.nht.dinesmart.dto.OrderDtos.*;
import datn.nht.dinesmart.dto.ReservationDtos.*;
import datn.nht.dinesmart.entity.OrderStatus;
import datn.nht.dinesmart.entity.ReservationStatus;
import datn.nht.dinesmart.security.CurrentUser;
import datn.nht.dinesmart.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Thu ngân / quản lý: đặt bàn, bàn, gọi món, hóa đơn, thanh toán. (CASHIER + ADMIN, xem SecurityConfig) */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {
    private final ReservationService reservations;
    private final TableService tables;
    private final OrderService orders;
    private final BillingService billing;
    private final VoucherService vouchers;

    private static Long me() {
        return CurrentUser.require().id();
    }

    // ---------------- đặt bàn ----------------
    @GetMapping("/reservations")
    public List<ReservationDto> reservations(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @RequestParam(required = false) ReservationStatus status) {
        return reservations.listForStaff(date, status);
    }

    @GetMapping("/reservations/{id}")
    public ReservationDto reservation(@PathVariable Long id) {
        return reservations.detail(id);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDto createReservation(@Valid @RequestBody StaffReservationRequest r) {
        return reservations.createForStaff(r, me());
    }

    @PostMapping("/reservations/{id}/confirm")
    public ReservationDto confirm(@PathVariable Long id) {
        return reservations.confirm(id, me());
    }

    @PostMapping("/reservations/{id}/reject")
    public ReservationDto reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest r) {
        return reservations.reject(id, r.reason(), me());
    }

    @PostMapping("/reservations/{id}/cancel")
    public ReservationDto cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest r) {
        return reservations.cancelByStaff(id, r.reason(), me());
    }

    @PostMapping("/reservations/{id}/check-in")
    public ReservationDto checkIn(@PathVariable Long id) {
        return reservations.checkIn(id, me());
    }

    @PostMapping("/reservations/{id}/complete")
    public ReservationDto complete(@PathVariable Long id) {
        return reservations.complete(id, me());
    }

    @PostMapping("/reservations/{id}/no-show")
    public ReservationDto noShow(@PathVariable Long id) {
        return reservations.noShow(id, me());
    }

    // ---------------- bàn ----------------
    @GetMapping("/tables")
    public List<TableDto> tables() {
        return tables.list();
    }

    @PatchMapping("/tables/{id}/status")
    public TableDto tableStatus(@PathVariable Long id, @Valid @RequestBody TableStatusRequest r) {
        return tables.setStatus(id, r.status(), me());
    }

    // ---------------- đơn gọi món ----------------
    @GetMapping("/orders")
    public List<OrderDto> orders(@RequestParam(required = false) List<OrderStatus> status) {
        return orders.list(status);
    }

    @GetMapping("/orders/{id}")
    public OrderDto order(@PathVariable Long id) {
        return orders.get(id);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto openOrder(@Valid @RequestBody CreateOrderRequest r) {
        return orders.create(r, me());
    }

    @PostMapping("/orders/{id}/items")
    public OrderDto addItems(@PathVariable Long id, @Valid @RequestBody AddItemsRequest r) {
        return orders.addItems(id, r, me());
    }

    @PostMapping("/order-items/{itemId}/cancel")
    public OrderDto cancelItem(@PathVariable Long itemId, @Valid @RequestBody CancelRequest r) {
        return orders.cancelItem(itemId, r.reason(), me());
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderDto cancelOrder(@PathVariable Long id, @Valid @RequestBody CancelRequest r) {
        return orders.cancelOrder(id, r.reason(), me());
    }

    // ---------------- hóa đơn & thanh toán ----------------
    @PostMapping("/orders/{id}/invoice")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDto bill(@PathVariable Long id, @Valid @RequestBody(required = false) BillRequest r) {
        return billing.bill(id, r, me());
    }

    @GetMapping("/invoices")
    public List<InvoiceDto> invoices(@RequestParam(defaultValue = "30") int limit) {
        return billing.recent(limit);
    }

    @GetMapping("/invoices/{id}")
    public InvoiceDto invoice(@PathVariable Long id) {
        return billing.get(id);
    }

    @PostMapping("/invoices/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDto pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest r) {
        return billing.pay(id, r, me());
    }

    @GetMapping("/vouchers/check")
    public VoucherCheckResponse checkVoucher(@RequestParam String code, @RequestParam BigDecimal subtotal) {
        return vouchers.check(code, subtotal);
    }
}
