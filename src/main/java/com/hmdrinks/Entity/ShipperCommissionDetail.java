package com.hmdrinks.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipper_commission_detail")
public class ShipperCommissionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(name = "commission_date", nullable = false)
    private LocalDate commissionDate;

    @Column(name = "daily_commission", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyCommission;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "bonus")
    private Double bonus;
}
