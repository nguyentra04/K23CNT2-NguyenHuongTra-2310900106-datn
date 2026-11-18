package datn.nht.dinesmart.security;

import datn.nht.dinesmart.config.AppProperties;
import datn.nht.dinesmart.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class JwtService {
    private final AppProperties props;
    private SecretKey key;

    public JwtService(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret phải dài ít nhất 32 byte");
        }
        if (props.jwt().secret().startsWith("dev-only-secret")) {
            log.warn("Đang dùng JWT secret mặc định — CHỈ dành cho phát triển. Đặt biến môi trường JWT_SECRET khi triển khai.");
        }
        key = Keys.hmacShaKeyFor(secret);
    }

    /** Access token sống ngắn; claim "ver" = users.token_version để thu hồi hàng loạt. */
    public String createAccessToken(User u) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("email", u.getEmail())
                .claim("role", u.getRole().name())
                .claim("ver", u.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.jwt().accessTtlMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public int accessTtlSeconds() {
        return props.jwt().accessTtlMinutes() * 60;
    }
}
