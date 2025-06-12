package com.hmdrinks.Entity;

import com.hmdrinks.Enum.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import scala.Int;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "product_variants")  // Chỉ định index trong Elasticsearch
public class ProductVariantsElasticsearch {

    @Id
    private String id; // ID trong Elasticsearch

    @Field(type = FieldType.Keyword)
    private Integer productId; // ID của sản phẩm (liên kết với Product)

    @Field(type = FieldType.Keyword)
    private Size size;  // Enum lưu dưới dạng keyword

    @Field(type = FieldType.Double)
    private double price;

    @Field(type = FieldType.Integer)
    private int stock;

    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateDeleted;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateUpdated;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateCreated;
}
