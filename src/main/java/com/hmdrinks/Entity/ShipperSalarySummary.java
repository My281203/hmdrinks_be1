package com.hmdrinks.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipper_salary_summary")
public class ShipperSalarySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(name = "month", nullable = false)
    private int month;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "commission", nullable = false, precision = 12, scale = 2)
    private BigDecimal commission;

    @Column(name = "total_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSalary;

    @Column(name = "total_orders", nullable = false)
    private int totalOrders;

    @Column(name = "working_days", nullable = false)
    private int workingDays;

    @Column(name = "approved_leave_days", nullable = false)
    private int approvedLeaveDays;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "date_deleted",columnDefinition = "DATETIME")
    private LocalDateTime dateDeleted;
}
