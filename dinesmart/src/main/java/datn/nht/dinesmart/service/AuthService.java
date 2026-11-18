package datn.nht.dinesmart.service;

import datn.nht.dinesmart.config.AppProperties;
import datn.nht.dinesmart.dto.AuthDtos.*;
import datn.nht.dinesmart.entity.RefreshToken;
import datn.nht.dinesmart.entity.Role;
import datn.nht.dinesmart.entity.User;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.RefreshTokenRepository;
import datn.nht.dinesmart.repository.UserRepository;
import datn.nht.dinesmart.security.JwtService;
import datn.nht.dinesmart.util.Crypto;
import datn.nht.dinesmart.util.TextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Kết quả nội bộ: refresh token thô chỉ tồn tại trong bộ nhớ để controller đặt vào cookie HttpOnly. */
    public record Session(AuthResponse response, String refreshToken) {}

    private static final String BAD_CREDENTIALS = "Email hoặc mật khẩu không đúng";
    /** Hash giả để thời gian phản hồi không lộ email có tồn tại hay không. */
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BtvyPSXLhzZh6Zt0vE0kF5kQY1Wy";

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProperties props;
    private final MailService mail;
    private final AuditService audit;

    @Transactional
    public Session register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email này đã được đăng ký");
        }
        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setPhone(TextUtil.blankToNull(r.phone()));
        u.setRole(Role.CUSTOMER);        // tự đăng ký luôn là khách; nhân viên do ADMIN tạo
        users.save(u);
        audit.log(u.getId(), "USER_REGISTERED", "user", u.getId(), null, AuditService.kv("email", email));
        return issue(u);
    }

    /** noRollbackFor: bộ đếm đăng nhập sai phải được lưu dù ta ném lỗi 401. */
    @Transactional(noRollbackFor = ApiException.class)
    public Session login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElse(null);
        if (u == null) {
            encoder.matches(r.password(), DUMMY_HASH);
            throw ApiException.unauthorized(BAD_CREDENTIALS);
        }
        if (u.isLocked()) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                    "Tài khoản tạm khoá do đăng nhập sai nhiều lần, vui lòng thử lại sau ít phút");
        }
        if (!encoder.matches(r.password(), u.getPassword())) {
            int fails = u.getFailedLoginCount() + 1;
            u.setFailedLoginCount(fails);
            if (fails >= props.security().maxFailedLogins()) {
                u.setLockedUntil(LocalDateTime.now().plusMinutes(props.security().lockMinutes()));
                u.setFailedLoginCount(0);
                audit.log(u.getId(), "LOGIN_LOCKED", "user", u.getId(), null, AuditService.kv("minutes", props.security().lockMinutes()));
            }
            users.save(u);
            throw ApiException.unauthorized(BAD_CREDENTIALS);
        }
        if (!u.isActive()) {
            throw ApiException.forbidden("Tài khoản đã bị vô hiệu hoá");
        }
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        users.save(u);
        return issue(u);
    }

    /**
     * Xoay vòng refresh token. Dùng lại token ĐÃ thu hồi = dấu hiệu bị đánh cắp -> thu hồi toàn bộ phiên của user.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Session refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw ApiException.unauthorized("Phiên đăng nhập đã hết hạn");
        String hash = Crypto.sha256Hex(rawToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("Phiên đăng nhập không hợp lệ"));
        LocalDateTime now = LocalDateTime.now();
        if (stored.getRevokedAt() != null) {
            refreshTokens.revokeAllByUser(stored.getUserId(), now);
            audit.log(stored.getUserId(), "REFRESH_REUSE_DETECTED", "user", stored.getUserId(), null, null);
            throw ApiException.unauthorized("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại");
        }
        if (refreshTokens.revokeIfActive(hash, now) == 0) {
            throw ApiException.unauthorized("Phiên đăng nhập đã hết hạn");
        }
        User u = users.findById(stored.getUserId()).filter(x -> x.isActive() && !x.isLocked())
                .orElseThrow(() -> ApiException.unauthorized("Tài khoản không khả dụng"));
        return issue(u);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.revokeIfActive(Crypto.sha256Hex(rawToken), LocalDateTime.now());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest r) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Vui lòng đăng nhập"));
        if (!encoder.matches(r.currentPassword(), u.getPassword())) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        setNewPassword(u, r.newPassword());
        audit.log(userId, "PASSWORD_CHANGED", "user", userId, null, null);
    }

    /** Luôn trả thành công để không lộ email nào đã đăng ký. */
    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(email.trim()).filter(User::isActive).ifPresent(u -> {
            String raw = Crypto.randomToken(32);
            u.setResetTokenHash(Crypto.sha256Hex(raw));
            u.setResetExpiresAt(LocalDateTime.now().plusMinutes(props.security().resetTokenMinutes()));
            users.save(u);
            mail.sendPasswordReset(u, raw);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest r) {
        User u = users.findByResetTokenHash(Crypto.sha256Hex(r.token()))
                .filter(x -> x.getResetExpiresAt() != null && x.getResetExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> ApiException.badRequest("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));
        setNewPassword(u, r.newPassword());
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        audit.log(u.getId(), "PASSWORD_RESET", "user", u.getId(), null, null);
    }

    /** Đổi/đặt lại mật khẩu: tăng token_version (vô hiệu mọi JWT cũ) và thu hồi mọi refresh token. */
    void setNewPassword(User u, String raw) {
        u.setPassword(encoder.encode(raw));
        u.setResetTokenHash(null);
        u.setResetExpiresAt(null);
        u.setTokenVersion(u.getTokenVersion() + 1);
        users.save(u);
        refreshTokens.revokeAllByUser(u.getId(), LocalDateTime.now());
    }

    private Session issue(User u) {
        String raw = Crypto.randomToken(48);
        RefreshToken t = new RefreshToken();
        t.setUserId(u.getId());
        t.setTokenHash(Crypto.sha256Hex(raw));
        t.setExpiresAt(LocalDateTime.now().plusDays(props.jwt().refreshTtlDays()));
        refreshTokens.save(t);
        AuthResponse res = new AuthResponse(jwt.createAccessToken(u), jwt.accessTtlSeconds(), Mappers.user(u));
        return new Session(res, raw);
    }
}
