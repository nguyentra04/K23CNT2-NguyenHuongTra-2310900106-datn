package datn.nht.dinesmart.controller;

import datn.nht.dinesmart.config.AppProperties;
import datn.nht.dinesmart.dto.AuthDtos.*;
import datn.nht.dinesmart.security.CurrentUser;
import datn.nht.dinesmart.service.AuthService;
import datn.nht.dinesmart.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String COOKIE = "refresh_token";

    private final AuthService auth;
    private final UserService users;
    private final AppProperties props;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r, HttpServletResponse res) {
        return withCookie(auth.register(r), res);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r, HttpServletResponse res) {
        return withCookie(auth.login(r), res);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = COOKIE, required = false) String token, HttpServletResponse res) {
        try {
            return withCookie(auth.refresh(token), res);
        } catch (RuntimeException e) {
            clearCookie(res);       // token hỏng/đã dùng -> xoá cookie để client về trang đăng nhập
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = COOKIE, required = false) String token, HttpServletResponse res) {
        auth.logout(token);
        clearCookie(res);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgot(@Valid @RequestBody ForgotPasswordRequest r) {
        auth.forgotPassword(r.email());
        return Map.of("message", "Nếu email tồn tại, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu");
    }

    @PostMapping("/reset-password")
    public Map<String, String> reset(@Valid @RequestBody ResetPasswordRequest r) {
        auth.resetPassword(r);
        return Map.of("message", "Đã đặt lại mật khẩu, hãy đăng nhập lại");
    }

    @GetMapping("/me")
    public UserDto me() {
        return users.me(CurrentUser.require().id());
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> change(@Valid @RequestBody ChangePasswordRequest r, HttpServletResponse res) {
        auth.changePassword(CurrentUser.require().id(), r);
        clearCookie(res);
        return ResponseEntity.noContent().build();
    }

    // ---- cookie refresh token: HttpOnly + SameSite=Strict, chỉ gửi tới /api/auth ----
    private AuthResponse withCookie(AuthService.Session s, HttpServletResponse res) {
        ResponseCookie c = ResponseCookie.from(COOKIE, s.refreshToken())
                .httpOnly(true).secure(props.jwt().cookieSecure()).sameSite("Strict").path("/api/auth")
                .maxAge(Duration.ofDays(props.jwt().refreshTtlDays())).build();
        res.addHeader(HttpHeaders.SET_COOKIE, c.toString());
        return s.response();
    }

    private void clearCookie(HttpServletResponse res) {
        ResponseCookie c = ResponseCookie.from(COOKIE, "")
                .httpOnly(true).secure(props.jwt().cookieSecure()).sameSite("Strict").path("/api/auth")
                .maxAge(Duration.ZERO).build();
        res.addHeader(HttpHeaders.SET_COOKIE, c.toString());
    }
}
