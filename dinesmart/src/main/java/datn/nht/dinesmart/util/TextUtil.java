package datn.nht.dinesmart.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextUtil {
    private TextUtil() {}

    /** Bỏ dấu tiếng Việt, đ->d, chữ thường, gom khoảng trắng. Dùng cho menu_item.search_key và từ khoá tìm kiếm. */
    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        return n.length() > 200 ? n.substring(0, 200) : n;
    }

    public static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    public static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
