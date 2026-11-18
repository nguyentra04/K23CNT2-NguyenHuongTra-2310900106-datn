package datn.nht.dinesmart.service;

import datn.nht.dinesmart.config.AppProperties;
import datn.nht.dinesmart.entity.Role;
import datn.nht.dinesmart.entity.User;
import datn.nht.dinesmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * File SQL chỉ tạo admin ĐANG KHOÁ với mật khẩu không ai biết (S4). Nếu đặt biến môi trường ADMIN_PASSWORD,
 * lúc khởi động ta gán mật khẩu đó (BCrypt) và bật tài khoản admin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String pw = props.bootstrap().adminPassword();
        if (pw == null || pw.isBlank()) return;
        String email = props.bootstrap().adminEmail().trim().toLowerCase();
        User u = users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User n = new User();
            n.setEmail(email);
            n.setFullName("Quản trị viên");
            n.setRole(Role.ADMIN);
            n.setPassword("!");
            return n;
        });
        boolean changed = u.getId() == null || !u.isActive() || !encoder.matches(pw, u.getPassword());
        if (!changed) return;
        u.setPassword(encoder.encode(pw));
        u.setRole(Role.ADMIN);
        u.setActive(true);
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        u.setTokenVersion(u.getTokenVersion() + 1);
        users.save(u);
        log.info("Đã thiết lập tài khoản quản trị {}", email);
    }
}
