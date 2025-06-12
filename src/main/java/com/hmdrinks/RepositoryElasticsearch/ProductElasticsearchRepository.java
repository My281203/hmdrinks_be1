package com.hmdrinks.RepositoryElasticsearch;

import com.hmdrinks.Entity.Product;
import com.hmdrinks.Entity.ProductElasticsearch;
import com.hmdrinks.Entity.ProductTranslationElasticsearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductElasticsearchRepository extends ElasticsearchRepository<ProductElasticsearch, String> {

    List<ProductElasticsearch> findByProNameContaining(String keyword);
    Page<ProductElasticsearch> findByProNameContainingOrDescriptionContainingAndIsDeletedFalse(
            String proName, String description, Pageable pageable);


//    @Query("""
//{
//  "bool": {
//    "must": [
//      {
//        "bool": {
//          "should": [
//            { "match": { "proName": "?0" } },
//            { "match": { "description": "?1" } }
//          ]
//        }
//      },
//      { "term": { "isDeleted": false } }
//    ]
//  }
//}
//""")
//    Page<ProductElasticsearch> searchByProNameOrDescriptionAndIsDeletedFalse(String proName, String description, Pageable pageable);
//
//    @Query("""
//{
//  "bool": {
//    "should": [
//      { "match": { "proName": "?0" } },
//      { "match": { "description": "?1" } }
//    ]
//  }
//}
//""")
//    Page<ProductElasticsearch> searchByProNameOrDescription(String proName, String description, Pageable pageable);
//
//
//
//
//
//
//    @Query("""
//{
//  "bool": {
//    "must": [
//      {
//        "bool": {
//          "should": [
//            { "match": { "proName": "?0" } },
//            { "match": { "description": "?1" } }
//          ]
//        }
//      },
//      { "term": { "categoryId": ?2 } },
//      { "term": { "isDeleted": false } }
//    ]
//  }
//}
//""")
//    Page<ProductElasticsearch> searchByProNameOrDescriptionAndCategoryId(String proName, String description, Integer cateId, Pageable pageable);


    @Query("""
{
  "bool": {
    "should": [
      {
        "wildcard": {
          "proName.keyword": {
            "value": "*?0*",
            "boost": 4
          }
        }
      },
      {
        "match": {
          "proName": {
            "query": "?0",
            "fuzziness": 2,
            "prefix_length": 0,
            "boost": 3
          }
        }
      },
      {
        "match_phrase": {
          "proName": {
            "query": "?0",
            "boost": 2
          }
        }
      },
      {
        "query_string": {
          "query": "?0*",
          "fields": ["proName"],
          "analyze_wildcard": true
        }
      }
    ],
    "filter": [
      { "term": { "isDeleted": false } }
    ]
  }
}
""")
    Page<ProductElasticsearch> searchByProNameAndIsDeletedFalse(String keyword, Pageable pageable);

//    @Query("""
//{
//  "bool": {
//    "should": [
//       { "multi_match": { "query": "?0", "fields": ["proName"], "fuzziness": "AUTO" } },
//      { "match_phrase": { "proName": "?0" } },
//      { "wildcard": { "proName": "*?0*" } },
//      { "regexp": { "proName": ".*?0.*" } },
//      { "query_string": { "query": "?0*", "fields": ["proName"], "analyze_wildcard": true } }
//    ]
//  }
//}
//""")
//    Page<ProductElasticsearch> searchByProNameAndIsDeletedFalse(String proName,  Pageable pageable);


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
    ],
          "minimum_should_match": 1
        }
      }
    ],
    "must_not": [
         { "term": { "isDeleted": true } }
       ],
    "filter": [
      { "term": { "categoryId": "?1" } },
  
    ]
  }
}
""")
    Page<ProductElasticsearch> searchByProNameAndCategoryIdAndIsDeletedFalse(String proName, Long categoryId, Pageable pageable,Boolean check);









    List<ProductElasticsearch> findByProNameContainingOrDescriptionContainingAndIsDeletedFalse(String proName, String description);
}
