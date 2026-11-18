package datn.nht.dinesmart.security;

import datn.nht.dinesmart.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Xác thực Bearer JWT. Ngoài chữ ký + hạn, luôn đối chiếu DB: tài khoản còn
 * hoạt động, không bị khoá tạm
 * và token_version khớp (đổi mật khẩu / khoá / đổi role -> JWT cũ vô hiệu
 * ngay).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.parse(header.substring(7).trim()).ifPresent(this::authenticate);
        }
        chain.doFilter(req, res);
    }

    private void authenticate(Claims c) {
        try {
            Long userId = Long.valueOf(c.getSubject());
            Integer ver = c.get("ver", Integer.class);
            userRepository.findById(userId)
                    .filter(u -> u.isActive() && !u.isLocked() && ver != null && u.getTokenVersion() == ver)
                    .ifPresent(u -> {
                        AuthUser principal = new AuthUser(u.getId(), u.getEmail(), u.getRole());
                        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        } catch (RuntimeException ignored) {
            // token sai định dạng -> coi như chưa đăng nhập
        }
    }
}
