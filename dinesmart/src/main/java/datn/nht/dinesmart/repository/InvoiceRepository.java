package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrderId(Long orderId);

    Optional<Invoice> findByInvoiceNo(String invoiceNo);

    List<Invoice> findAllByOrderByIdDesc(Pageable page);

    /** Khoá dòng hóa đơn khi thu tiền để hai thu ngân không thanh toán chồng nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> lockById(@Param("id") Long id);
}
