package com.hmdrinks.Entity;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Type_Post;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "post_translation")
public class PostTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "postTransId",columnDefinition = "BIGINT")
    private Integer postTransId;

    @Column(name = "title")
    private String title;

    @Lob
    @Column(name = "description",columnDefinition = "TEXT")
    private  String description;

    @Column(name = "date_create",nullable = false)
    private LocalDateTime dateCreate;

    @Column(name = "shortDes", nullable = false)
    private String shortDes;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "date_deleted",columnDefinition = "DATETIME")
    private LocalDateTime dateDeleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_code")
    private Language language;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
}
