package com.hmdrinks.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(indexName = "products") // Tên index trên Elasticsearch
public class ProductElasticsearch {

    @Id
    private String id; // ID của Elasticsearch thường là String

    @Field(type = FieldType.Text, analyzer = "standard")
    private String proName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String listProImg; // Ảnh có thể lưu dưới dạng URL hoặc JSON array

    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateDeleted;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateUpdated;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateCreated;

    @Field(type = FieldType.Integer)
    private Integer categoryId;

    @Field(type = FieldType.Nested)
    private List<ProductVariantsElasticsearch> productVariants;

    @Field(type = FieldType.Nested)
    private List<ProductTranslationElasticsearch> productTranslations;


    public ProductElasticsearch(String id, String proName, String description, String listProImg,
                                Boolean isDeleted, LocalDateTime dateCreated,
                                LocalDateTime dateUpdated, LocalDateTime dateDeleted,Integer cateId) {
        this.id = id;
        this.proName = proName;
        this.description = description;
        this.listProImg = listProImg;
        this.isDeleted = isDeleted;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.dateDeleted = dateDeleted;
        this.categoryId = cateId;
    }


}
