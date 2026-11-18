package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Bảng singleton (id luôn = 1). */
@Entity
@Table(name = "restaurant_setting")
@Getter @Setter
public class RestaurantSetting {
    @Id
    private Integer id = 1;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(name = "slot_minutes", nullable = false)
    private int slotMinutes = 30;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 90;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes = 15;

    @Column(name = "max_advance_days", nullable = false)
    private int maxAdvanceDays = 30;

    @Column(name = "min_advance_minutes", nullable = false)
    private int minAdvanceMinutes = 60;

    @Column(name = "hold_minutes", nullable = false)
    private int holdMinutes = 15;

    @Column(name = "pending_expire_minutes", nullable = false)
    private int pendingExpireMinutes = 30;

    @Column(name = "vat_percent", nullable = false, precision = 4, scale = 2)
    private BigDecimal vatPercent = new BigDecimal("8.00");

    @Column(name = "chat_max_messages", nullable = false)
    private int chatMaxMessages = 20;

    /** JSON: [{"topic","question","answer","active"}] */
    @Column(nullable = false)
    private String faqs = "[]";

    /** JSON: [{"date":"2026-12-25","reason":"Noel"}] */
    @Column(name = "closed_dates", nullable = false)
    private String closedDates = "[]";
}
