package com.hmdrinks.RepositoryElasticsearch;

import com.hmdrinks.Entity.ProductTranslationElasticsearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductTranslationElasticsearchRepository extends ElasticsearchRepository<ProductTranslationElasticsearch, String> {

    List<ProductTranslationElasticsearch> findByProNameContainingAndIsDeletedFalse(String proName);


    @Query("""
{
  "bool": {
    "must": [
      {
        "bool": {
          "should": [
             { "multi_match": { "query": "?0", "fields": ["proName"], "fuzziness": "AUTO" } },
                  { "match_phrase": { "proName": "?0" } },
                  { "wildcard": { "proName": "*?0*" } },
                  { "regexp": { "proName": ".*?0.*" } },
                  { "query_string": { "query": "?0*", "fields": ["proName"], "analyze_wildcard": true } }
          ]
        }
      },
      { "term": { "isDeleted": false } }
    ]
  }
}
""")
    Page<ProductTranslationElasticsearch> searchByProNameAndIsDeletedFalse(String proName, Pageable pageable);





    @Query("""
{
  "bool": {
    "should": [
       { "multi_match": { "query": "?0", "fields": ["proName"], "fuzziness": "AUTO" } },
            { "match_phrase": { "proName": "?0" } },
            { "wildcard": { "proName": "*?0*" } },
            { "regexp": { "proName": ".*?0.*" } },
            { "query_string": { "query": "?0*", "fields": ["proName"], "analyze_wildcard": true } }
    ]
  }
}
""")
    Page<ProductTranslationElasticsearch> searchByProName(String proName, String description, Pageable pageable);



    @Query("""
{
  "bool": {
    "must": [
      {
        "bool": {
          "should": [
             { "multi_match": { "query": "?0", "fields": ["proName"], "fuzziness": "AUTO" } },
                  { "match_phrase": { "proName": "?0" } },
                  { "wildcard": { "proName": "*?0*" } },
                  { "regexp": { "proName": ".*?0.*" } },
                  { "query_string": { "query": "?0*", "fields": ["proName"], "analyze_wildcard": true } }
            
          ]
        }
      },
      { "term": { "isDeleted": false } }
    ]
  }
}
""")
    Page<ProductTranslationElasticsearch> searchByProName(String proName, Pageable pageable);


    Page<ProductTranslationElasticsearch> findByProNameContainingOrDescriptionContainingAndIsDeletedFalse(String keyword, String keyword1, Pageable pageable);
}
