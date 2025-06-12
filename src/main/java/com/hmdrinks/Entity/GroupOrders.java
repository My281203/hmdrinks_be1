package com.hmdrinks.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hmdrinks.Enum.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "group_orders")
public class GroupOrders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "groupOrderId")
    private int groupOrderId;

    @Column(name = "orderDate", nullable = false,columnDefinition = "DATETIME")
    private LocalDateTime orderDate;

    // Cho phép thêm thành viên bất kỳ lúc nào hoặc phải theo hạn chót
    @Column(name = "is_flexible_payment")
    private Boolean isFlexiblePayment;


    @Column(name = "deadline_payment ", columnDefinition = "DATETIME")
    private LocalDateTime deadlinePayment;
    @Lob
    @Column(name = "address",columnDefinition = "TEXT")
    private String address;

    @Lob
    @Column(name = "note",columnDefinition = "TEXT")
    private String note;

    @Column(name = "link",unique = true)
    private String link;

    @Column(name = "code",unique = true,length = 10)
    private String code;

    @Column(name = "nameGroup")
    private String nameGroup;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "totalPrice", nullable = false)
    private double totalPrice;

    @Column(name = "totalQuantity", nullable = false)
    private double totalQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_bill", nullable = false)
    private TypeGroupOrder typeGroupOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusGroupOrder status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_payment", nullable = false)
    private TypePayment typePayment;


    @Enumerated(EnumType.STRING)
    @Column(name = "type_time", nullable = true)
    private Status_Type_Time_Group typeTime;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "date_deleted",columnDefinition = "DATETIME")
    private LocalDateTime dateDeleted;

    @Column(name = "date_checkout",columnDefinition = "DATETIME")
    private LocalDateTime dateCheckout;

    @Column(name = "date_created",columnDefinition = "DATETIME")
    private LocalDateTime dateCreated;

    @Column(name = "date_updated",columnDefinition = "DATETIME")
    private LocalDateTime dateUpdated;

    @Column(name = "date_payment_time")
    private LocalTime datePaymentTime;


    @OneToMany(mappedBy = "groupOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GroupOrderMember> groupOrderMembers;

    @OneToMany(mappedBy = "groupOrder", cascade = CascadeType.ALL,  orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentGroup> payment;

}
