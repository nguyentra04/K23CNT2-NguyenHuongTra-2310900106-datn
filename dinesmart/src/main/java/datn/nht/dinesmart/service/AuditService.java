package datn.nht.dinesmart.service;

import datn.nht.dinesmart.entity.AuditLog;
import datn.nht.dinesmart.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Ghi audit_log (chỉ INSERT). Tham gia cùng giao dịch của nghiệp vụ để nhật ký và dữ liệu luôn khớp nhau. */
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public void log(Long userId, String action, String entityType, Long entityId, Object oldValue, Object newValue) {
        AuditLog a = new AuditLog();
        a.setUserId(userId);
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setOldValue(toJson(oldValue));
        a.setNewValue(toJson(newValue));
        repo.save(a);
    }

    private String toJson(Object o) {
        if (o == null) return null;          // cột có CHECK ISJSON hoặc NULL
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    /** Dựng map cho phép giá trị null: kv("status", "OPEN", "total", 1000). */
    public static Map<String, Object> kv(Object... pairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) m.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return m;
    }
}
