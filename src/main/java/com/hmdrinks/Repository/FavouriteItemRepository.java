package com.hmdrinks.Repository;

import com.hmdrinks.Entity.CartItem;
import com.hmdrinks.Entity.Favourite;
import com.hmdrinks.Entity.FavouriteItem;
import com.hmdrinks.Entity.ProductVariants;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Service.FavouriteService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavouriteItemRepository extends JpaRepository<FavouriteItem,Integer> {
    List<FavouriteItem> findByFavourite_FavId(Integer id);
    List<FavouriteItem> findByFavourite_FavIdAndIsDeletedFalse(Integer id);
    List<FavouriteItem> findByProductVariants_VarId(Integer id);

    @Query("SELECT fi.productVariants.product.proId AS proId, COUNT(fi.favItemId) AS count " +
            "FROM FavouriteItem fi " +
            "GROUP BY fi.productVariants.product.proId")
    List<Object[]> countFavouriteGroupedByProduct();

    FavouriteItem findByFavItemId(int id);

    FavouriteItem findByProductVariants_VarIdAndProductVariants_SizeAndFavourite_FavId(Integer varId, Size size, Integer favId);

    List<FavouriteItem> findByProductVariantsIn(List<ProductVariants> productVariants);

    @Query("""
    SELECT 
        fi.favItemId AS favItemId,
        f.favId AS favId,
        p.proId AS proId,
        pv.size AS size
    FROM FavouriteItem fi
    JOIN fi.favourite f
    JOIN fi.productVariants pv
    JOIN pv.product p
    WHERE fi.favourite.favId = :favouriteId
      AND fi.isDeleted = false
""")
    List<FavouriteService.FavouriteItemProjection> findAllByFavouriteIdWithProjection(@Param("favouriteId") int favouriteId);

}
