package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.SettingDtos.*;
import datn.nht.dinesmart.entity.RestaurantSetting;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.SettingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SettingService {
    private final SettingRepository repo;
    private final ObjectMapper mapper;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public RestaurantSetting get() {
        return repo.findById(1).orElseThrow(() -> new IllegalStateException("Thiếu dòng restaurant_setting (id = 1)"));
    }

    public List<FaqDto> faqs(RestaurantSetting s) {
        return parse(s.getFaqs(), new TypeReference<List<FaqDto>>() {});
    }

    public List<ClosedDateDto> closedDates(RestaurantSetting s) {
        return parse(s.getClosedDates(), new TypeReference<List<ClosedDateDto>>() {});
    }

    /** Lý do nghỉ nếu ngày đó là ngày nghỉ, ngược lại rỗng. */
    public Optional<String> closedReason(RestaurantSetting s, LocalDate date) {
        return closedDates(s).stream().filter(c -> date.equals(c.date()))
                .map(c -> c.reason() == null || c.reason().isBlank() ? "Nhà hàng nghỉ" : c.reason()).findFirst();
    }

    @Transactional(readOnly = true)
    public PublicSettingDto publicView() {
        RestaurantSetting s = get();
        List<FaqDto> activeFaqs = faqs(s).stream().filter(FaqDto::active).toList();
        return new PublicSettingDto(s.getOpenTime(), s.getCloseTime(), s.getSlotMinutes(), s.getDurationMinutes(),
                s.getBufferMinutes(), s.getMaxAdvanceDays(), s.getMinAdvanceMinutes(), s.getHoldMinutes(),
                s.getPendingExpireMinutes(), activeFaqs,
                closedDates(s).stream().filter(c -> !c.date().isBefore(LocalDate.now())).toList());
    }

    @Transactional(readOnly = true)
    public SettingDto adminView() {
        return toDto(get());
    }

    @Transactional
    public SettingDto update(SettingRequest r, Long actorId) {
        if (!r.closeTime().isAfter(r.openTime())) {
            throw ApiException.badRequest("Giờ đóng cửa phải sau giờ mở cửa");
        }
        RestaurantSetting s = get();
        SettingDto before = toDto(s);
        s.setOpenTime(r.openTime());
        s.setCloseTime(r.closeTime());
        s.setSlotMinutes(r.slotMinutes());
        s.setDurationMinutes(r.durationMinutes());
        s.setBufferMinutes(r.bufferMinutes());
        s.setMaxAdvanceDays(r.maxAdvanceDays());
        s.setMinAdvanceMinutes(r.minAdvanceMinutes());
        s.setHoldMinutes(r.holdMinutes());
        s.setPendingExpireMinutes(r.pendingExpireMinutes());
        s.setVatPercent(r.vatPercent());
        s.setChatMaxMessages(r.chatMaxMessages());
        s.setFaqs(write(r.faqs() == null ? List.of() : r.faqs()));
        s.setClosedDates(write(r.closedDates() == null ? List.of() : r.closedDates()));
        repo.save(s);
        SettingDto after = toDto(s);
        audit.log(actorId, "SETTING_UPDATED", "restaurant_setting", 1L, before, after);
        return after;
    }

    private SettingDto toDto(RestaurantSetting s) {
        return new SettingDto(s.getOpenTime(), s.getCloseTime(), s.getSlotMinutes(), s.getDurationMinutes(),
                s.getBufferMinutes(), s.getMaxAdvanceDays(), s.getMinAdvanceMinutes(), s.getHoldMinutes(),
                s.getPendingExpireMinutes(), s.getVatPercent(), s.getChatMaxMessages(), faqs(s), closedDates(s));
    }

    private <T> List<T> parse(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String write(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw ApiException.badRequest("Không ghi được dữ liệu JSON");
        }
    }
}
