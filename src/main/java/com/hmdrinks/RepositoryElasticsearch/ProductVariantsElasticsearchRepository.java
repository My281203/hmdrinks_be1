package com.hmdrinks.RepositoryElasticsearch;

import com.hmdrinks.Entity.ProductVariantsElasticsearch;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantsElasticsearchRepository extends ElasticsearchRepository<ProductVariantsElasticsearch, String> {

    List<ProductVariantsElasticsearch> findByProductIdAndIsDeletedFalse(Integer productId);
}
