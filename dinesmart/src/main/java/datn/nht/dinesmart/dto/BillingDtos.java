package datn.nht.dinesmart.dto;

import datn.nht.dinesmart.entity.DiscountType;
import datn.nht.dinesmart.entity.PaymentMethod;
import datn.nht.dinesmart.entity.PaymentStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BillingDtos {
    private BillingDtos() {}

    public record BillRequest(@Size(max = 30) String voucherCode) {}

    public record PaymentRequest(
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin("1") @Digits(integer = 14, fraction = 0) BigDecimal amount,
            @Size(max = 100) String transactionRef) {}

    public record PaymentDto(Long id, PaymentMethod method, BigDecimal amount, PaymentStatus status,
                             String transactionRef, LocalDateTime paidAt, LocalDateTime createdAt) {}

    public record InvoiceDto(Long id, String invoiceNo, Long orderId, String voucherCode,
                             BigDecimal subtotal, BigDecimal discountAmount, BigDecimal vatAmount,
                             BigDecimal totalAmount, BigDecimal vatPercent,
                             LocalDateTime issuedAt, LocalDateTime paidAt,
                             BigDecimal paidAmount, BigDecimal remaining, List<PaymentDto> payments) {}

    public record VoucherDto(Long id, String code, String description, DiscountType discountType,
                             BigDecimal discountValue, BigDecimal maxDiscount, BigDecimal minOrderAmount,
                             LocalDateTime validFrom, LocalDateTime validTo, Integer usageLimit,
                             int usedCount, boolean active) {}

    public record VoucherRequest(
            @NotBlank @Size(max = 30) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã chỉ gồm chữ, số, _ và -") String code,
            @Size(max = 255) String description,
            @NotNull DiscountType discountType,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal discountValue,
            @DecimalMin(value = "0", inclusive = false) BigDecimal maxDiscount,
            @NotNull @DecimalMin("0") BigDecimal minOrderAmount,
            @NotNull LocalDateTime validFrom,
            @NotNull LocalDateTime validTo,
            @Min(1) Integer usageLimit,
            boolean active) {}

    public record VoucherCheckResponse(boolean valid, String message, BigDecimal discountAmount) {}
}
