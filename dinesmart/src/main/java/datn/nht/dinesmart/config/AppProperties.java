package datn.nht.dinesmart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        Cors cors,
        Jwt jwt,
        Security security,
        Bootstrap bootstrap,
        Mail mail,
        Upload upload,
        Gemini gemini,
        Housekeeping housekeeping) {

    public record Cors(List<String> allowedOrigins) {}

    public record Jwt(String secret, int accessTtlMinutes, int refreshTtlDays, boolean cookieSecure) {}

    public record Security(int maxFailedLogins, int lockMinutes, int resetTokenMinutes) {}

    public record Bootstrap(String adminEmail, String adminPassword) {}

    public record Mail(boolean logResetLink, String publicBaseUrl) {}

    public record Upload(String dir) {}

    public record Gemini(String apiKey, String baseUrl, String model, int timeoutSeconds,
                         int maxToolRounds, int maxHistoryMessages) {}

    public record Housekeeping(boolean enabled, long intervalMs) {}
}
