package datn.nht.dinesmart.repository;

import datn.nht.dinesmart.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByIdDesc(Pageable page);

    List<AuditLog> findByEntityTypeOrderByIdDesc(String entityType, Pageable page);
}
