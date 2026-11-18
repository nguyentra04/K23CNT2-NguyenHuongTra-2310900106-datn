package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.BillingDtos.*;
import datn.nht.dinesmart.entity.*;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lập hóa đơn và thu tiền. Thứ tự bắt buộc để trigger đối soát trg_orders_close_reconcile chấp nhận:
 *   INSERT payment -> UPDATE invoice.paid_at -> UPDATE orders.status = 'CLOSED' (flush từng bước, cùng một giao dịch).
 */
@Service
@RequiredArgsConstructor
public class BillingService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DiningOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final VoucherRepository voucherRepo;
    private final DiningTableRepository tables;
    private final SettingService settings;
    private final VoucherService vouchers;
    private final ReservationService reservationService;
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    // =============================== LẬP HÓA ĐƠN ===============================

    @Transactional
    public InvoiceDto bill(Long orderId, BillRequest req, Long staffId) {
        DiningOrder o = orders.findWithTableById(orderId).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        if (o.getStatus() != OrderStatus.OPEN) throw ApiException.conflict("Đơn không ở trạng thái đang gọi món");
        if (invoices.findByOrderId(orderId).isPresent()) throw ApiException.conflict("Đơn này đã có hóa đơn");

        List<OrderItem> items = orderItems.findByOrderIdOrderByIdAsc(orderId);
        boolean unfinished = items.stream().anyMatch(i ->
                i.getStatus() == OrderItemStatus.PENDING || i.getStatus() == OrderItemStatus.PREPARING);
        if (unfinished) throw ApiException.conflict("Còn món chưa làm xong — hãy chờ bếp hoàn tất hoặc huỷ món trước khi tính tiền");

        BigDecimal subtotal = orderItems.sumActive(orderId, OrderItemStatus.CANCELLED);
        if (subtotal.signum() <= 0) throw ApiException.badRequest("Đơn chưa có món nào để tính tiền");

        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (req != null && req.voucherCode() != null && !req.voucherCode().isBlank()) {
            voucher = vouchers.find(req.voucherCode());
            discount = vouchers.discountFor(voucher, subtotal, now);
        }
        BigDecimal vatPercent = settings.get().getVatPercent();
        BigDecimal base = subtotal.subtract(discount);
        BigDecimal vat = base.multiply(vatPercent).divide(HUNDRED, 0, RoundingMode.HALF_UP);

        Invoice inv = new Invoice();
        inv.setOrderId(orderId);
        inv.setInvoiceNo(nextInvoiceNo());
        inv.setVoucherId(discount.signum() > 0 && voucher != null ? voucher.getId() : null);
        inv.setSubtotal(subtotal);
        inv.setDiscountAmount(discount);
        inv.setVatAmount(vat);
        inv.setTotalAmount(base.add(vat));           // ck_invoice_total: total = subtotal - discount + vat
        inv.setVatPercent(vatPercent);
        inv.setIssuedBy(staffId);
        inv.setIssuedAt(now);
        invoices.saveAndFlush(inv);

        o.setStatus(OrderStatus.BILLED);
        orders.saveAndFlush(o);
        audit.log(staffId, "INVOICE_ISSUED", "invoice", inv.getId(), null,
                AuditService.kv("invoice_no", inv.getInvoiceNo(), "total_amount", inv.getTotalAmount()));
        return toDto(inv);
    }

    /** Cấp số hóa đơn bằng thủ tục usp_next_invoice_no (SEQUENCE seq_invoice_no). Cùng kết nối/giao dịch với JPA. */
    private String nextInvoiceNo() {
        return jdbc.execute((ConnectionCallback<String>) con -> {
            try (CallableStatement cs = con.prepareCall("{call usp_next_invoice_no(?, ?)}")) {
                cs.setDate(1, Date.valueOf(LocalDate.now()));
                cs.registerOutParameter(2, Types.VARCHAR);
                cs.execute();
                return cs.getString(2);
            }
        });
    }

    // =============================== THU TIỀN ===============================

    @Transactional
    public InvoiceDto pay(Long invoiceId, PaymentRequest req, Long staffId) {
        Invoice inv = invoices.lockById(invoiceId).orElseThrow(() -> ApiException.notFound("Không tìm thấy hóa đơn"));
        if (inv.getPaidAt() != null) throw ApiException.conflict("Hóa đơn đã được thanh toán đủ");
        DiningOrder o = orders.findWithTableById(inv.getOrderId()).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn"));
        if (o.getStatus() != OrderStatus.BILLED) throw ApiException.conflict("Đơn không ở trạng thái chờ thanh toán");

        BigDecimal paid = payments.sumByStatus(invoiceId, PaymentStatus.SUCCESS);
        BigDecimal remaining = inv.getTotalAmount().subtract(paid);
        BigDecimal amount = req.amount().setScale(0, RoundingMode.HALF_UP);
        if (amount.compareTo(remaining) > 0) {
            throw ApiException.badRequest("Số tiền vượt quá số còn phải thu (" + remaining.toPlainString() + "đ)");
        }
        String ref = req.transactionRef() == null ? null : req.transactionRef().trim();
        if (req.method() != PaymentMethod.CASH) {
            if (ref == null || ref.isEmpty()) throw ApiException.badRequest("Cần nhập mã giao dịch cho hình thức này");
            if (payments.existsByMethodAndTransactionRef(req.method(), ref)) {
                throw ApiException.conflict("Mã giao dịch này đã được ghi nhận");
            }
        } else {
            ref = null;
        }

        LocalDateTime now = LocalDateTime.now();
        Payment p = new Payment();
        p.setInvoiceId(invoiceId);
        p.setMethod(req.method());
        p.setAmount(amount);
        p.setStatus(PaymentStatus.SUCCESS);
        p.setTransactionRef(ref);
        p.setReceivedBy(staffId);
        p.setPaidAt(now);
        payments.saveAndFlush(p);                                   // (1) payment

        if (paid.add(amount).compareTo(inv.getTotalAmount()) == 0) {
            LocalDateTime paidAt = now.isBefore(inv.getIssuedAt()) ? inv.getIssuedAt() : now;   // ck_invoice_paid
            inv.setPaidAt(paidAt);
            invoices.saveAndFlush(inv);                             // (2) invoice.paid_at

            if (inv.getVoucherId() != null && voucherRepo.tryConsume(inv.getVoucherId()) == 0) {
                throw ApiException.conflict("Voucher đã hết lượt dùng, không thể hoàn tất thanh toán");
            }
            o.setStatus(OrderStatus.CLOSED);
            orders.saveAndFlush(o);                                 // (3) orders CLOSED — trigger đối soát chạy ở đây

            DiningTable t = o.getTable();
            if (t != null) {
                t.setStatus(TableStatus.CLEANING);
                tables.save(t);
            }
            if (o.getReservationId() != null) {
                Reservation r = reservationService.findEntity(o.getReservationId());
                if (r.getStatus() == ReservationStatus.CHECKED_IN) reservationService.completeInternal(r, staffId);
            }
            audit.log(staffId, "INVOICE_PAID", "invoice", invoiceId, null,
                    AuditService.kv("invoice_no", inv.getInvoiceNo(), "total_amount", inv.getTotalAmount()));
        }
        return toDto(inv);
    }

    // =============================== TRUY VẤN ===============================

    @Transactional(readOnly = true)
    public InvoiceDto get(Long id) {
        return toDto(invoices.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy hóa đơn")));
    }

    @Transactional(readOnly = true)
    public InvoiceDto getByOrder(Long orderId) {
        return invoices.findByOrderId(orderId).map(this::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> recent(int limit) {
        return invoices.findAllByOrderByIdDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 100)))).stream().map(this::toDto).toList();
    }

    InvoiceDto toDto(Invoice i) {
        List<PaymentDto> pays = payments.findByInvoiceIdOrderByIdAsc(i.getId()).stream().map(Mappers::payment).toList();
        BigDecimal paid = payments.sumByStatus(i.getId(), PaymentStatus.SUCCESS);
        String code = i.getVoucherId() == null ? null : voucherRepo.findById(i.getVoucherId()).map(Voucher::getCode).orElse(null);
        return new InvoiceDto(i.getId(), i.getInvoiceNo(), i.getOrderId(), code, i.getSubtotal(), i.getDiscountAmount(),
                i.getVatAmount(), i.getTotalAmount(), i.getVatPercent(), i.getIssuedAt(), i.getPaidAt(), paid,
                i.getTotalAmount().subtract(paid), pays);
    }
}
