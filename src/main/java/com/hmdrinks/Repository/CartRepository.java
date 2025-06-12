package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Cart;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Service.CartService;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart,Integer> {
    Cart findByUserUserIdAndStatus(int userId, Status_Cart status);
    @Query("SELECT c.cartId AS cartId, c.totalPrice AS totalPrice, c.totalProduct AS totalProduct, c.user.userId AS userId, c.status AS status " +
            "FROM Cart c WHERE c.user.userId = :userId")
    List<CartService.CartProjection> findCartProjectionsByUserId(@Param("userId") Integer userId);



    List<Cart> findByUserUserId(int userId);

    @Transactional
    @Modifying
    @Query("UPDATE Cart c SET c.totalProduct = 0, c.totalPrice = 0, c.status = :status WHERE c.cartId = :cartId")
    void updateCartAfterDelete(@Param("cartId") Integer cartId, @Param("status") Status_Cart status);



    Cart findByCartId(int cartId);

    Cart findByCartIdAndStatus(int cartId,Status_Cart statusCart);
}

