package com.hmdrinks.RepositoryElasticsearch;

import com.hmdrinks.Entity.CategoryElasticsearch;
import com.hmdrinks.Entity.CategoryTranslationElasticsearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryTranslationElasticsearchRepository extends ElasticsearchRepository<CategoryTranslationElasticsearch, String> {

    @Query("""
{
  "bool": {
    "must": [
      {
        "bool": {
          "should": [
            { "match": { "cateName": "?0" } },
          ]
        }
      },
      { "term": { "isDeleted": false } }
    ]
  }
}
""")
    Page<CategoryTranslationElasticsearch> searchByCateName(String cateName, Pageable pageable);

    @Query("""
{
  "bool": {
    "must": [
      {
        "bool": {
          "should": [
            { "match": { "cateName": "?0" } },
          ]
        }
      },
      { "term": { "isDeleted": false } }
    ]
  }
}
""")
    List<CategoryTranslationElasticsearch> searchByCateName(String cateName);
}
