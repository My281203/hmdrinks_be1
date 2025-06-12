package com.hmdrinks.Repository;

import com.hmdrinks.Entity.ProductVariants;
import com.hmdrinks.Entity.Review;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Service.ProductService;
import com.hmdrinks.Service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review,Integer> {
  List<Review> findByProduct_ProIdAndIsDeletedFalse(Integer productId);
  List<Review> findByUser_UserId(Integer userId);
  Page<Review> findByProduct_ProId(Integer productId, Pageable pageable);
  Page<Review> findByProduct_ProIdAndIsDeletedFalse(int proId, Pageable pageable);
  List<Review> findByProduct_ProIdAndIsDeletedFalse(int proId);
  List<Review> findAll();
  List<Review> findAllByProduct_ProId(int proId);
  Review findByReviewIdAndIsDeletedFalse(Integer reviewId);
  Review findByReviewIdAndUser_UserIdAndIsDeletedFalse(Integer reviewId, Integer userId);

  List<Review> findAllByProduct_ProIdIn(List<Integer> productIds);



  @Query("""
    SELECT 
        r.reviewId AS reviewId,
        u.userId AS userId,
        p.proId AS proId,
        u.fullName AS fullName,
        r.content AS content,
        r.ratingStar AS ratingStar,
        r.isDeleted AS isDeleted,
        r.dateDeleted AS dateDeleted,
        r.dateUpdated AS dateUpdated,
        r.dateCreated AS dateCreated
    FROM Review r
    JOIN r.user u
    JOIN r.product p
    WHERE r.product.proId = :proId AND r.isDeleted = false
""")
  Page<ReviewService.ReviewProjection> findReviewProjectionsByProductId(@Param("proId") int proId, Pageable pageable);


  @Query("""
    SELECT COUNT(r)
    FROM Review r
    WHERE r.product.proId = :proId AND r.isDeleted = false
""")
  int countAllReviewsByProductId(@Param("proId") int proId);


  @Query("SELECT r.product.proId AS productId, AVG(r.ratingStar) AS avgRating " +
          "FROM Review r " +
          "WHERE r.isDeleted = false AND r.product.proId IN :productIds " +
          "GROUP BY r.product.proId")
  List<Object[]> findAverageRatingForProducts(@Param("productIds") List<Integer> productIds);
//@Query("SELECT r.product.proId, AVG(r.ratingStar) FROM Review r WHERE r.product.proId IN :productIds GROUP BY r.product.proId")
//List<Object[]> findAverageRatingForProducts(@Param("productIds") List<Integer> productIds);


}
