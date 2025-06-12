package com.hmdrinks.Entity;

import com.hmdrinks.Enum.StatusGroupOrder;
import com.hmdrinks.Enum.StatusGroupOrderMember;
import com.hmdrinks.Enum.TypePayment;
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
@Table(name = "group_order_members")
public class GroupOrderMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memberId")
    private int memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupOrderId", nullable = false)
    private GroupOrders groupOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(name = "amount", nullable = true)
    private Double amount;

    @Column(name = "quantity", nullable = true)
    private Integer quantity;// Số tiền thành viên phải thanh toán

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(name = "is_leader", nullable = false)
    private Boolean isLeader; // Trưởng nhóm hay không
// Đã thanh toán hay chưa

    @Column(name = "is_blacklist",nullable = true)
    private Boolean isBlacklist;

    @Lob
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "date_created", columnDefinition = "DATETIME")
    private LocalDateTime dateCreated;

    @Column(name = "date_updated", columnDefinition = "DATETIME")
    private LocalDateTime dateUpdated;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "is_deleted_leader")
    private Boolean isDeletedLeader;

    @Column(name = "date_deleted",columnDefinition = "DATETIME")
    private LocalDateTime dateDeleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusGroupOrderMember status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_payment", nullable = false)
    private TypePayment typePayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartId", nullable = true)
    private CartGroup cartGroup;



}
