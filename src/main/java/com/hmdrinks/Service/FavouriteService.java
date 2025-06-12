package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Exception.BadRequestException;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CreateNewCart;
import com.hmdrinks.Request.CreateNewFavourite;
import com.hmdrinks.Response.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FavouriteService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FavouriteRepository favouriteRepository;
    @Autowired
    private FavouriteItemRepository favouriteItemRepository;
    @Autowired
    private ProductTranslationRepository productTranslationRepository;

    @Transactional
    public ResponseEntity<?> createFavourite(CreateNewFavourite req)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if(user == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("UserId not exists");

        }
        Favourite favourite = favouriteRepository.findByUserUserId(user.getUserId());
        if(favourite != null)
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Favourite already exists");
        }
        Favourite favourite1 = new Favourite();
        favourite1.setUser(user);
        favourite1.setIsDeleted(false);
        favourite1.setDateCreated(LocalDateTime.now());
        favouriteRepository.save(favourite1);

        return ResponseEntity.status(HttpStatus.OK).body(new CreateNewFavouriteResponse(
                favourite1.getFavId(),
                favourite1.getUser().getUserId(),
                favourite1.getIsDeleted(),
                favourite1.getDateDeleted(),
                favourite1.getDateUpdated(),
                favourite1.getDateCreated()
        ));
    }

    public interface FavouriteItemProjection {
        Integer getFavItemId();
        Integer getFavId();
        Integer getProId();
        Size getSize();
    }


    @Transactional
    public ResponseEntity<?> getAllItemFavourite(int id, Language language) {

        Favourite favourite = favouriteRepository.findByFavId(id);
        if (favourite == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Favourite not found");
        }

        List<FavouriteItemProjection> projections = favouriteItemRepository.findAllByFavouriteIdWithProjection(id);

        List<CRUDFavouriteItemResponse> responses = projections.stream()
                .map(p -> new CRUDFavouriteItemResponse(
                        p.getFavItemId(),
                        p.getFavId(),
                        p.getProId(),
                        p.getSize()
                )).toList();

        return ResponseEntity.ok(new ListItemFavouriteResponse(
                id,
                responses.size(),
                responses
        ));
    }


    public ResponseEntity<?> getFavoriteById(int userId) {
        Favourite favourite = favouriteRepository.findActiveFavouriteByUserId(userId);
        if (favourite == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User or Favourite not found");
        }

        CreateNewFavouriteResponse favouriteResponse = new CreateNewFavouriteResponse(
                favourite.getFavId(),
                favourite.getUser().getUserId(),
                favourite.getIsDeleted(),
                favourite.getDateDeleted(),
                favourite.getDateUpdated(),
                favourite.getDateCreated()
        );

        return ResponseEntity.ok(favouriteResponse);
    }

}
