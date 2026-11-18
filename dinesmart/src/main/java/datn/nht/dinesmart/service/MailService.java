package datn.nht.dinesmart.service;

import datn.nht.dinesmart.config.AppProperties;
import datn.nht.dinesmart.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Điểm cắm gửi email. Hiện chưa có SMTP: khi app.mail.log-reset-link=true, link đặt lại mật khẩu được ghi vào log
 * để phát triển/kiểm thử. TRIỂN KHAI THẬT: thay thân hàm bằng JavaMailSender và tắt log-reset-link.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final AppProperties props;

    public void sendPasswordReset(User user, String rawToken) {
        if (props.mail().logResetLink()) {
            log.warn("[DEV] Link đặt lại mật khẩu cho {}: {}/reset.html?token={}",
                    user.getEmail(), props.mail().publicBaseUrl(), rawToken);
        }
    }
}
