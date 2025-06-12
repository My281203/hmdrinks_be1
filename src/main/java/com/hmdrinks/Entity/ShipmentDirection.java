package com.hmdrinks.Entity;

import com.hmdrinks.Repository.ShipmentGroupRepository;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "map_directions")
public class ShipmentDirection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "map_direction_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id",nullable = true) // 🔥 Sửa tên cột cho hợp lý
    private Shippment shipment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_group_id") // 🔥 Sửa tên cột cho hợp lý
    private ShippmentGroup shipmentGroup;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "deleted_at", columnDefinition = "DATETIME")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @Column(name = "overview_polyline", columnDefinition = "TEXT")
    private String overviewPolyline;

    @Column(name = "latitudeStart")
    private double latitudeStart;

    @Column(name = "longitudeStart")
    private double longitudeStart;

    @Column(name = "latitudeEnd")
    private double latitudeEnd;

    @Column(name = "longitudeEnd")
    private double longitudeEnd;

    @OneToMany(mappedBy = "direction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StepDetail> steps;
}
