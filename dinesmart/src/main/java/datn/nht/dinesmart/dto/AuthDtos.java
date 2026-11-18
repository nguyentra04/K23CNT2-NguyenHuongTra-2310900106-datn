package datn.nht.dinesmart.dto;

import datn.nht.dinesmart.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthDtos {
    private AuthDtos() {}

    public static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MSG = "Mật khẩu 8–72 ký tự, gồm ít nhất một chữ cái và một chữ số";
    public static final String PHONE_RULE = "^(\\+?\\d{9,15})$";

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String password,
            @NotBlank @Size(max = 100) String fullName,
            @Pattern(regexp = PHONE_RULE, message = "Số điện thoại không hợp lệ") String phone) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max = 72) String password) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String newPassword) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String newPassword) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String fullName,
            @Pattern(regexp = PHONE_RULE, message = "Số điện thoại không hợp lệ") String phone) {}

    public record UserDto(Long id, String email, String fullName, String phone, Role role, boolean active,
                          LocalDateTime lockedUntil, LocalDateTime createdAt) {}

    public record AuthResponse(String accessToken, int expiresInSeconds, UserDto user) {}
}
