package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.MenuDtos.MenuItemDto;
import datn.nht.dinesmart.dto.ReservationDtos.AvailabilityResponse;
import datn.nht.dinesmart.dto.ReservationDtos.ReservationDto;
import datn.nht.dinesmart.dto.ReservationDtos.ReservationRequest;
import datn.nht.dinesmart.dto.SettingDtos.FaqDto;
import datn.nht.dinesmart.entity.CategoryType;
import datn.nht.dinesmart.entity.EventType;
import datn.nht.dinesmart.entity.MenuStatus;
import datn.nht.dinesmart.entity.ReservationSource;
import datn.nht.dinesmart.entity.RestaurantSetting;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.MenuSearchCriteria;
import datn.nht.dinesmart.util.TextUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Các hàm Gemini được phép gọi (FR 20). Chatbot KHÔNG đọc DB trực tiếp: mọi dữ liệu đi qua các service nghiệp vụ,
 * nên cùng quy tắc (ẩn món HIDDEN, kiểm tra trùng bàn, giờ mở cửa...) áp dụng như web.
 * Kết quả lỗi nghiệp vụ trả về dạng {"error": "..."} để mô hình giải thích cho khách, không ném ngoại lệ.
 */
@Service
@RequiredArgsConstructor
public class ChatToolService {
    private static final int MAX_ITEMS = 12;

    private final MenuService menu;
    private final TableService tableService;
    private final ReservationService reservations;
    private final SettingService settings;
    private final ObjectMapper mapper;

    // ======================= KHAI BÁO HÀM CHO GEMINI =======================
    public ArrayNode declarations() {
        ArrayNode a = mapper.createArrayNode();
        a.add(fn("search_menu",
                "Tìm món trong thực đơn thật của nhà hàng (chỉ món đang bán). Dùng mỗi khi khách hỏi về món, giá, món chay/cay, món theo nước, hoặc cần tránh dị ứng.",
                props(
                        str("keyword", "Tên món hoặc từ khoá, vd 'pho', 'kimchi', 'sushi'"),
                        str("cuisine", "Ẩm thực: vn, thai, korean, japanese, chinese, western, indian"),
                        strArr("tags", "Thẻ món phải có TẤT CẢ: vegetarian, spicy, seafood, shareable, kids, celebration, beef, pork, chicken, fish, duck, noodle, soup, grill, dimsum, coffee"),
                        strArr("exclude_allergens", "Loại món chứa BẤT KỲ chất này: shrimp, shellfish, peanut, nuts, egg, milk, gluten, soy, sesame, fish"),
                        num("max_price", "Giá tối đa (VND) cho một món"),
                        str("category_type", "APPETIZER, FOOD, DRINK hoặc DESSERT"),
                        bool("signature_only", "Chỉ món đặc trưng của quán"),
                        num("limit", "Số món tối đa (mặc định 8, tối đa 12)")),
                List.of()));
        a.add(fn("suggest_for_group",
                "Gợi ý món cho nhóm: tính số phần cần gọi và tổng tiền theo số người (dựa trên số người ăn của mỗi món).",
                props(
                        num("guests", "Số người ăn"),
                        str("cuisine", "Ẩm thực: vn, thai, korean, japanese, chinese, western, indian"),
                        strArr("exclude_allergens", "Chất gây dị ứng cần tránh")),
                List.of("guests")));
        a.add(fn("get_restaurant_info",
                "Lấy thông tin nhà hàng: giờ mở cửa, địa chỉ, hotline, chính sách giữ bàn/đặt bàn, ngày nghỉ sắp tới.",
                props(str("topic", "Lọc theo chủ đề FAQ: info, hours, policy (bỏ trống = tất cả)")),
                List.of()));
        a.add(fn("check_availability",
                "Kiểm tra còn bàn không vào một ngày/giờ cho số khách, và gợi ý các khung giờ còn chỗ trong ngày.",
                props(
                        str("date", "Ngày dạng YYYY-MM-DD"),
                        str("time", "Giờ dạng HH:mm (24h)"),
                        num("guests", "Số khách")),
                List.of("date", "time", "guests")));
        a.add(fn("create_reservation",
                "Tạo yêu cầu đặt bàn. CHỈ gọi sau khi khách đã cung cấp đủ họ tên, số điện thoại, ngày, giờ, số khách VÀ đã xác nhận rõ ràng lại thông tin. Đặt bàn ở trạng thái chờ nhân viên xác nhận.",
                props(
                        str("customer_name", "Họ tên khách"),
                        str("phone", "Số điện thoại"),
                        str("date", "Ngày YYYY-MM-DD"),
                        str("time", "Giờ HH:mm"),
                        num("guests", "Số khách"),
                        str("event_type", "BIRTHDAY, ANNIVERSARY, FAMILY, BUSINESS, PARTY hoặc OTHER (không bắt buộc)"),
                        str("special_request", "Yêu cầu đặc biệt (không bắt buộc)")),
                List.of("customer_name", "phone", "date", "time", "guests")));
        a.add(fn("lookup_reservation",
                "Tra cứu trạng thái đặt bàn bằng mã đặt bàn và số điện thoại đã dùng khi đặt.",
                props(str("code", "Mã đặt bàn"), str("phone", "Số điện thoại")),
                List.of("code", "phone")));
        return a;
    }

    // ======================= THỰC THI =======================
    public Object execute(String name, JsonNode args, Long userId) {
        try {
            return switch (name) {
                case "search_menu" -> searchMenu(args);
                case "suggest_for_group" -> suggestForGroup(args);
                case "get_restaurant_info" -> restaurantInfo(args);
                case "check_availability" -> checkAvailability(args);
                case "create_reservation" -> createReservation(args, userId);
                case "lookup_reservation" -> lookup(args);
                default -> Map.of("error", "Hàm không tồn tại: " + name);
            };
        } catch (ApiException e) {
            return Map.of("error", e.getMessage());
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            return Map.of("error", "Tham số không hợp lệ: " + e.getMessage());
        }
    }

    private Object searchMenu(JsonNode a) {
        List<String> tags = new ArrayList<>(strings(a, "tags"));
        String cuisine = text(a, "cuisine");
        if (cuisine != null) tags.add(cuisine.toLowerCase());
        CategoryType type = text(a, "category_type") == null ? null : CategoryType.valueOf(text(a, "category_type").toUpperCase());
        int limit = Math.max(1, Math.min(intOr(a, "limit", 8), MAX_ITEMS));
        MenuSearchCriteria c = new MenuSearchCriteria(null, type, TextUtil.normalize(text(a, "keyword")), tags,
                strings(a, "exclude_allergens"), decimal(a, "max_price"), a.path("signature_only").asBoolean(false),
                List.of(MenuStatus.AVAILABLE));
        List<MenuItemDto> found = menu.search(c);
        return Map.of("count", found.size(), "items", found.stream().limit(limit).map(this::compact).toList(),
                "note", found.size() > limit ? "Còn nhiều món khác, hãy hỏi khách muốn lọc thêm" : "");
    }

    private Object suggestForGroup(JsonNode a) {
        int guests = Math.max(1, Math.min(intOr(a, "guests", 2), 100));
        List<String> tags = new ArrayList<>();
        if (text(a, "cuisine") != null) tags.add(text(a, "cuisine").toLowerCase());
        MenuSearchCriteria c = new MenuSearchCriteria(null, CategoryType.FOOD, null, tags, strings(a, "exclude_allergens"),
                null, null, List.of(MenuStatus.AVAILABLE));
        List<Map<String, Object>> out = menu.search(c).stream().filter(m -> m.serves() >= 2).map(m -> {
            int portions = (int) Math.ceil((double) guests / m.serves());
            Map<String, Object> row = new LinkedHashMap<>(compact(m));
            row.put("portions_needed", portions);
            row.put("total_price", m.price().multiply(BigDecimal.valueOf(portions)));
            return row;
        }).sorted(Comparator.comparing(r -> (BigDecimal) r.get("total_price"))).limit(8).toList();
        return Map.of("guests", guests, "suggestions", out);
    }

    private Object restaurantInfo(JsonNode a) {
        RestaurantSetting s = settings.get();
        String topic = text(a, "topic");
        List<Map<String, String>> faqs = settings.faqs(s).stream().filter(FaqDto::active)
                .filter(f -> topic == null || topic.equalsIgnoreCase(f.topic()))
                .map(f -> Map.of("question", f.question(), "answer", f.answer())).toList();
        return Map.of("open_time", s.getOpenTime().toString(), "close_time", s.getCloseTime().toString(),
                "reservation_minutes", s.getDurationMinutes(), "hold_minutes", s.getHoldMinutes(),
                "min_advance_minutes", s.getMinAdvanceMinutes(), "max_advance_days", s.getMaxAdvanceDays(),
                "closed_dates", settings.closedDates(s).stream().filter(c -> !c.date().isBefore(LocalDate.now())).limit(10)
                        .map(c -> c.date() + (c.reason() == null ? "" : " (" + c.reason() + ")")).toList(),
                "faqs", faqs);
    }

    private Object checkAvailability(JsonNode a) {
        LocalDate date = LocalDate.parse(required(a, "date"));
        LocalTime time = LocalTime.parse(required(a, "time"));
        int guests = intOr(a, "guests", 0);
        AvailabilityResponse r = tableService.availability(date, time, guests);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("available", !r.tables().isEmpty());
        out.put("tables_free_at_requested_time", r.tables().size());
        out.put("alternative_slots", r.availableSlots().stream().limit(8).map(LocalTime::toString).toList());
        if (r.closedReason() != null) out.put("reason_unavailable", r.closedReason());
        return out;
    }

    private Object createReservation(JsonNode a, Long userId) {
        EventType ev = text(a, "event_type") == null ? null : EventType.valueOf(text(a, "event_type").toUpperCase());
        ReservationRequest req = new ReservationRequest(required(a, "customer_name"), required(a, "phone").replaceAll("[\\s.-]", ""),
                null, LocalDate.parse(required(a, "date")), LocalTime.parse(required(a, "time")), intOr(a, "guests", 0), ev,
                text(a, "special_request"));
        if (!req.phone().matches("^\\+?\\d{9,15}$")) return Map.of("error", "Số điện thoại không hợp lệ");
        if (req.guestCount() < 1 || req.guestCount() > 100) return Map.of("error", "Số khách không hợp lệ");
        ReservationDto d = reservations.createForCustomer(req, ReservationSource.CHATBOT, userId);
        return Map.of("success", true, "code", d.code(), "table", d.tableName(), "date", d.reservationDate().toString(),
                "start_time", d.startTime().toString(), "end_time", d.endTime().toString(), "status", d.status().name(),
                "note", "Đang chờ nhân viên xác nhận; khách có thể tra cứu bằng mã và số điện thoại");
    }

    private Object lookup(JsonNode a) {
        ReservationDto d = reservations.lookup(required(a, "code"), required(a, "phone"));
        return Map.of("code", d.code(), "status", d.status().name(), "date", d.reservationDate().toString(),
                "start_time", d.startTime().toString(), "guests", d.guestCount(), "table", d.tableName());
    }

    // ======================= TIỆN ÍCH =======================
    private Map<String, Object> compact(MenuItemDto m) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("name", m.name());
        r.put("price_vnd", m.price());
        r.put("serves", m.serves());
        r.put("category", m.categoryName());
        r.put("tags", m.tags());
        r.put("allergens", m.allergens());
        if (m.sauceSuggestion() != null) r.put("sauce", m.sauceSuggestion());
        if (m.aiDescription() != null) r.put("highlight", m.aiDescription());
        else if (m.description() != null) r.put("highlight", m.description());
        if (m.signature()) r.put("signature", true);
        return r;
    }

    private static String text(JsonNode a, String k) {
        JsonNode n = a.path(k);
        return n.isMissingNode() || n.isNull() || n.asText().isBlank() ? null : n.asText().trim();
    }

    private static String required(JsonNode a, String k) {
        String v = text(a, k);
        if (v == null) throw new IllegalArgumentException("thiếu " + k);
        return v;
    }

    private static int intOr(JsonNode a, String k, int def) {
        JsonNode n = a.path(k);
        return n.isNumber() ? n.asInt() : (n.isTextual() && n.asText().matches("\\d+") ? Integer.parseInt(n.asText()) : def);
    }

    private static BigDecimal decimal(JsonNode a, String k) {
        JsonNode n = a.path(k);
        return n.isNumber() ? n.decimalValue() : null;
    }

    private static List<String> strings(JsonNode a, String k) {
        List<String> out = new ArrayList<>();
        JsonNode n = a.path(k);
        if (n.isArray()) n.forEach(x -> {
            if (x.isTextual() && !x.asText().isBlank()) out.add(x.asText().trim().toLowerCase());
        });
        return out;
    }

    // ---- dựng JSON schema khai báo hàm ----
    private record Prop(String name, ObjectNode schema) {}

    private Prop str(String n, String d) { return prop(n, "string", d); }
    private Prop num(String n, String d) { return prop(n, "number", d); }
    private Prop bool(String n, String d) { return prop(n, "boolean", d); }

    private Prop strArr(String n, String d) {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "array").put("description", d);
        s.putObject("items").put("type", "string");
        return new Prop(n, s);
    }

    private Prop prop(String n, String type, String d) {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", type).put("description", d);
        return new Prop(n, s);
    }

    private ObjectNode props(Prop... ps) {
        ObjectNode o = mapper.createObjectNode();
        for (Prop p : ps) o.set(p.name(), p.schema());
        return o;
    }

    private ObjectNode fn(String name, String description, ObjectNode properties, List<String> required) {
        ObjectNode f = mapper.createObjectNode();
        f.put("name", name).put("description", description);
        ObjectNode params = f.putObject("parameters");
        params.put("type", "object");
        params.set("properties", properties);
        if (!required.isEmpty()) {
            ArrayNode req = params.putArray("required");
            required.forEach(req::add);
        }
        return f;
    }
}
