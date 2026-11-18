package datn.nht.dinesmart.service;

import datn.nht.dinesmart.entity.ReservationStatus;
import datn.nht.dinesmart.entity.RestaurantSetting;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Quy tắc đặt bàn lấy từ restaurant_setting: giờ mở cửa, ngày nghỉ, đặt trước tối thiểu/tối đa. */
@Component
@RequiredArgsConstructor
public class BookingRules {
    /** Trạng thái chiếm bàn khi kiểm tra trùng giờ. */
    public static final List<ReservationStatus> BUSY =
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);

    private final SettingService settings;

    public LocalTime endOf(RestaurantSetting s, LocalTime start) {
        return TimeUtil.plusClamped(start, s.getDurationMinutes());
    }

    /** Trả lý do không đặt được, hoặc rỗng nếu hợp lệ. */
    public Optional<String> violation(RestaurantSetting s, LocalDate date, LocalTime start, LocalDateTime now) {
        Optional<String> closed = settings.closedReason(s, date);
        if (closed.isPresent()) return Optional.of("Nhà hàng nghỉ ngày " + date + ": " + closed.get());

        LocalDateTime startAt = LocalDateTime.of(date, start);
        if (startAt.isBefore(now.plusMinutes(s.getMinAdvanceMinutes()))) {
            return Optional.of("Cần đặt trước ít nhất " + s.getMinAdvanceMinutes() + " phút");
        }
        if (date.isAfter(now.toLocalDate().plusDays(s.getMaxAdvanceDays()))) {
            return Optional.of("Chỉ nhận đặt trước tối đa " + s.getMaxAdvanceDays() + " ngày");
        }
        LocalDateTime endAt = startAt.plusMinutes(s.getDurationMinutes());
        if (start.isBefore(s.getOpenTime()) || endAt.toLocalDate().isAfter(date)
                || endAt.toLocalTime().isAfter(s.getCloseTime())) {
            return Optional.of("Ngoài giờ nhận đặt bàn (" + s.getOpenTime() + " – " + s.getCloseTime()
                    + ", mỗi lượt " + s.getDurationMinutes() + " phút)");
        }
        return Optional.empty();
    }

    public void validate(RestaurantSetting s, LocalDate date, LocalTime start, LocalDateTime now) {
        violation(s, date, start, now).ifPresent(msg -> {
            throw ApiException.badRequest(msg);
        });
    }
}
