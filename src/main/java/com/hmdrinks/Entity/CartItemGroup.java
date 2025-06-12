package com.hmdrinks.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cart_item_group")
public class CartItemGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartItemId", nullable = false)
    private int cartItemId;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "proId", referencedColumnName = "proId"),
            @JoinColumn(name = "size", referencedColumnName = "size")
    })
    private ProductVariants productVariants;

    @ManyToOne
    @JoinColumn(name = "cartId", nullable = false)
    private CartGroup cartGroup;


    @Column(name = "itemPrice")
    private Double itemPrice;

    @Column(name = "totalPrice")
    private Double totalPrice;

    @Column(name = "note")
    private String note;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "is_disabled")
    private Boolean isDisabled;

    @Column(name = "date_deleted",columnDefinition = "DATETIME")
    private LocalDateTime dateDeleted;

    @Column(name = "date_created",columnDefinition = "DATETIME")
    private LocalDateTime dateCreated;

    @Column(name = "date_updated",columnDefinition = "DATETIME")
    private LocalDateTime dateUpdated;
}

