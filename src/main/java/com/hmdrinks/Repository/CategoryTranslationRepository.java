package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Category;
import com.hmdrinks.Entity.CategoryTranslation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation,Integer> {

    CategoryTranslation findByCategory_CateIdAndIsDeletedFalse(Integer cateId);
    CategoryTranslation findByCategory_CateId(Integer cateId);
    CategoryTranslation findByCateNameAndCategory_CateIdNot(String cateName, Integer cateId );
    CategoryTranslation findByCateName(String cateName);
    Page<CategoryTranslation> findByCateNameContaining(String cateName, Pageable pageable);
    List<CategoryTranslation> findByCateNameContaining(String cateName);

}
