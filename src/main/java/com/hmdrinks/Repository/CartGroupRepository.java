package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Cart;
import com.hmdrinks.Entity.CartGroup;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Service.CartGroupService;
import com.hmdrinks.Service.CartService;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartGroupRepository extends JpaRepository<CartGroup,Integer> {
    CartGroup findByUserUserIdAndGroupOrderMemberMemberId(int userId, Integer memberId);
    @Query("SELECT c.cartId AS cartId, c.totalPrice AS totalPrice, c.totalProduct AS totalProduct, c.user.userId AS userId " +
            "FROM CartGroup c WHERE c.user.userId = :userId and c.groupOrderMember.memberId = :memberId")
    List<CartGroupService.CartGroupProjection> findCartProjectionsByUserIdAndGroupOrderMemberMemberId(@Param("userId") Integer userId,@Param("memberId") Integer memberId);

    List<CartGroup> findByUserUserId(int userId);

    @Transactional
    @Modifying
    @Query("UPDATE CartGroup c SET c.totalProduct = 0, c.totalPrice = 0  WHERE c.cartId = :cartId")
    void updateCartAfterDelete(@Param("cartId") Integer cartId);



    CartGroup findByCartId(int cartId);

}

