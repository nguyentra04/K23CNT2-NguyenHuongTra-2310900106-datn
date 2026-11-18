package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.Payment;
import datn.nht.dinesmart.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceIdOrderByIdAsc(Long invoiceId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.invoiceId = :invoiceId and p.status = :status")
    BigDecimal sumByStatus(@Param("invoiceId") Long invoiceId, @Param("status") PaymentStatus status);

    boolean existsByMethodAndTransactionRef(datn.nht.dinesmart.entity.PaymentMethod method, String transactionRef);
}
