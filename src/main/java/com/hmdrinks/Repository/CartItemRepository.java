package com.hmdrinks.Repository;

import com.hmdrinks.Entity.CartItem;
import com.hmdrinks.Entity.ProductVariants;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Service.CartItemWithTranslationProjection;
import com.hmdrinks.Service.CartService;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem,Integer> {
    List<CartItem> findByCart_CartIdAndIsDisabledFalse(Integer id);


    @Query(value = """
    SELECT p.list_pro_img
    FROM cart_item ci
    JOIN product_variants pv ON ci.pro_id = pv.pro_id AND ci.size = pv.size
    JOIN product p ON pv.pro_id = p.pro_id
    WHERE ci.cart_item_id = :cartItemId
    """, nativeQuery = true)
    String findListProImgByCartItemId(@Param("cartItemId") int cartItemId);

    @Query(value = """
    SELECT p.pro_name
    FROM cart_item ci
    JOIN product_variants pv ON ci.pro_id = pv.pro_id AND ci.size = pv.size
    JOIN product p ON pv.pro_id = p.pro_id
    WHERE ci.cart_item_id = :cartItemId
    """, nativeQuery = true)
    String findProductNameByCartItemId(@Param("cartItemId") int cartItemId);


    @Query("""
    SELECT 
        ci.cartItemId AS cartItemId,
        p.proId AS proId,
        COALESCE(pt.proName, p.proName) AS proName,
        ci.productVariants.size AS size,
        ci.totalPrice AS totalPrice,
        ci.quantity AS quantity,
        FUNCTION('SUBSTRING_INDEX', p.listProImg, ', ', 1) AS listProImg
    FROM CartItem ci
    JOIN ci.productVariants.product p
    LEFT JOIN ProductTranslation pt ON pt.product.proId = p.proId AND pt.language = :lang
    WHERE ci.cart.cartId = :cartId
""")
    List<CartService.CartItemProjection> findCartItemsWithTranslation(@Param("cartId") int cartId, @Param("lang") Language lang);





    List<CartItem> findByCart_CartIdAndIsDeletedFalseAndIsDisabledFalse(Integer id);

    CartItem findByCartItemId(int id);
    CartItem findByCartItemIdAndIsDeletedFalseAndIsDisabledFalse(int id);

    List<CartItem> findByProductVariants_VarId(int varId);

    CartItem findByProductVariants_VarIdAndProductVariants_SizeAndCart_CartId(Integer varId, Size size, Integer cartId);

    List<CartItem> findByProductVariantsIn(List<ProductVariants> productVariants);
    @Transactional
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cartId")
    void deleteAllByCartId(@Param("cartId") Integer cartId);



    @Query("SELECT c.cartItemId AS cartItemId, " +
            "c.productVariants.product.proId AS proId, " +
            "c.productVariants.product.proName AS proName, " +
            "c.productVariants.size AS size, " +
            "c.totalPrice AS totalPrice, " +
            "c.quantity AS quantity, " +
            "c.productVariants.product.listProImg AS listProImg " +
            "FROM CartItem c WHERE c.cart.cartId = :cartId")
    List<CartService.CartItemProjection> findCartItemProjectionByCartId(int cartId);

    List<CartItem> findByProductVariants_VarIdIn(List<Integer> varIds);


    // CartItemRepo
//    @Query("""
//SELECT ci FROM CartItem ci
// JOIN FETCH ci.productVariants pv
// JOIN FETCH pv.product p
// LEFT JOIN FETCH p.productTranslations t WITH t.language = :lang
//WHERE ci.cart.cartId = :cartId
//""")
//    List<CartItem> fetchCartItemsWithAll(@Param("cartId") int cartId,
//                                         @Param("lang") String lang);




}
