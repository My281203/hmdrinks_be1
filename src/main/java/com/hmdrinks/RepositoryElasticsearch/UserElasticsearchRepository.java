package com.hmdrinks.RepositoryElasticsearch;

import com.hmdrinks.Entity.UserDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserElasticsearchRepository extends ElasticsearchRepository<UserDocument, String> {

    @Query("{" +
            "  \"bool\": {" +
            "    \"should\": [" +
            "      { \"multi_match\": { \"query\": \"?0\", \"fields\": [\"userName\", \"email\", \"fullName\", \"street\", \"district\", \"city\", \"phoneNumber\",\"ward\"], \"fuzziness\": \"AUTO\" } }," +
            "      { \"match_phrase\": { \"userName\": \"?0\" } }," +
            "      { \"match_phrase\": { \"email\": \"?0\" } }," +
            "      { \"match_phrase\": { \"fullName\": \"?0\" } }," +
            "      { \"match_phrase\": { \"street\": \"?0\" } }," +
            "      { \"match_phrase\": { \"district\": \"?0\" } }," +
            "      { \"match_phrase\": { \"city\": \"?0\" } }," +
            "      { \"match_phrase\": { \"phoneNumber\": \"?0\" } }," +
            "      { \"match_phrase\": { \"ward\": \"?0\" } }," +
            "      { \"wildcard\": { \"userName\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"email\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"fullName\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"street\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"district\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"city\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"phoneNumber\": \"*?0*\" } }," +
            "      { \"regexp\": { \"userName\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"email\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"fullName\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"street\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"district\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"city\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"phoneNumber\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"ward\": \".*?0.*\" } }," +
            "      { \"query_string\": { \"query\": \"?0*\", \"fields\": [\"userName\", \"email\", \"fullName\", \"street\", \"district\", \"city\", \"phoneNumber\",\"ward\"], \"analyze_wildcard\": true } }" +
            "    ]," +
            "    \"filter\": [" +
            "      { \"term\": { \"isDeleted\": false } }" +
            "    ]" +
            "  }" +
            "}")
    Page<UserDocument> searchUsers(String keyword, Pageable pageable);

    @Query("{" +
            "  \"bool\": {" +
            "    \"should\": [" +
            "      { \"multi_match\": { \"query\": \"?0\", \"fields\": [\"userName\", \"email\", \"fullName\", \"street\", \"district\", \"city\", \"phoneNumber\"], \"fuzziness\": \"AUTO\" } }," +
            "      { \"match_phrase\": { \"userName\": \"?0\" } }," +
            "      { \"match_phrase\": { \"email\": \"?0\" } }," +
            "      { \"match_phrase\": { \"fullName\": \"?0\" } }," +
            "      { \"match_phrase\": { \"street\": \"?0\" } }," +
            "      { \"match_phrase\": { \"district\": \"?0\" } }," +
            "      { \"match_phrase\": { \"city\": \"?0\" } }," +
            "      { \"match_phrase\": { \"phoneNumber\": \"?0\" } }," +
            "      { \"wildcard\": { \"userName\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"email\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"fullName\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"street\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"district\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"city\": \"*?0*\" } }," +
            "      { \"wildcard\": { \"phoneNumber\": \"*?0*\" } }," +
            "      { \"regexp\": { \"userName\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"email\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"fullName\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"street\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"district\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"city\": \".*?0.*\" } }," +
            "      { \"regexp\": { \"phoneNumber\": \".*?0.*\" } }," +
            "      { \"query_string\": { \"query\": \"?0*\", \"fields\": [\"userName\", \"email\", \"fullName\", \"street\", \"district\", \"city\", \"phoneNumber\"], \"analyze_wildcard\": true } }" +
            "    ]," +
            "    \"filter\": [" +
            "      { \"term\": { \"isDeleted\": false } }" +
            "    ]" +
            "  }" +
            "}")
    List<UserDocument> searchUsers(String keyword);
}
