package datn.nht.dinesmart.service;

import datn.nht.dinesmart.dto.ReservationDtos.*;
import datn.nht.dinesmart.entity.*;
import datn.nht.dinesmart.exception.ApiException;
import datn.nht.dinesmart.repository.DiningTableRepository;
import datn.nht.dinesmart.repository.ReservationEventRepository;
import datn.nht.dinesmart.repository.ReservationRepository;
import datn.nht.dinesmart.util.Crypto;
import datn.nht.dinesmart.util.TextUtil;
import datn.nht.dinesmart.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Vòng đời đặt bàn. Hai quy tắc của schema luôn được tôn trọng:
 *  1) Đổi reservation.status phải có reservation_event tương ứng -> ghi sự kiện (flush) TRƯỚC, đổi trạng thái SAU.
 *  2) Giữ chỗ: khoá dòng bàn (UPDLOCK) bằng một câu lệnh riêng, rồi mới kiểm tra trùng giờ.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservations;
    private final ReservationEventRepository events;
    private final DiningTableRepository tables;
    private final SettingService settings;
    private final BookingRules rules;
    private final AuditService audit;

    // =============================== TẠO ĐẶT BÀN ===============================

    /** Khách đặt bàn (web hoặc chatbot). userId = khách đang đăng nhập (nếu có). */
    @Transactional
    public ReservationDto createForCustomer(ReservationRequest r, ReservationSource source, Long userId) {
        Reservation res = book(r.customerName(), r.phone(), r.email(), r.reservationDate(), r.startTime(), r.guestCount(),
                null, r.eventType(), r.specialRequest(), source, userId, false);
        audit.log(userId, "RESERVATION_CREATED", "reservation", res.getId(), null,
                AuditService.kv("code", res.getCode(), "source", source.name()));
        return Mappers.reservation(res, null);
    }

    /** Thu ngân tạo tại quầy: có thể chỉ định bàn, khách vãng lai không cần SĐT. */
    @Transactional
    public ReservationDto createForStaff(StaffReservationRequest r, Long staffId) {
        String phone = TextUtil.blankToNull(r.phone());
        if (!r.walkIn() && phone == null) throw ApiException.badRequest("Cần số điện thoại khách (trừ khách vãng lai)");
        if (r.walkIn() && !r.reservationDate().equals(LocalDate.now())) {
            throw ApiException.badRequest("Khách vãng lai chỉ được nhận trong ngày hôm nay");
        }
        ReservationSource source = r.walkIn() ? ReservationSource.WALK_IN : ReservationSource.ONLINE;
        Reservation res = book(r.customerName(), phone, r.email(), r.reservationDate(), r.startTime(), r.guestCount(),
                r.tableId(), r.eventType(), r.specialRequest(), source, null, true);
        audit.log(staffId, "RESERVATION_CREATED", "reservation", res.getId(), null,
                AuditService.kv("code", res.getCode(), "source", source.name(), "by", "staff"));
        if (r.autoConfirm() || r.walkIn()) confirmInternal(res, staffId);
        if (r.walkIn()) checkInInternal(res, staffId);
        return Mappers.reservation(res, null);
    }

    private Reservation book(String name, String phone, String email, LocalDate date, LocalTime start, int guests,
                             Long tableId, EventType eventType, String special, ReservationSource source,
                             Long userId, boolean byStaff) {
        RestaurantSetting s = settings.get();
        LocalDateTime now = LocalDateTime.now();
        LocalTime end;
        if (byStaff) {
            end = rules.endOf(s, start);
            if (!end.isAfter(start)) throw ApiException.badRequest("Giờ bắt đầu quá muộn trong ngày");
        } else {
            rules.validate(s, date, start, now);
            end = rules.endOf(s, start);
        }
        LocalTime from = TimeUtil.minusClamped(start, s.getBufferMinutes());
        LocalTime to = TimeUtil.plusClamped(end, s.getBufferMinutes());

        DiningTable table = null;
        if (tableId != null) {
            table = tables.lockById(tableId).orElseThrow(() -> ApiException.notFound("Không tìm thấy bàn"));
            if (!table.isActive()) throw ApiException.badRequest("Bàn này hiện không nhận đặt");
            if (table.getCapacity() < guests) throw ApiException.badRequest("Bàn " + table.getName() + " chỉ có " + table.getCapacity() + " chỗ");
            if (reservations.existsOverlap(tableId, date, BookingRules.BUSY, from, to, 0L)) {
                throw ApiException.conflict("Bàn " + table.getName() + " đã có người đặt trong khung giờ này");
            }
        } else {
            for (DiningTable cand : tables.findFreeTables(date, guests, BookingRules.BUSY, from, to)) {
                // Khoá dòng bàn bằng câu lệnh riêng TRƯỚC khi kiểm tra lại trùng giờ (xem [P1] trong file SQL).
                DiningTable locked = tables.lockById(cand.getId()).orElse(null);
                if (locked == null || !locked.isActive()) continue;
                if (!reservations.existsOverlap(locked.getId(), date, BookingRules.BUSY, from, to, 0L)) {
                    table = locked;
                    break;
                }
            }
            if (table == null) {
                throw ApiException.conflict("Rất tiếc, không còn bàn phù hợp cho khung giờ này. Vui lòng chọn giờ khác.");
            }
        }

        Reservation res = new Reservation();
        res.setCode(newCode());
        res.setUserId(userId);
        res.setCustomerName(name.trim());
        res.setPhone(TextUtil.blankToNull(phone));
        res.setEmail(TextUtil.blankToNull(email));
        res.setTable(table);
        res.setReservationDate(date);
        res.setStartTime(start);
        res.setEndTime(end);
        res.setGuestCount(guests);
        res.setEventType(eventType);
        res.setSpecialRequest(TextUtil.blankToNull(special));
        res.setSource(source);
        res.setStatus(ReservationStatus.PENDING);
        return reservations.saveAndFlush(res);
    }

    private String newCode() {
        for (int i = 0; i < 8; i++) {
            String c = Crypto.reservationCode();
            if (!reservations.existsByCode(c)) return c;
        }
        throw new IllegalStateException("Không sinh được mã đặt bàn duy nhất");
    }

    // =============================== TRUY VẤN ===============================

    @Transactional(readOnly = true)
    public List<ReservationDto> listForStaff(LocalDate date, ReservationStatus status) {
        List<Reservation> rows;
        if (date != null && status != null) rows = reservations.findByReservationDateAndStatusOrderByStartTimeAsc(date, status);
        else if (date != null) rows = reservations.findByReservationDateOrderByStartTimeAsc(date);
        else if (status != null) rows = reservations.findByStatusOrderByReservationDateAscStartTimeAsc(status, PageRequest.of(0, 200));
        else rows = reservations.findByReservationDateOrderByStartTimeAsc(LocalDate.now());
        return rows.stream().map(r -> Mappers.reservation(r, null)).toList();
    }

    @Transactional(readOnly = true)
    public ReservationDto detail(Long id) {
        Reservation r = find(id);
        return Mappers.reservation(r, events.findByReservationIdOrderByCreatedAtAsc(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> mine(Long userId) {
        return reservations.findByUserIdOrderByReservationDateDescStartTimeDesc(userId).stream()
                .map(r -> Mappers.reservation(r, null)).toList();
    }

    /** Khách vãng lai tra cứu bằng mã + SĐT. Sai một trong hai đều trả cùng một lỗi 404. */
    @Transactional(readOnly = true)
    public ReservationDto lookup(String code, String phone) {
        return publicView(findByCodeAndPhone(code, phone));
    }

    private ReservationDto publicView(Reservation r) {
        ReservationDto d = Mappers.reservation(r, null);
        return new ReservationDto(d.id(), d.code(), null, d.customerName(), d.phone(), null, d.tableId(), d.tableName(),
                d.reservationDate(), d.startTime(), d.endTime(), d.guestCount(), d.eventType(), d.specialRequest(),
                d.status(), d.source(), d.createdAt(), null);
    }

    // =============================== NHÂN VIÊN ===============================

    @Transactional
    public ReservationDto confirm(Long id, Long staffId) {
        Reservation r = find(id);
        confirmInternal(r, staffId);
        return Mappers.reservation(r, null);
    }

    @Transactional
    public ReservationDto reject(Long id, String reason, Long staffId) {
        Reservation r = find(id);
        transition(r, Set.of(ReservationStatus.PENDING), ReservationStatus.CANCELLED, ReservationAction.REJECTED, staffId, reason);
        releaseTableIfIdle(r);
        return Mappers.reservation(r, null);
    }

    /** Nhân viên huỷ đặt bàn đã xác nhận (kèm lý do). */
    @Transactional
    public ReservationDto cancelByStaff(Long id, String reason, Long staffId) {
        Reservation r = find(id);
        transition(r, Set.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED), ReservationStatus.CANCELLED,
                ReservationAction.CANCELLED, staffId, reason);
        releaseTableIfIdle(r);
        return Mappers.reservation(r, null);
    }

    @Transactional
    public ReservationDto checkIn(Long id, Long staffId) {
        Reservation r = find(id);
        checkInInternal(r, staffId);
        return Mappers.reservation(r, null);
    }

    @Transactional
    public ReservationDto complete(Long id, Long staffId) {
        Reservation r = find(id);
        completeInternal(r, staffId);
        return Mappers.reservation(r, null);
    }

    @Transactional
    public ReservationDto noShow(Long id, Long staffId) {
        Reservation r = find(id);
        transition(r, Set.of(ReservationStatus.CONFIRMED), ReservationStatus.NO_SHOW, ReservationAction.NO_SHOW, staffId, "Khách không đến");
        releaseTableIfIdle(r);
        return Mappers.reservation(r, null);
    }

    // ---- dùng chung với OrderService / BillingService (cùng giao dịch) ----

    void confirmInternal(Reservation r, Long staffId) {
        transition(r, Set.of(ReservationStatus.PENDING), ReservationStatus.CONFIRMED, ReservationAction.CONFIRMED, staffId, null);
        DiningTable t = r.getTable();
        if (r.getReservationDate().equals(LocalDate.now()) && t.getStatus() == TableStatus.AVAILABLE) {
            t.setStatus(TableStatus.RESERVED);
            tables.save(t);
        }
    }

    void checkInInternal(Reservation r, Long staffId) {
        if (!r.getReservationDate().equals(LocalDate.now())) {
            throw ApiException.conflict("Chỉ check-in được vào đúng ngày đặt bàn");
        }
        transition(r, Set.of(ReservationStatus.CONFIRMED), ReservationStatus.CHECKED_IN, ReservationAction.CHECKED_IN, staffId, null);
        DiningTable t = r.getTable();
        t.setStatus(TableStatus.OCCUPIED);
        tables.save(t);
    }

    void completeInternal(Reservation r, Long staffId) {
        transition(r, Set.of(ReservationStatus.CHECKED_IN), ReservationStatus.COMPLETED, ReservationAction.COMPLETED, staffId, null);
    }

    public Reservation findEntity(Long id) {
        return find(id);
    }

    // =============================== KHÁCH TỰ HUỶ ===============================

    @Transactional
    public ReservationDto cancelByCustomer(Long userId, Long id) {
        Reservation r = find(id);
        if (r.getUserId() == null || !r.getUserId().equals(userId)) {
            throw ApiException.notFound("Không tìm thấy đặt bàn");
        }
        return customerCancel(r, userId);
    }

    @Transactional
    public ReservationDto cancelByGuest(String code, String phone) {
        return customerCancel(findByCodeAndPhone(code, phone), null);
    }

    private ReservationDto customerCancel(Reservation r, Long userId) {
        if (!LocalDateTime.of(r.getReservationDate(), r.getStartTime()).isAfter(LocalDateTime.now())) {
            throw ApiException.conflict("Đã quá giờ đặt bàn, vui lòng liên hệ trực tiếp với nhà hàng");
        }
        transition(r, Set.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED), ReservationStatus.CANCELLED,
                ReservationAction.CANCELLED, userId, "Khách báo huỷ");
        releaseTableIfIdle(r);
        return publicView(r);
    }

    // =============================== NỘI BỘ ===============================

    /** Ghi sự kiện (flush) TRƯỚC, đổi trạng thái SAU — trigger trg_reservation_status_has_event yêu cầu thứ tự này. */
    private void transition(Reservation r, Set<ReservationStatus> allowedFrom, ReservationStatus to,
                            ReservationAction action, Long userId, String reason) {
        if (!allowedFrom.contains(r.getStatus())) {
            throw ApiException.conflict("Không thể thực hiện với đặt bàn đang ở trạng thái " + r.getStatus());
        }
        ReservationStatus old = r.getStatus();
        events.saveAndFlush(new ReservationEvent(r.getId(), action, userId, reason));
        r.setStatus(to);
        reservations.saveAndFlush(r);
        audit.log(userId, "RESERVATION_" + to.name(), "reservation", r.getId(),
                AuditService.kv("status", old.name()), AuditService.kv("status", to.name(), "reason", reason));
    }

    /** Nhả bàn RESERVED nếu hôm nay không còn đặt chỗ nào khác chưa kết thúc (giống bước 3 của usp_reservation_housekeeping). */
    private void releaseTableIfIdle(Reservation r) {
        DiningTable t = r.getTable();
        if (t.getStatus() != TableStatus.RESERVED) return;
        boolean other = reservations.existsOngoing(t.getId(), LocalDate.now(),
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN), LocalTime.now(), r.getId());
        if (!other) {
            t.setStatus(TableStatus.AVAILABLE);
            tables.save(t);
        }
    }

    private Reservation find(Long id) {
        return reservations.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đặt bàn"));
    }

    private Reservation findByCodeAndPhone(String code, String phone) {
        return reservations.findByCode(code.trim().toUpperCase())
                .filter(r -> r.getPhone() != null && r.getPhone().equals(phone.trim()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đặt bàn với mã và số điện thoại này"));
    }
}
