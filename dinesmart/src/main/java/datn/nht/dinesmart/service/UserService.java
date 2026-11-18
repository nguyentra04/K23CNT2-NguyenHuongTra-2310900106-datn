package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.AdminDtos.*;
import datn.nht.dinesmart.dto.AuthDtos.UpdateProfileRequest;
import datn.nht.dinesmart.dto.AuthDtos.UserDto;
import datn.nht.dinesmart.entity.Role;
import datn.nht.dinesmart.entity.User;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.RefreshTokenRepository;
import datn.nht.dinesmart.repository.UserRepository;
import datn.nht.dinesmart.util.TextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public UserDto me(Long id) {
        return Mappers.user(find(id));
    }

    @Transactional
    public UserDto updateProfile(Long id, UpdateProfileRequest r) {
        User u = find(id);
        u.setFullName(r.fullName().trim());
        u.setPhone(TextUtil.blankToNull(r.phone()));
        return Mappers.user(users.save(u));
    }

    // ---------------- quản trị ----------------
    @Transactional(readOnly = true)
    public List<UserDto> list(Role roleFilter) {
        List<Role> roles = roleFilter != null ? List.of(roleFilter) : List.of(Role.values());
        return users.findByRoleInOrderByIdDesc(roles).stream().map(Mappers::user).toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest r, Long actorId) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Email đã tồn tại");
        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setPhone(TextUtil.blankToNull(r.phone()));
        u.setRole(r.role());
        users.save(u);
        audit.log(actorId, "USER_CREATED", "user", u.getId(), null, AuditService.kv("email", email, "role", r.role().name()));
        return Mappers.user(u);
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest r, Long actorId) {
        User u = find(id);
        boolean selfHarm = id.equals(actorId) && (r.role() != u.getRole() || !r.active());
        if (selfHarm) throw ApiException.badRequest("Không thể tự đổi vai trò hoặc tự khoá tài khoản của chính mình");

        boolean securityChange = r.role() != u.getRole() || r.active() != u.isActive();
        Object before = AuditService.kv("role", u.getRole().name(), "active", u.isActive());
        u.setFullName(r.fullName().trim());
        u.setPhone(TextUtil.blankToNull(r.phone()));
        u.setRole(r.role());
        u.setActive(r.active());
        if (securityChange) {
            u.setTokenVersion(u.getTokenVersion() + 1);                    // vô hiệu mọi JWT cũ
            refreshTokens.revokeAllByUser(u.getId(), LocalDateTime.now()); // và mọi refresh token
        }
        users.save(u);
        if (securityChange) {
            audit.log(actorId, "USER_ACCESS_CHANGED", "user", id, before,
                    AuditService.kv("role", r.role().name(), "active", r.active()));
        }
        return Mappers.user(u);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword, Long actorId) {
        User u = find(id);
        u.setPassword(encoder.encode(newPassword));
        u.setResetTokenHash(null);
        u.setResetExpiresAt(null);
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        u.setTokenVersion(u.getTokenVersion() + 1);
        users.save(u);
        refreshTokens.revokeAllByUser(id, LocalDateTime.now());
        audit.log(actorId, "PASSWORD_RESET_BY_ADMIN", "user", id, null, null);
    }

    @Transactional
    public UserDto unlock(Long id, Long actorId) {
        User u = find(id);
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        audit.log(actorId, "USER_UNLOCKED", "user", id, null, null);
        return Mappers.user(users.save(u));
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
    }
}
