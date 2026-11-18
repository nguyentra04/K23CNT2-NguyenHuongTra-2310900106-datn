package datn.nht.dinesmart.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(int status, String code, String message, Map<String, String> fields) {}

    private ResponseEntity<ErrorBody> body(HttpStatus s, String code, String msg, Map<String, String> fields) {
        return ResponseEntity.status(s).body(new ErrorBody(s.value(), code, msg, fields));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> api(ApiException e) {
        return body(e.getStatus(), e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException e) {
        Map<String, String> f = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> f.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "VALIDATION", "Dữ liệu không hợp lệ", f);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorBody> constraint(ConstraintViolationException e) {
        return body(HttpStatus.BAD_REQUEST, "VALIDATION", "Dữ liệu không hợp lệ", null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ErrorBody> badInput(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Yêu cầu không hợp lệ (sai định dạng hoặc thiếu tham số)", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorBody> method(HttpRequestMethodNotSupportedException e) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorBody> denied(AccessDeniedException e) {
        return body(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này", null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorBody> optimistic(OptimisticLockingFailureException e) {
        return body(HttpStatus.CONFLICT, "STALE", "Dữ liệu vừa được người khác thay đổi, hãy tải lại rồi thử lại", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorBody> integrity(DataIntegrityViolationException e) {
        log.warn("Vi phạm ràng buộc dữ liệu: {}", rootMessage(e));
        return body(HttpStatus.CONFLICT, "CONSTRAINT", "Dữ liệu vi phạm ràng buộc (trùng lặp hoặc đang được sử dụng)", null);
    }

    /** Lỗi THROW 50xxx từ trigger/thủ tục của file SQL: thông điệp do chính DB định nghĩa nên an toàn để trả về. */
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorBody> dataAccess(DataAccessException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException se && se.getErrorCode() >= 50000 && se.getErrorCode() < 60000) {
                return body(HttpStatus.CONFLICT, "DB_RULE_" + se.getErrorCode(), se.getMessage(), null);
            }
        }
        log.error("Lỗi truy cập dữ liệu", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "DB_ERROR", "Lỗi cơ sở dữ liệu", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> other(Exception e) {
        log.error("Lỗi không mong đợi", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "Có lỗi xảy ra, vui lòng thử lại sau", null);
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage();
    }
}
