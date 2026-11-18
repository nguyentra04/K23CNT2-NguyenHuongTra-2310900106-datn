package datn.nht.dinesmart.service;

import datn.nht.dinesmart.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Gọi Gemini REST API (generateContent) với function calling.
 * Dùng JsonNode để giữ NGUYÊN các phần (parts) mô hình trả về — kể cả thought signature — khi gửi lại ở lượt tiếp theo.
 * Không gửi temperature/top_p/top_k: các model Gemini mới đã bỏ các tham số lấy mẫu này.
 */
@Slf4j
@Component
public class GeminiClient {

    public static class GeminiException extends RuntimeException {
        public final boolean timeout;
        public GeminiException(String message, boolean timeout, Throwable cause) {
            super(message, cause);
            this.timeout = timeout;
        }
    }

    private final AppProperties.Gemini cfg;
    private final ObjectMapper mapper;
    private final RestClient rest;

    public GeminiClient(AppProperties props, ObjectMapper mapper) {
        this.cfg = props.gemini();
        this.mapper = mapper;
        Duration timeout = Duration.ofSeconds(cfg.timeoutSeconds());
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(timeout);
        this.rest = RestClient.builder().baseUrl(cfg.baseUrl()).requestFactory(factory).build();
    }

    public boolean configured() {
        return cfg.apiKey() != null && !cfg.apiKey().isBlank();
    }

    public String model() {
        return cfg.model();
    }

    public ObjectNode newRequest(String systemPrompt, ArrayNode contents, ArrayNode functionDeclarations) {
        ObjectNode body = mapper.createObjectNode();
        body.putObject("systemInstruction").putArray("parts").addObject().put("text", systemPrompt);
        body.set("contents", contents);
        if (functionDeclarations != null && !functionDeclarations.isEmpty()) {
            body.putArray("tools").addObject().set("functionDeclarations", functionDeclarations);
            body.putObject("toolConfig").putObject("functionCallingConfig").put("mode", "AUTO");
        }
        body.putObject("generationConfig").put("maxOutputTokens", 1500);
        return body;
    }

    /** Trả về toàn bộ JSON phản hồi. Ném GeminiException khi lỗi mạng/timeout/HTTP. */
    public JsonNode generate(ObjectNode request) {
        if (!configured()) throw new GeminiException("Chưa cấu hình GEMINI_API_KEY", false, null);
        try {
            String raw = rest.post()
                    .uri("/models/{model}:generateContent", cfg.model())
                    .header("x-goog-api-key", cfg.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            return mapper.readTree(raw);
        } catch (RestClientResponseException e) {
            // Không ghi header/khoá API vào log; chỉ mã trạng thái và một đoạn đầu thân phản hồi.
            String snippet = e.getResponseBodyAsString();
            log.warn("Gemini HTTP {}: {}", e.getStatusCode().value(), snippet.length() > 300 ? snippet.substring(0, 300) : snippet);
            throw new GeminiException("Gemini trả về lỗi HTTP " + e.getStatusCode().value(), false, e);
        } catch (ResourceAccessException e) {
            boolean to = isTimeout(e);
            throw new GeminiException(to ? "Hết thời gian chờ phản hồi từ mô hình" : "Không kết nối được tới Gemini", to, e);
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Không đọc được phản hồi từ Gemini", false, e);
        }
    }

    private static boolean isTimeout(Throwable t) {
        for (; t != null; t = t.getCause()) {
            if (t instanceof HttpTimeoutException || t instanceof SocketTimeoutException) return true;
        }
        return false;
    }
}
