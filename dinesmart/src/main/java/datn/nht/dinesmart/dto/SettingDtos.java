package datn.nht.dinesmart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class SettingDtos {
    private SettingDtos() {}

    public record FaqDto(@Size(max = 50) String topic, @NotBlank @Size(max = 255) String question,
                         @NotBlank @Size(max = 2000) String answer, boolean active) {}

    public record ClosedDateDto(@NotNull LocalDate date, @Size(max = 200) String reason) {}

    /** Cho khách: giờ mở cửa, quy tắc đặt bàn, FAQ đang bật, ngày nghỉ. */
    public record PublicSettingDto(LocalTime openTime, LocalTime closeTime, int slotMinutes, int durationMinutes,
                                   int bufferMinutes, int maxAdvanceDays, int minAdvanceMinutes,
                                   int holdMinutes, int pendingExpireMinutes,
                                   List<FaqDto> faqs, List<ClosedDateDto> closedDates) {}

    public record SettingDto(LocalTime openTime, LocalTime closeTime, int slotMinutes, int durationMinutes,
                             int bufferMinutes, int maxAdvanceDays, int minAdvanceMinutes, int holdMinutes,
                             int pendingExpireMinutes, BigDecimal vatPercent, int chatMaxMessages,
                             List<FaqDto> faqs, List<ClosedDateDto> closedDates) {}

    public record SettingRequest(
            @NotNull LocalTime openTime, @NotNull LocalTime closeTime,
            @Min(5) @Max(240) int slotMinutes, @Min(15) @Max(480) int durationMinutes,
            @Min(0) @Max(120) int bufferMinutes, @Min(1) @Max(365) int maxAdvanceDays,
            @Min(0) @Max(10080) int minAdvanceMinutes, @Min(0) @Max(240) int holdMinutes,
            @Min(1) @Max(10080) int pendingExpireMinutes,
            @NotNull @DecimalMin("0") @DecimalMax("99.99") BigDecimal vatPercent,
            @Min(1) @Max(200) int chatMaxMessages,
            @Valid @Size(max = 100) List<FaqDto> faqs,
            @Valid @Size(max = 366) List<ClosedDateDto> closedDates) {}
}
