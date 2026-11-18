package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.ReservationDtos.*;
import datn.nht.dinesmart.entity.DiningTable;
import datn.nht.dinesmart.entity.Reservation;
import datn.nht.dinesmart.entity.RestaurantSetting;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.DiningTableRepository;
import datn.nht.dinesmart.repository.ReservationRepository;
import datn.nht.dinesmart.util.TextUtil;
import datn.nht.dinesmart.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TableService {
    private final DiningTableRepository tables;
    private final ReservationRepository reservations;
    private final SettingService settings;
    private final BookingRules rules;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<TableDto> list() {
        return tables.findAllByOrderByNameAsc().stream().map(Mappers::table).toList();
    }

    @Transactional
    public TableDto create(TableRequest r, Long actorId) {
        if (tables.existsByNameIgnoreCase(r.name().trim())) throw ApiException.conflict("Tên bàn đã tồn tại");
        DiningTable t = new DiningTable();
        apply(t, r);
        tables.save(t);
        audit.log(actorId, "TABLE_CREATED", "dining_table", t.getId(), null, AuditService.kv("name", t.getName()));
        return Mappers.table(t);
    }

    @Transactional
    public TableDto update(Long id, TableRequest r, Long actorId) {
        DiningTable t = tables.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy bàn"));
        if (!t.getName().equalsIgnoreCase(r.name().trim()) && tables.existsByNameIgnoreCase(r.name().trim())) {
            throw ApiException.conflict("Tên bàn đã tồn tại");
        }
        apply(t, r);
        tables.save(t);
        audit.log(actorId, "TABLE_UPDATED", "dining_table", id, null, AuditService.kv("name", t.getName(), "active", t.isActive()));
        return Mappers.table(t);
    }

    @Transactional
    public TableDto setStatus(Long id, datn.nht.dinesmart.entity.TableStatus status, Long actorId) {
        DiningTable t = tables.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy bàn"));
        String old = t.getStatus().name();
        t.setStatus(status);
        tables.save(t);
        audit.log(actorId, "TABLE_STATUS", "dining_table", id, AuditService.kv("status", old), AuditService.kv("status", status.name()));
        return Mappers.table(t);
    }

    private void apply(DiningTable t, TableRequest r) {
        t.setName(r.name().trim());
        t.setCapacity(r.capacity());
        t.setArea(TextUtil.blankToNull(r.area()));
        t.setPosX(r.posX());
        t.setPosY(r.posY());
        t.setActive(r.active());
    }

    /**
     * Bàn trống tại một giờ + các khung giờ còn bàn trong ngày (best-fit: sức chứa nhỏ nhất đủ chỗ trước).
     * Tính trong bộ nhớ từ MỘT truy vấn đặt bàn của ngày; việc giữ chỗ thật sự vẫn kiểm tra lại dưới khoá dòng bàn.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse availability(LocalDate date, LocalTime time, int guests) {
        if (guests < 1 || guests > 100) throw ApiException.badRequest("Số khách không hợp lệ");
        RestaurantSetting s = settings.get();
        Optional<String> closed = settings.closedReason(s, date);
        if (closed.isPresent()) {
            return new AvailabilityResponse(date, guests, time, List.of(), List.of(), closed.get());
        }
        List<DiningTable> candidates = tables.findAllByOrderByNameAsc().stream()
                .filter(t -> t.isActive() && t.getCapacity() >= guests)
                .sorted(Comparator.comparingInt(DiningTable::getCapacity).thenComparing(DiningTable::getId))
                .toList();
        List<Reservation> busy = reservations.findByReservationDateOrderByStartTimeAsc(date).stream()
                .filter(r -> BookingRules.BUSY.contains(r.getStatus())).toList();

        LocalDateTime now = LocalDateTime.now();
        List<TableDto> freeAtTime = List.of();
        String reason = null;
        if (time != null) {
            Optional<String> v = rules.violation(s, date, time, now);
            if (v.isPresent()) reason = v.get();
            else freeAtTime = free(candidates, busy, s, time).stream().map(Mappers::table).toList();
        }

        List<LocalTime> slots = new ArrayList<>();
        for (LocalTime t = s.getOpenTime(); !t.plusMinutes(s.getDurationMinutes()).isAfter(s.getCloseTime());
             t = t.plusMinutes(s.getSlotMinutes())) {
            if (rules.violation(s, date, t, now).isPresent()) continue;
            if (!free(candidates, busy, s, t).isEmpty()) slots.add(t);
            if (t.plusMinutes(s.getSlotMinutes()).isBefore(t)) break;     // phòng quay vòng nửa đêm
        }
        return new AvailabilityResponse(date, guests, time, freeAtTime, slots, reason);
    }

    private List<DiningTable> free(List<DiningTable> candidates, List<Reservation> busy, RestaurantSetting s, LocalTime start) {
        LocalTime end = rules.endOf(s, start);
        LocalTime from = TimeUtil.minusClamped(start, s.getBufferMinutes());
        LocalTime to = TimeUtil.plusClamped(end, s.getBufferMinutes());
        return candidates.stream().filter(t -> busy.stream().noneMatch(r ->
                r.getTable().getId().equals(t.getId()) && r.getStartTime().isBefore(to) && r.getEndTime().isAfter(from))).toList();
    }
}
