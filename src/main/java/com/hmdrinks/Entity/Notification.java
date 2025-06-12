package com.hmdrinks.Entity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notifiId")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "userId")
    @JsonIgnore
    private User user;

    @Column(name = "shipmentId", nullable = true)
    private Integer shipmentId;

    @Column(name = "type")
    private String type;

    @Column(name = "groupOrderId", nullable = true)
    private Integer groupOrderId;

    @Column(name = "message",nullable = true)
    private String message;

    @Column(name = "time",nullable = false)
    private LocalDateTime time;

    @Column(name = "isRead",nullable = false)
    private Boolean isRead = false;
}
