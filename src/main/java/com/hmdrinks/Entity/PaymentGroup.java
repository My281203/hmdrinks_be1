package com.hmdrinks.Entity;

import com.hmdrinks.Enum.Payment_Method;
import com.hmdrinks.Enum.Status_Payment;
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
@Table(name = "payments_group")
public class PaymentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private int paymentId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "order_id_payment")
    private String orderIdPayment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status_Payment status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private Payment_Method paymentMethod;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "is_refunded")
    private Boolean isRefund;

    @Column(name = "date_deleted")
    private LocalDateTime dateDeleted;

    @Column(name = "date_refunded", columnDefinition = "DATETIME")
    private LocalDateTime dateRefunded;

    @Column(name = "discount_percent")
    private Double discountPercent;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "link", columnDefinition = "TEXT")
    private String link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_order_id", nullable = false,unique = false)
    private GroupOrders groupOrder;

    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ShippmentGroup shipment;

}

