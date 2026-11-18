package datn.nht.dinesmart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn tần suất theo IP (cửa sổ cố định 1 phút, lưu trong bộ nhớ) cho các endpoint công khai dễ bị lạm dụng.
 * Chạy nhiều instance phía sau load balancer thì thay bằng Redis/bucket4j.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Window(long minute, int count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private int limitFor(HttpServletRequest req) {
        String p = req.getRequestURI();
        String m = req.getMethod();
        if (!"POST".equals(m)) return 0;
        if (p.equals("/api/auth/login") || p.equals("/api/auth/register")
                || p.equals("/api/auth/forgot-password") || p.equals("/api/auth/reset-password")) return 10;
        if (p.equals("/api/chat")) return 30;
        if (p.equals("/api/reservations") || p.equals("/api/reservations/cancel")) return 10;
        return 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        int limit = limitFor(req);
        if (limit > 0) {
            long minute = System.currentTimeMillis() / 60_000;
            String key = req.getRemoteAddr() + "|" + req.getRequestURI();
            Window w = windows.merge(key, new Window(minute, 1),
                    (old, fresh) -> old.minute() == minute ? new Window(minute, old.count() + 1) : fresh);
            if (w.count() > limit) {
                res.setStatus(429);
                res.setHeader("Retry-After", "60");
                res.setContentType("application/json;charset=UTF-8");
                res.getOutputStream().write(
                        "{\"status\":429,\"code\":\"RATE_LIMIT\",\"message\":\"Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút\"}"
                                .getBytes(StandardCharsets.UTF_8));
                return;
            }
            if (windows.size() > 10_000) windows.entrySet().removeIf(e -> e.getValue().minute() < minute);
        }
        chain.doFilter(req, res);
    }
}
