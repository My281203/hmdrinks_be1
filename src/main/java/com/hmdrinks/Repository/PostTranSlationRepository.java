package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Post;
import com.hmdrinks.Entity.PostTranslation;
import com.hmdrinks.Enum.Type_Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PostTranSlationRepository extends JpaRepository<PostTranslation, Integer> {
    PostTranslation findByPostTransId(int postId);
    PostTranslation findByPostTransIdAndIsDeletedFalse(int postId);
    PostTranslation findByPost_PostIdAndIsDeletedFalse(int postId);
    PostTranslation findByPost_PostId(int postId);
    List<PostTranslation> findAll();
    Page<PostTranslation> findAll(Pageable pageable);
    Page<PostTranslation> findAllByIsDeletedFalse(Pageable pageable);
    List<PostTranslation> findAllByIsDeletedFalse();

    Page<PostTranslation> findAllByIsDeletedFalseOrderByPostTransIdDesc(Pageable pageable);
    List<PostTranslation> findAllByIsDeletedFalseOrderByPostTransIdDesc();

}
