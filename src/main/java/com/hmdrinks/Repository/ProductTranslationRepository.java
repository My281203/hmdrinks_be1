package com.hmdrinks.Repository;

import com.hmdrinks.Entity.ProductTranslation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ProductTranslationRepository extends JpaRepository<ProductTranslation,Integer> {
    ProductTranslation findByProTransId(Integer proId);
    ProductTranslation findByProTransIdAndIsDeletedFalse(Integer proId);
    ProductTranslation findByProNameAndIsDeletedFalse(String proName);
    ProductTranslation findByProNameAndProTransIdNot(String proName,Integer proId);
    Page<ProductTranslation> findByIsDeletedFalse(Pageable pageable);
    List<ProductTranslation> findByIsDeletedFalse();
    Page<ProductTranslation> findAll(Pageable pageable);
    List<ProductTranslation> findAll();
    List<ProductTranslation> findAllByIsDeletedFalse();
    Page<ProductTranslation> findByProNameContaining(String proName, Pageable pageable);
    Page<ProductTranslation> findByProNameContainingAndIsDeletedFalse(String proName, Pageable pageable);
    List<ProductTranslation> findAllByProduct_ProIdIn(Set<Integer> productIds);

    List<ProductTranslation> findByProduct_ProIdInAndIsDeletedFalse(List<Integer> proIds);




    ProductTranslation findByProduct_ProIdAndIsDeletedFalse(int cateId);
    ProductTranslation findByProduct_ProId(int cateId);

    ProductTranslation findByProName(String proName);

    ProductTranslation findByProNameAndProduct_ProIdNot(String proName, int proId);

    List<ProductTranslation> findAllByProduct_ProIdInAndIsDeletedFalse(List<Integer> productIds);

    List<ProductTranslation> findByProduct_ProIdIn(Set<Integer> productIds);

    List<ProductTranslation> findAllByProductProIdIn(List<Integer> productIds);
}