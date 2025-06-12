package com.hmdrinks.Repository;

import com.hmdrinks.Entity.CartItemGroup;
import com.hmdrinks.Entity.ProductVariants;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Service.CartGroupService;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemGroupRepository extends JpaRepository<CartItemGroup,Integer> {
      List<CartItemGroup> findByCartGroupCartIdAndIsDisabledFalse(Integer id);
      List<CartItemGroup> findByCartGroupCartIdAndIsDeletedFalseAndIsDisabledFalse(Integer id);
//
      CartItemGroup findByCartItemIdAndIsDisabledFalse(int id);
//
      List<CartItemGroup> findByProductVariants_VarId(int varId);
    List<CartItemGroup> findByProductVariants_VarIdIn(List<Integer> varIds);



    CartItemGroup findByProductVariants_VarIdAndProductVariants_SizeAndCartGroup_CartIdAndIsDeletedFalse(Integer varId, Size size, Integer cartId);

    List<CartItemGroup> findByProductVariantsIn(List<ProductVariants> productVariants);
    @Transactional
    @Modifying
    @Query("DELETE FROM CartItemGroup cig WHERE cig.cartGroup.cartId = :cartId")
    void deleteAllByCartId(@Param("cartId") Integer cartId);
//
//
//
    @Query("SELECT c.cartItemId AS cartItemId, " +
            "c.productVariants.product.proId AS proId, " +
            "c.productVariants.product.proName AS proName, " +
            "c.productVariants.size AS size, " +
            "c.totalPrice AS totalPrice, " +
            "c.quantity AS quantity, " +
            "c.productVariants.product.listProImg AS listProImg " +
            "FROM CartItemGroup c WHERE c.cartGroup.cartId = :cartId")
    List<CartGroupService.CartItemGroupProjection> findCartItemProjectionByCartId(int cartId);


    @Query("SELECT c FROM CartItemGroup c WHERE c.cartGroup.cartId IN :cartIds AND c.isDeleted = false AND c.isDisabled = false")
    List<CartItemGroup> findAllByCartGroupIds(@Param("cartIds") List<Integer> cartIds);



}
