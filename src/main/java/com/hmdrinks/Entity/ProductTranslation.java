package com.hmdrinks.Entity;

import com.hmdrinks.Enum.Language;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_translation")
public class ProductTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proTransId")
    private int proTransId;

    @Column(name = "proName", nullable = false)
    private String proName;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "date_deleted",columnDefinition = "DATETIME")
    private LocalDateTime dateDeleted;

    @Column(name = "date_updated",columnDefinition = "DATETIME")
    private LocalDateTime dateUpdated;

    @Column(name = "date_created",columnDefinition = "DATETIME")
    private LocalDateTime dateCreated;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_code")
    private Language language;

    @ManyToOne
    @JoinColumn(name = "pro_id")
    private Product product;

}
