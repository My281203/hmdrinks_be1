package com.hmdrinks.Entity;

import com.hmdrinks.Enum.AttendanceStatus;
import com.hmdrinks.Enum.Status_Shipment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipper_attendance")
public class ShipperAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;


    @Column(name = "attendance_date", nullable = false,columnDefinition = "DATE")
    private LocalDate attendanceDate;


    @Column(name = "is_present", nullable = false)
    private Boolean isPresent;

    // Thời gian check-in và check-out (nếu có)
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;


    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status;


    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
