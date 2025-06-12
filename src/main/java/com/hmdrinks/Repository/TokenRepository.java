package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Integer> {

    Optional<Token> findByAccessToken(String accessToken);

    @Query("SELECT t FROM Token t WHERE t.user.userId = :userId")
    Token findByUserUserId(@Param("userId") Integer userId);


    Optional<Token> findByRefreshToken(String refreshToken);

}
