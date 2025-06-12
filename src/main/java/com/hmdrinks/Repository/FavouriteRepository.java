package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Cart;
import com.hmdrinks.Entity.Favourite;
import com.hmdrinks.Entity.FavouriteItem;
import com.hmdrinks.Enum.Status_Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavouriteRepository extends JpaRepository<Favourite,Integer> {

    Favourite findByUserUserId(int userId);
    @Query("SELECT f FROM Favourite f WHERE f.user.userId = :userId AND f.user.isDeleted = false")
    Favourite findActiveFavouriteByUserId(@Param("userId") int userId);

    Favourite findByFavId(int cartId);

}

