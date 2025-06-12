// Shipment.java
package com.hmdrinks.Entity;

import com.hmdrinks.Enum.Status_Shipment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipment_group")
public class ShippmentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private int shipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "date_shipped", columnDefinition = "DATETIME")
    private LocalDateTime dateShip;

    @Column(name = "date_delivered", columnDefinition = "DATETIME")
    private LocalDateTime dateDelivered;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status_Shipment status;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "distance")
    private Double distance;

    @Column(name = "note")
    private String note;

    @Column(name = "date_deleted", columnDefinition = "DATETIME")
    private LocalDateTime dateDeleted;

    @Column(name = "date_created", columnDefinition = "DATETIME")
    private LocalDateTime dateCreated;

    @Column(name = "date_canceled", columnDefinition = "DATETIME")
    private LocalDateTime dateCancel;

    // Quan hệ 1-1 với PaymentGroup
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private PaymentGroup payment;

    @OneToOne(mappedBy = "shipmentGroup", cascade = CascadeType.ALL)
    private ShipmentDirection shipmentDirection;
}

