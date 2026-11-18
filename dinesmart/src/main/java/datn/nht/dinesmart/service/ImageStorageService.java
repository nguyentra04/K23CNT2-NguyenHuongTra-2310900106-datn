package datn.nht.dinesmart.service;

import datn.nht.dinesmart.config.AppProperties;
import datn.nht.dinesmart.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Lưu ảnh món. Kiểm tra bằng "magic bytes" (không tin Content-Type/đuôi file do client gửi). */
@Service
@RequiredArgsConstructor
public class ImageStorageService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final AppProperties props;

    public String saveMenuImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Chưa chọn ảnh");
        if (file.getSize() > MAX_BYTES) throw ApiException.badRequest("Ảnh tối đa 5 MB");
        try {
            byte[] head = new byte[12];
            int n;
            try (InputStream in = file.getInputStream()) {
                n = in.readNBytes(head, 0, head.length);
            }
            String ext = detectExt(head, n);
            if (ext == null) throw ApiException.badRequest("Chỉ chấp nhận ảnh JPG, PNG hoặc WebP");
            Path dir = Path.of(props.upload().dir(), "menu").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "." + ext;     // tên do server sinh -> không có path traversal
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(name));
            }
            return "/uploads/menu/" + name;
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_FAILED", "Không lưu được ảnh");
        }
    }

    private static String detectExt(byte[] b, int n) {
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }
}
