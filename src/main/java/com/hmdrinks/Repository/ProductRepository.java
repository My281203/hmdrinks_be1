package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Category;
import com.hmdrinks.Entity.Product;
import com.hmdrinks.Entity.ProductVariants;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Service.ProductRatingAvgDTO;
import com.hmdrinks.Service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;



public interface ProductRepository extends JpaRepository<Product,Integer> {
    Product findByProId(Integer proId);
    Product findByProIdAndIsDeletedFalse(Integer proId);
    @Query("""
    SELECT 
        p.proId AS proId,
        p.category.cateId AS cateId,
        p.proName AS proName,
        p.description AS description,
        p.listProImg AS listProImg,
        p.isDeleted AS isDeleted,
        p.dateDeleted AS dateDeleted,
        p.dateCreated AS dateCreated,
        p.dateUpdated AS dateUpdated
    FROM Product p
    WHERE p.proId = :proId AND p.isDeleted = false
""")
    ProductService.ProductDetailProjection findProductDetailProjectionById(@Param("proId") Integer id);

    Product findByProNameAndIsDeletedFalse(String proName);
    Product findByProNameAndProIdNot(String proName,Integer proId);
    Page<Product> findByIsDeletedFalse(Pageable pageable);
    List<Product> findByIsDeletedFalse();
    Page<Product> findAll(Pageable pageable);
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.productVariants " +
            "WHERE p.isDeleted = false")
    List<Product> findAllWithVariants(Pageable pageable);
    @Query(
            value = "SELECT * FROM product WHERE is_deleted = false ORDER BY date_created ASC LIMIT :limit OFFSET :offset",
            countQuery = "SELECT COUNT(*) FROM product WHERE is_deleted = false",
            nativeQuery = true)
    List<Product> findAllNotDeletedNativeManual(@Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM product WHERE is_deleted = false", nativeQuery = true)
    long countAllNotDeleted();


    @Query(value = "SELECT * FROM product WHERE is_deleted = false", nativeQuery = true)
    List<Product> findAllByIsDeletedFalseNative();

    List<Product> findAll();
    List<Product> findAllByIsDeletedFalse();
    List<Product> findAllByProIdIn(List<Integer> proIds);
    boolean existsByProIdAndIsDeletedFalse(int proId);

    @Query(value = """
    SELECT p.pro_id as proId,
           c.cate_id as cateId,
           p.pro_name as proName,
           p.description as description,
           p.is_deleted as isDeleted,
           p.date_deleted as dateDeleted,
           p.date_created as dateCreated,
           p.date_updated as dateUpdated,
           p.list_pro_img as listProImg,
           COALESCE(r.avg_rating, 0) as averageRating
    FROM product p
    LEFT JOIN category c ON p.category_id = c.cate_id
    LEFT JOIN (
        SELECT pro_id, AVG(rating_star) as avg_rating
        FROM review
        WHERE is_deleted = false
        GROUP BY pro_id
    ) r ON p.pro_id = r.pro_id
    WHERE p.is_deleted = false
    """,
            countQuery = "SELECT COUNT(*) FROM product p WHERE p.is_deleted = false",
            nativeQuery = true)
    Page<ProductService.ProductWithRatingProjection> findAllProductsWithAvgRating(Pageable pageable);



    @Query("""
    SELECT 
        v.varId AS varId,
        v.product.proId AS productId,
        v.size AS size,
        v.price AS price,
        v.stock AS stock,
        v.isDeleted AS isDeleted,
        v.dateDeleted AS dateDeleted,
        v.dateCreated AS dateCreated,
        v.dateUpdated AS dateUpdated
    FROM ProductVariants v
    WHERE v.product.proId = :productId
""")
    List<ProductService.ProductVariantProjection> findVariantProjectionByProductId(@Param("productId") int productId);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.productVariants v " +
            "LEFT JOIN FETCH p.category c " +
            "WHERE v.stock > 0 AND p.isDeleted = false")
    Page<Product> findAvailableProducts(Pageable pageable);


    @Query("""
    SELECT DISTINCT p FROM Product p
    JOIN p.productVariants v
    WHERE p.isDeleted = false AND v.stock > 0
""")
    List<Product> findAvailableProducts();


    @Query("SELECT p.proId as proId, p.category as category, p.proName as proName, p.listProImg as listProImg, " +
            "p.description as description, p.isDeleted as isDeleted, p.dateDeleted as dateDeleted, " +
            "p.dateCreated as dateCreated, p.dateUpdated as dateUpdated, " +
            "COALESCE(AVG(r.ratingStar), 0) as avgRating " +
            "FROM Product p LEFT JOIN Review r ON r.product.proId = p.proId " +
            "WHERE p.isDeleted = false " +
            "GROUP BY p.proId, p.category, p.proName, p.listProImg, p.description, p.isDeleted, p.dateDeleted, p.dateCreated, p.dateUpdated")
    Page<ProductService.ProductWithAvgRatingProjection> findProductsWithAvgRating(Pageable pageable);



    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.productVariants pv " +
            "LEFT JOIN FETCH p.productTranslations pt " +
            "WHERE p.isDeleted = false")
    Page<Product> findAllWithVariantsAndTranslation(Pageable pageable);

    long countByIsDeletedFalse();


    @Query(
            value = """
        SELECT 
            p.pro_id AS proId,
            p.category_id AS category,
            p.pro_name AS proName,
            p.list_pro_img AS listProImg,
            p.description AS description,
            p.is_deleted AS isDeleted,
            p.date_deleted AS dateDeleted,
            p.date_created AS dateCreated,
            p.date_updated AS dateUpdated,
            COALESCE(AVG(r.rating_star), 0) AS avgRating
        FROM product p
        LEFT JOIN review r ON p.pro_id = r.pro_id
        WHERE p.is_deleted = false
        GROUP BY p.pro_id
        """,
            countQuery = """
        SELECT COUNT(*) FROM product p WHERE p.is_deleted = false
        """,
            nativeQuery = true
    )
    Page<ProductService.ProductWithAvgRatingProjection> findAllWithAvgRating(Pageable pageable);
    Page<Product> findByProNameContaining(String proName, Pageable pageable);

    Page<Product> findByProNameContainingAndIsDeletedFalse(String proName, Pageable pageable);
    Page<Product> findByProNameContainingAndIsDeletedFalseAndCategory_CateId(String proName, Pageable pageable,Integer cateId );
    Page<Product> findByCategory_CateId(int cateId, Pageable pageable);
    List<Product> findByCategory_CateId(int cateId);

    Page<Product> findByCategory_CateIdAndIsDeletedFalse(int cateId,Pageable pageable);
    List<Product> findByCategory_CateIdAndIsDeletedFalse(int cateId);


    @Query("SELECT pv FROM Product pv " +
            "LEFT JOIN pv.reviews r " +
            "WHERE pv.category.cateId = :categoryId " +
            "AND pv.proId IN :productIds " +
            "AND pv.isDeleted = false " +
            "AND r.isDeleted = false " +
            "GROUP BY pv.proId " +
            "HAVING COUNT(r) > 0 " +
            "ORDER BY AVG(r.ratingStar) DESC")
    Page<Product> findTopRatedProductsDesc(
            @Param("categoryId") int categoryId,
            @Param("productIds") List<Integer> productIds,
            Pageable pageable
    );

    @Query("SELECT pv FROM Product pv " +
            "LEFT JOIN pv.reviews r " +
            "WHERE pv.category.cateId = :categoryId " +
            "AND pv.isDeleted = false " +
            "AND r.isDeleted = false " +
            "GROUP BY pv.proId " +
            "HAVING COUNT(r) > 0 " +
            "ORDER BY AVG(r.ratingStar) DESC")
    Page<Product> findTopRatedProductsDescByCategory(
            @Param("categoryId") int categoryId,
            Pageable pageable
    );


    @Query("SELECT pv FROM Product pv " +
            "LEFT JOIN pv.reviews r " +
            "WHERE pv.isDeleted = false " +
            "AND (r.isDeleted = false OR r IS NULL) " +
            "GROUP BY pv.proId " +
            "ORDER BY AVG(CASE WHEN r.ratingStar IS NOT NULL THEN r.ratingStar ELSE 0 END) DESC")
    Page<Product> findTopRatedProductsDesc(Pageable pageable);



    @Query("SELECT new com.hmdrinks.Service.ProductRatingAvgDTO(" +
            "r.product.proId, AVG(r.ratingStar)) " +
            "FROM Review r " +
            "WHERE r.product.isDeleted = false " +
            "GROUP BY r.product.proId")
    List<ProductRatingAvgDTO> findAllAvgRatings();




    @Query("SELECT AVG(r.ratingStar) " +
            "FROM Product pv " +
            "LEFT JOIN pv.reviews r " +
            "WHERE pv.category.cateId = :categoryId " +
            "AND pv.proId = :productId " +
            "AND pv.isDeleted = false " +
            "AND r.isDeleted = false " +
            "GROUP BY pv.proId " +
            "HAVING COUNT(r) > 0") // Có ít nhất 1 đánh giá
    Double findAverageRatingByProductId(
            @Param("categoryId") int categoryId,
            @Param("productId") int productId
    );






    @Query("SELECT pv FROM Product pv " +
            "LEFT JOIN pv.reviews r " +
            "WHERE pv.category.cateId = :categoryId " +
            "AND pv.proId IN :productIds " +
            "AND pv.isDeleted = false " +
            "AND r.isDeleted = false " +
            "GROUP BY pv.proId " +
            "HAVING COUNT(r) > 0 " +
            "ORDER BY AVG(r.ratingStar) ASC")
    Page<Product> findTopRatedProductsAsc(
            @Param("categoryId") int categoryId,
            @Param("productIds") List<Integer> productIds,
            Pageable pageable
    );

    @Query("SELECT pv FROM Product pv " +
            "LEFT JOIN pv.reviews r " +
            "WHERE pv.category.cateId = :categoryId " +
            "AND pv.isDeleted = false " +
            "AND r.isDeleted = false " +
            "GROUP BY pv.proId " +
            "HAVING COUNT(r) > 0 " +
            "ORDER BY AVG(r.ratingStar) ASC")
    Page<Product> findTopRatedProductsAscByCategory(
            @Param("categoryId") int categoryId,
            Pageable pageable
    );

    @Query("SELECT pv FROM Product pv " +
            "LEFT JOIN pv.reviews r " +
            "WHERE pv.isDeleted = false " +
            "AND (r.isDeleted = false OR r IS NULL) " +
            "GROUP BY pv.proId " +
            "ORDER BY AVG(CASE WHEN r.ratingStar IS NOT NULL THEN r.ratingStar ELSE 0 END) ASC")
    Page<Product> findTopRatedProductsAsc(Pageable pageable);

    @Query("SELECT pv.varId, pv.size, COALESCE(SUM(ci.quantity), 0) as totalSold " +
            "FROM ProductVariants pv " +
            "LEFT JOIN pv.product p " +
            "LEFT JOIN CartItem ci ON ci.productVariants.varId = pv.varId AND (ci.isDeleted = false) " +
            "LEFT JOIN Cart c ON ci.cart.cartId = c.cartId " +
            "LEFT JOIN OrderItem oi ON c.orderItem.orderItemId = oi.orderItemId " +
            "LEFT JOIN Orders o ON oi.order.orderId = o.orderId AND o.status = 'CONFIRMED' AND o.isDeleted = false " +
            "WHERE pv.isDeleted = false " +
            "GROUP BY pv.varId, pv.size " +
            "ORDER BY " +
            "   CASE WHEN COALESCE(SUM(ci.quantity), 0) = 0 THEN 1 ELSE 0 END ASC, " +
            "   COALESCE(SUM(ci.quantity), 0) DESC")
    Page<Object[]> findBestSellingProducts(Pageable pageable);














    @Query("SELECT pv.varId, pv.size, COALESCE(SUM(ci.quantity), 0) as totalSold " +
            "FROM ProductVariants pv " +
            "LEFT JOIN pv.product p " +  // Liên kết với bảng Product
            "LEFT JOIN CartItem ci ON ci.productVariants.varId = pv.varId AND ci.isDeleted = false " +
            "LEFT JOIN Cart c ON ci.cart.cartId = c.cartId " +
            "LEFT JOIN OrderItem oi ON c.orderItem.orderItemId = oi.orderItemId " +
            "LEFT JOIN Orders o ON oi.order.orderId = o.orderId AND o.status = 'CONFIRMED' AND o.isDeleted = false " +
            "WHERE pv.isDeleted = false AND p.category.cateId = :cateId " + // Lọc theo cateId
            "GROUP BY pv.varId, pv.size " +
            "ORDER BY " +
            "   CASE WHEN COALESCE(SUM(ci.quantity), 0) = 0 THEN 1 ELSE 0 END ASC, " +
            "   COALESCE(SUM(ci.quantity), 0) DESC")
    Page<Object[]> findBestSellingProductsByCategory(@Param("cateId") int cateId, Pageable pageable);



    @Query("SELECT pv.varId, pv.size, COALESCE(SUM(ci.quantity), 0) as totalSold " +
            "FROM ProductVariants pv " +
            "LEFT JOIN pv.product p " +  // Liên kết với bảng Product
            "LEFT JOIN CartItem ci ON ci.productVariants.varId = pv.varId AND ci.isDeleted = false " +
            "LEFT JOIN Cart c ON ci.cart.cartId = c.cartId " +
            "LEFT JOIN OrderItem oi ON c.orderItem.orderItemId = oi.orderItemId " +
            "LEFT JOIN Orders o ON oi.order.orderId = o.orderId AND o.status = 'CONFIRMED' AND o.isDeleted = false " +
            "WHERE pv.isDeleted = false " +
            "AND p.category.cateId = :cateId " +
            "AND p.proId IN :productIds " +
            "GROUP BY pv.varId, pv.size " +
            "ORDER BY " +
            "   CASE WHEN COALESCE(SUM(ci.quantity), 0) = 0 THEN 1 ELSE 0 END ASC, " +
            "   COALESCE(SUM(ci.quantity), 0) DESC")
    Page<Object[]> findBestSellingProductsByCategoryAndProductIds(@Param("cateId") int cateId,
                                                                  @Param("productIds") List<Integer> productIds,
                                                                  Pageable pageable);

    @Query(value = "SELECT p.*, " +
            "(SELECT MIN(v.price) " +
            " FROM product_variants v " +
            " WHERE v.pro_id = p.pro_id " +
            "   AND v.is_deleted = false) AS minPrice " +
            "FROM product p " +
            "WHERE p.category_id = :categoryId " +
            "  AND p.pro_id IN :productIds " +
            "  AND p.is_deleted = false " +
            "ORDER BY minPrice ASC",
            countQuery = "SELECT COUNT(*) " +
                    "FROM product p " +
                    "WHERE p.category_id = :categoryId " +
                    "  AND p.pro_id IN :productIds " +
                    "  AND p.is_deleted = false",
            nativeQuery = true)
    Page<Product> findProductsWithMinPrice(@Param("categoryId") int categoryId,
                                           @Param("productIds") List<Integer> productIds,
                                           Pageable pageable);

    @Query(value = "SELECT p.*, " +
            "(SELECT MIN(v.price) " +
            " FROM product_variants v " +
            " WHERE v.pro_id = p.pro_id " +
            "   AND v.is_deleted = false) AS minPrice " +
            "FROM product p " +
            "WHERE p.category_id = :categoryId " +
            "  AND p.is_deleted = false " +
            "ORDER BY minPrice ASC",
            countQuery = "SELECT COUNT(*) " +
                    "FROM product p " +
                    "WHERE p.category_id = :categoryId " +
                    "  AND p.is_deleted = false",
            nativeQuery = true)
    Page<Product> findProductsWithMinPriceNoProduct(@Param("categoryId") int categoryId, Pageable pageable);


    @Query(value = "SELECT p.*, " +
            "(SELECT MIN(v.price) " +
            " FROM product_variants v " +
            " WHERE v.pro_id = p.pro_id " +
            "   AND v.is_deleted = false) AS minPrice " +
            "FROM product p " +
            "WHERE p.is_deleted = false " +
            "ORDER BY minPrice ASC",
            countQuery = "SELECT COUNT(*) FROM product p WHERE p.is_deleted = false",
            nativeQuery = true)
    Page<Product> findAllProductsWithMinPriceNoCategory(Pageable pageable);


    @Query(value = "SELECT p.*, " +
            "(SELECT MAX(v.price) " +
            " FROM product_variants v " +
            " WHERE v.pro_id = p.pro_id " +
            "   AND v.is_deleted = false) AS maxPrice " +
            "FROM product p " +
            "WHERE p.category_id = :categoryId " +
            "  AND p.is_deleted = false " +
            "ORDER BY maxPrice DESC",
            countQuery = "SELECT COUNT(*) " +
                    "FROM product p " +
                    "WHERE p.category_id = :categoryId " +
                    "  AND p.is_deleted = false",
            nativeQuery = true)
    Page<Product> findProductsWithMaxPriceNoProduct(@Param("categoryId") int categoryId, Pageable pageable);

    @Query(value = "SELECT p.*, " +
            "(SELECT MAX(v.price) " +
            " FROM product_variants v " +
            " WHERE v.pro_id = p.pro_id " +
            "   AND v.is_deleted = false) AS maxPrice " +
            "FROM product p " +
            "WHERE p.is_deleted = false " +
            "ORDER BY maxPrice DESC",
            countQuery = "SELECT COUNT(*) " +
                    "FROM product p " +
                    "WHERE p.is_deleted = false",
            nativeQuery = true)
    Page<Product> findAllProductsWithMaxPriceNoCategory(Pageable pageable);

    @Query(value = "SELECT p.*, " +
            "(SELECT MAX(v.price) " +
            " FROM product_variants v " +
            " WHERE v.pro_id = p.pro_id " +
            "   AND v.is_deleted = false) AS maxPrice " +
            "FROM product p " +
            "WHERE p.category_id = :categoryId " +
            "  AND p.pro_id IN :productIds " +
            "  AND p.is_deleted = false " +
            "ORDER BY maxPrice DESC",
            countQuery = "SELECT COUNT(*) " +
                    "FROM product p " +
                    "WHERE p.category_id = :categoryId " +
                    "  AND p.pro_id IN :productIds " +
                    "  AND p.is_deleted = false",
            nativeQuery = true)
    Page<Product> findProductsWithMaxPrice(@Param("categoryId") int categoryId,
                                           @Param("productIds") List<Integer> productIds,
                                           Pageable pageable);



    Page<Product> findByCategory_CateIdAndProIdInAndIsDeletedFalse(
            int categoryId,
            List<Integer> productIds,
            Pageable pageable
    );



}