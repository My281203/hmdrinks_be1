package com.hmdrinks.Entity;

import com.hmdrinks.Enum.Language;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "product_translation_elasticsearch")
public class ProductTranslationElasticsearch {

    @Field(type = FieldType.Keyword)
    private String id; // ID trong Elasticsearch

    @Field(type = FieldType.Text, analyzer = "standard")
    private String proName;

    @Field(type = FieldType.Integer)
    private  int proId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateDeleted;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateUpdated;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dateCreated;

    @Field(type = FieldType.Keyword)
    private Language language;


}
