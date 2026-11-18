package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Getter @Setter
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "invoice_no", nullable = false, length = 20, unique = true)
    private String invoiceNo;

    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "vat_amount", nullable = false, precision = 14, scale = 0)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 0)
    private BigDecimal totalAmount;

    @Column(name = "vat_percent", nullable = false, precision = 4, scale = 2)
    private BigDecimal vatPercent = BigDecimal.ZERO;

    @Column(name = "issued_by")
    private Long issuedBy;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    void onCreate() {
        if (issuedAt == null) issuedAt = LocalDateTime.now();
    }
}
