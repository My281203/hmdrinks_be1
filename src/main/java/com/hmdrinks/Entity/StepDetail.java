package com.hmdrinks.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "step_details")
public class StepDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_id") // 🔥 Thêm tên cột cho rõ ràng
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_direction_id") // 🔥 Đảm bảo trùng với `mappedBy` bên ShipmentDirection
    private ShipmentDirection direction;

    @Column(name = "instruction", columnDefinition = "TEXT")
    private String instruction;

    @Column(name = "latitude")
    private double latitude;

    @Column(name = "longitude")
    private double longitude;

    @Column(name = "distance_text")
    private String distanceText;

    @Column(name = "duration_text")
    private String durationText;
}
