package com.hmdrinks.Entity;

import com.hmdrinks.Enum.Status_Cart;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipper_detail")
public class ShipperDetail {

    @Id
    private Integer id;

    @OneToOne
    @JoinColumn(name = "userId")
    private User user;

    private Boolean onLeave;
    private String status;
    private LocalDateTime expectedReturnTime;
    private Integer totalOrdersToday;
    private String location;
    private Boolean isReset;
    private LocalDate dateReset;
}