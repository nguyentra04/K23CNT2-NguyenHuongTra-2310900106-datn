package datn.nht.dinesmart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dining_table")
@Getter @Setter
public class DiningTable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column(length = 50)
    private String area;

    @Column(name = "pos_x")
    private Integer posX;

    @Column(name = "pos_y")
    private Integer posY;

    /** Bàn có được phép nhận đặt không (lâu dài). */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Trạng thái vận hành tức thời. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TableStatus status = TableStatus.AVAILABLE;

    @Version
    @Column(nullable = false)
    private int version;
}
