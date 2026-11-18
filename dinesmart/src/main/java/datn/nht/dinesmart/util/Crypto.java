package datn.nht.dinesmart.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class Crypto {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";   // bỏ ký tự dễ nhầm: 0 O 1 I

    private Crypto() {}

    /** SHA-256 -> 64 ký tự hex (khớp cột CHAR(64)). */
    public static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Token ngẫu nhiên URL-safe, `bytes` byte entropy. */
    public static String randomToken(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** Session token chatbot: 32 ký tự hex (128 bit) — thoả CHECK LEN >= 32. */
    public static String randomHex32() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    /** Mã đặt bàn 10 ký tự (cột code VARCHAR(12)), vd "DS7K2M9QXA". */
    public static String reservationCode() {
        StringBuilder sb = new StringBuilder("DS");
        for (int i = 0; i < 8; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        return sb.toString();
    }
}
