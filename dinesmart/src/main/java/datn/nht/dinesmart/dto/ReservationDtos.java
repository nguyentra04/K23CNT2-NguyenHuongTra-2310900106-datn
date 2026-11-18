package datn.nht.dinesmart.dto;

import datn.nht.dinesmart.entity.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class ReservationDtos {
    private ReservationDtos() {}

    public record TableDto(Long id, String name, int capacity, String area, Integer posX, Integer posY,
                           boolean active, TableStatus status) {}

    public record TableRequest(
            @NotBlank @Size(max = 20) String name,
            @Min(1) @Max(100) int capacity,
            @Size(max = 50) String area,
            Integer posX, Integer posY,
            boolean active) {}

    public record TableStatusRequest(@NotNull TableStatus status) {}

    public record AvailabilityResponse(LocalDate date, int guests, LocalTime time,
                                       List<TableDto> tables, List<LocalTime> availableSlots, String closedReason) {}

    /** Khách đặt bàn (công khai). Hệ thống tự chọn bàn best-fit. */
    public record ReservationRequest(
            @NotBlank @Size(max = 100) String customerName,
            @NotBlank @Pattern(regexp = AuthDtos.PHONE_RULE, message = "Số điện thoại không hợp lệ") String phone,
            @Email @Size(max = 100) String email,
            @NotNull LocalDate reservationDate,
            @NotNull LocalTime startTime,
            @Min(1) @Max(100) int guestCount,
            EventType eventType,
            @Size(max = 500) String specialRequest) {}

    /** Thu ngân tạo đặt bàn tại quầy: có thể chỉ định bàn, khách vãng lai không cần SĐT, tự xác nhận. */
    public record StaffReservationRequest(
            @NotBlank @Size(max = 100) String customerName,
            @Pattern(regexp = "^$|" + AuthDtos.PHONE_RULE, message = "Số điện thoại không hợp lệ") String phone,
            @Email @Size(max = 100) String email,
            @NotNull LocalDate reservationDate,
            @NotNull LocalTime startTime,
            @Min(1) @Max(100) int guestCount,
            Long tableId,
            EventType eventType,
            @Size(max = 500) String specialRequest,
            boolean walkIn,
            boolean autoConfirm) {}

    public record GuestCancelRequest(@NotBlank String code,
                                     @NotBlank @Pattern(regexp = AuthDtos.PHONE_RULE) String phone) {}

    public record ReasonRequest(@NotBlank @Size(max = 255) String reason) {}

    public record ReservationEventDto(ReservationAction action, Long userId, String reason, LocalDateTime createdAt) {}

    public record ReservationDto(
            Long id, String code, Long userId, String customerName, String phone, String email,
            Long tableId, String tableName, LocalDate reservationDate, LocalTime startTime, LocalTime endTime,
            int guestCount, EventType eventType, String specialRequest,
            ReservationStatus status, ReservationSource source, LocalDateTime createdAt,
            List<ReservationEventDto> events) {}
}
