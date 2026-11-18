package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.BillingDtos.*;
import datn.nht.dinesmart.entity.DiscountType;
import datn.nht.dinesmart.entity.Voucher;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {
    private final VoucherRepository vouchers;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<VoucherDto> list() {
        return vouchers.findAllByOrderByIdDesc().stream().map(Mappers::voucher).toList();
    }

    @Transactional
    public VoucherDto create(VoucherRequest r, Long actorId) {
        String code = r.code().trim().toUpperCase();
        if (vouchers.existsByCodeIgnoreCase(code)) throw ApiException.conflict("Mã voucher đã tồn tại");
        Voucher v = new Voucher();
        apply(v, r, code);
        vouchers.save(v);
        audit.log(actorId, "VOUCHER_CREATED", "voucher", v.getId(), null, AuditService.kv("code", code));
        return Mappers.voucher(v);
    }

    @Transactional
    public VoucherDto update(Long id, VoucherRequest r, Long actorId) {
        Voucher v = vouchers.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy voucher"));
        String code = r.code().trim().toUpperCase();
        if (!v.getCode().equalsIgnoreCase(code) && vouchers.existsByCodeIgnoreCase(code)) {
            throw ApiException.conflict("Mã voucher đã tồn tại");
        }
        if (r.usageLimit() != null && r.usageLimit() < v.getUsedCount()) {
            throw ApiException.badRequest("Giới hạn lượt dùng không được nhỏ hơn số lượt đã dùng (" + v.getUsedCount() + ")");
        }
        apply(v, r, code);
        audit.log(actorId, "VOUCHER_UPDATED", "voucher", id, null, AuditService.kv("code", code, "active", v.isActive()));
        return Mappers.voucher(vouchers.save(v));
    }

    private void apply(Voucher v, VoucherRequest r, String code) {
        if (!r.validTo().isAfter(r.validFrom())) throw ApiException.badRequest("Ngày hết hạn phải sau ngày bắt đầu");
        if (r.discountType() == DiscountType.PERCENT && r.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest("Giảm theo % không vượt quá 100");
        }
        v.setCode(code);
        v.setDescription(r.description());
        v.setDiscountType(r.discountType());
        v.setDiscountValue(r.discountValue());
        v.setMaxDiscount(r.maxDiscount() == null ? null : r.maxDiscount().setScale(0, RoundingMode.HALF_UP));
        v.setMinOrderAmount(r.minOrderAmount().setScale(0, RoundingMode.HALF_UP));
        v.setValidFrom(r.validFrom());
        v.setValidTo(r.validTo());
        v.setUsageLimit(r.usageLimit());
        v.setActive(r.active());
    }

    /** Kiểm tra mã trước khi lập hóa đơn (không trừ lượt dùng). */
    @Transactional(readOnly = true)
    public VoucherCheckResponse check(String code, BigDecimal subtotal) {
        try {
            Voucher v = vouchers.findByCodeIgnoreCase(code.trim()).orElse(null);
            BigDecimal d = discountFor(v, subtotal, LocalDateTime.now());
            return new VoucherCheckResponse(true, "Áp dụng thành công", d);
        } catch (ApiException e) {
            return new VoucherCheckResponse(false, e.getMessage(), BigDecimal.ZERO);
        }
    }

    @Transactional(readOnly = true)
    public Voucher find(String code) {
        return vouchers.findByCodeIgnoreCase(code.trim()).orElseThrow(() -> ApiException.badRequest("Mã voucher không tồn tại"));
    }

    /** Tính số tiền giảm hoặc ném lỗi giải thích vì sao không áp dụng được. discount luôn <= subtotal (ck_invoice_discount). */
    public BigDecimal discountFor(Voucher v, BigDecimal subtotal, LocalDateTime now) {
        if (v == null) throw ApiException.badRequest("Mã voucher không tồn tại");
        if (!v.isActive()) throw ApiException.badRequest("Voucher đã ngừng áp dụng");
        if (now.isBefore(v.getValidFrom()) || now.isAfter(v.getValidTo())) throw ApiException.badRequest("Voucher chưa đến hạn hoặc đã hết hạn");
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) throw ApiException.badRequest("Voucher đã hết lượt dùng");
        if (subtotal.compareTo(v.getMinOrderAmount()) < 0) {
            throw ApiException.badRequest("Đơn tối thiểu " + v.getMinOrderAmount().toPlainString() + "đ để dùng voucher này");
        }
        BigDecimal d;
        if (v.getDiscountType() == DiscountType.PERCENT) {
            d = subtotal.multiply(v.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (v.getMaxDiscount() != null && d.compareTo(v.getMaxDiscount()) > 0) d = v.getMaxDiscount();
        } else {
            d = v.getDiscountValue().setScale(0, RoundingMode.HALF_UP);
        }
        return d.min(subtotal);
    }
}
