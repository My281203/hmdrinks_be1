package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Post;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Type_Post;
import com.hmdrinks.Service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface PostRepository extends JpaRepository<Post, Integer> {
    Post findByPostId(int postId);
    Post findByPostIdAndIsDeletedFalse(int postId);
    List<Post> findAll();
    Page<Post> findAll(Pageable pageable);
    Page<Post> findAllByIsDeletedFalse(Pageable pageable);
    List<Post> findAllByIsDeletedFalse();

    Page<Post> findAllByIsDeletedFalseOrderByPostIdDesc(Pageable pageable);
    List<Post> findAllByIsDeletedFalseOrderByPostIdDesc();

    List<Post> findByUserUserIdAndIsDeletedFalse(Integer userId);
    Page<Post> findAllByType(Type_Post typePost,Pageable pageable);
    List<Post> findAllByType(Type_Post typePost);

    Page<Post> findAllByTypeAndIsDeletedFalse(Type_Post typePost,Pageable pageable);
    List<Post> findAllByTypeAndIsDeletedFalse(Type_Post typePost);


    @Query(value = """

            SELECT p.post_id AS postId, p.type AS type, p.banner_url AS bannerUrl,
           COALESCE(pt.title, p.title) AS title,
           COALESCE(pt.description, p.description) AS description,
           COALESCE(pt.short_des, p.short_des) AS shortDes,
           p.date_create AS dateCreate, p.date_deleted AS dateDeleted, p.is_deleted AS isDeleted,
           p.user_id AS userId,
           v.voucher_id AS voucherId, v.key AS key, v.number AS number,
           v.start_date AS startDate, v.end_date AS endDate, v.discount AS discount, v.status AS status
    FROM post p
    LEFT JOIN (
        SELECT pt.* FROM post_translation pt\s
        WHERE pt.is_deleted = false AND pt.language = :language
    ) pt ON pt.post_id = p.post_id
    LEFT JOIN voucher v ON v.voucher_id = p.voucher_id
    WHERE p.is_deleted = false
    
    /* Pagination handled by Spring Data */
    """,
            countQuery = "SELECT count(*) FROM post p WHERE p.is_deleted = false",
            nativeQuery = true
    )
    Page<PostService.PostProjection> findAllPostNative(@Param("language") String language, Pageable pageable);



}
