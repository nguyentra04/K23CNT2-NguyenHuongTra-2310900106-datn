package datn.nht.dinesmart.security;

import datn.nht.dinesmart.entity.Role;

/** Principal đặt vào SecurityContext sau khi xác thực JWT. */
public record AuthUser(Long id, String email, Role role) {
    public boolean isStaff() {
        return role == Role.ADMIN || role == Role.CASHIER;
    }
}
