package com.hmdrinks.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import scala.Int;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "categories") // Elasticsearch index name
public class CategoryElasticsearch {

    @Id
    private String id; // Elasticsearch thường sử dụng String làm ID

    @Field(type = FieldType.Text, analyzer = "standard")
    private String cateName;

    @Field(type = FieldType.Keyword)
    private Integer categoryId;

    @Field(type = FieldType.Keyword)
    private String cateImg; // Ảnh có thể là URL

    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateDeleted;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateUpdated;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateCreated;

    @Field(type = FieldType.Nested)
    private List<CategoryTranslationElasticsearch> categoryTranslations;

    public CategoryElasticsearch(String id, String cateName, String cateImg,
                                 Boolean isDeleted, LocalDateTime dateCreated,
                                 LocalDateTime dateUpdated, LocalDateTime dateDeleted, Integer categoryId) {
        this.id = id;
        this.cateName = cateName;
        this.categoryId = categoryId;
        this.cateImg = cateImg;
        this.isDeleted = isDeleted;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.dateDeleted = dateDeleted;
    }
}
