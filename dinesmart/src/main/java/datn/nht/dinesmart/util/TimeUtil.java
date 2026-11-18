package datn.nht.dinesmart.util;

import java.time.LocalTime;

public final class TimeUtil {
    private TimeUtil() {}

    /** Cộng phút nhưng không vượt qua nửa đêm (tránh quay vòng về 00:xx). */
    public static LocalTime plusClamped(LocalTime t, int minutes) {
        int total = t.getHour() * 60 + t.getMinute() + minutes;
        if (total >= 24 * 60) return LocalTime.of(23, 59, 59);
        if (total < 0) return LocalTime.MIN;
        return LocalTime.of(total / 60, total % 60);
    }

    public static LocalTime minusClamped(LocalTime t, int minutes) {
        return plusClamped(t, -minutes);
    }
}
