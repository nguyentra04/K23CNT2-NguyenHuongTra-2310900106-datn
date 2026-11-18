package datn.nht.dinesmart.config;

import datn.nht.dinesmart.security.JwtAuthFilter;
import datn.nht.dinesmart.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AppProperties props;

    /** Hai filter là @Component: tắt đăng ký tự động vào servlet container để chỉ chạy trong chuỗi Spring Security. */
    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter f) {
        FilterRegistrationBean<JwtAuthFilter> r = new FilterRegistrationBean<>(f);
        r.setEnabled(false);
        return r;
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitRegistration(RateLimitFilter f) {
        FilterRegistrationBean<RateLimitFilter> r = new FilterRegistrationBean<>(f);
        r.setEnabled(false);
        return r;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Chấp nhận hash $2a/$2b/$2y — khớp hash BCrypt trong file SQL ($2b$10$...).
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)   // API stateless dùng Bearer token; refresh cookie là SameSite=Strict và chỉ gửi tới /api/auth
            .cors(c -> c.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h
                .contentTypeOptions(o -> {})
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; img-src 'self' data: https:; style-src 'self' https://fonts.googleapis.com 'unsafe-inline'; "
                    + "font-src 'self' https://fonts.gstatic.com; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'")))
            .authorizeHttpRequests(a -> a
                // --- tĩnh ---
                .requestMatchers("/", "/index.html", "/staff.html", "/reset.html", "/favicon.ico", "/css/**", "/js/**", "/uploads/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // --- công khai ---
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                        "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/menu/**", "/api/settings/public", "/api/tables/availability",
                        "/api/reservations/lookup").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservations", "/api/reservations/cancel", "/api/chat").permitAll()
                // --- phân quyền theo vai trò ---
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/staff/**").hasAnyRole("CASHIER", "ADMIN")
                .requestMatchers("/api/kitchen/**").hasAnyRole("KITCHEN", "CASHIER", "ADMIN")
                .requestMatchers("/api/auth/me", "/api/auth/change-password", "/api/me/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> writeJson(res, 401, "UNAUTHORIZED", "Vui lòng đăng nhập"))
                .accessDeniedHandler((req, res, ex) -> writeJson(res, 403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này")))
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeJson(jakarta.servlet.http.HttpServletResponse res, int status, String code, String msg)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        res.getOutputStream().write(("{\"status\":" + status + ",\"code\":\"" + code + "\",\"message\":\"" + msg + "\"}")
                .getBytes(StandardCharsets.UTF_8));
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.cors().allowedOrigins());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        c.setAllowCredentials(true);
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/api/**", c);
        return s;
    }
}
