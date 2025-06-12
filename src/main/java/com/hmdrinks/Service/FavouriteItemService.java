package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Status_Cart;
import com.hmdrinks.Exception.BadRequestException;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.*;
import com.hmdrinks.Response.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.sparkproject.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FavouriteItemService {
    @Autowired
    private FavouriteRepository favouriteRepository;
    @Autowired
    private ProductVariantsRepository productVariantsRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private FavouriteItemRepository favouriteItemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductTranslationRepository productTranslationRepository;
    @Autowired
    private SupportFunction supportFunction;



    @Transactional
    public ResponseEntity<?> insertFavouriteItem(InsertItemToFavourite req)
    {
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if(user == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND_404).body("User not found");
        }
        Product product= productRepository.findByProId(req.getProId());
        if(product == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND_404).body("Product not found");
        }
        if(product.getIsDeleted())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST_400).body("Product is deleted");
        }
        ProductVariants productVariants = productVariantsRepository.findBySizeAndProduct_ProIdAndIsDeletedFalse(req.getSize(),req.getProId());
        if(productVariants == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND_404).body("production size not exists");
        }
        Favourite favourite= favouriteRepository.findByUserUserId(req.getUserId());
        if(favourite == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND_404).body("Favourite for userId not exists");
        }
        Favourite favourite1 = favouriteRepository.findByFavId(req.getFavId());
        if(favourite1 == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND_404).body("Favourite not found");
        }
        ProductVariants productVariants1 = productVariantsRepository.findBySizeAndProduct_ProIdAndIsDeletedFalse(req.getSize(),req.getProId());
        FavouriteItem favouriteItem1 = favouriteItemRepository.findByProductVariants_VarIdAndProductVariants_SizeAndFavourite_FavId(productVariants1.getVarId(),req.getSize(),req.getFavId());
        FavouriteItem favouriteItem = new FavouriteItem();
        if(favouriteItem1 == null)
        {
            favouriteItem.setFavourite(favourite);
            favouriteItem.setProductVariants(productVariants);
            favouriteItem.setIsDeleted(false);
            favouriteItemRepository.save(favouriteItem);
            favourite.setDateUpdated(LocalDateTime.now());
            favouriteRepository.save(favourite);
            return ResponseEntity.status(HttpStatus.OK_200).body(new CRUDFavouriteItemResponse (
                    favouriteItem.getFavItemId(),
                    favouriteItem.getFavourite().getFavId(),
                    favouriteItem.getProductVariants().getProduct().getProId(),
                    favouriteItem.getProductVariants().getSize()
            ));
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST_400).body("Favourite item already exists");
        }
    }

    public ResponseEntity<?> deleteOneItem(DeleteOneFavouriteItemReq req)
    {
        FavouriteItem favouriteItem = favouriteItemRepository.findByFavItemId(req.getFavItemId());
        if(favouriteItem == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND_404).body("Favourite item not found");
        }
        favouriteItemRepository.delete(favouriteItem);
        Favourite favourite = favouriteRepository.findByFavId(favouriteItem.getFavourite().getFavId());
        favourite.setDateUpdated(LocalDateTime.now());
        favouriteRepository.save(favourite);
        return ResponseEntity.status(HttpStatus.OK_200).body(new DeleteFavouriteItemResponse(
                "Delete item success"
        ));
    }

    public ResponseEntity<?> deleteAllFavouriteItem(DeleteAllFavouriteItemReq req)
    {
        Favourite favourite = favouriteRepository.findByFavId(req.getFavId());
        if(favourite == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND_404).body("Favourite not found");
        }
        List<FavouriteItem> favouriteItems = favouriteItemRepository.findByFavourite_FavId(req.getFavId());
        favouriteItemRepository.deleteAll(favouriteItems);
        favourite.setDateUpdated(LocalDateTime.now());
        favouriteRepository.save(favourite);
        return ResponseEntity.status(HttpStatus.OK_200).body(new DeleteFavouriteItemResponse(
                "Delete all item success"
        ));
    }

    @Transactional
    public ResponseEntity<?> listAllTotalFavouriteByProId(Language language) {
        // B1: Lấy dữ liệu đã gộp sẵn
        List<Object[]> rawFavouriteCounts = favouriteItemRepository.countFavouriteGroupedByProduct();
        List<Integer> proIds = rawFavouriteCounts.stream()
                .map(row -> (Integer) row[0])
                .collect(Collectors.toList());

        // B2: Load product + translation 1 lần
        List<Product> products = productRepository.findAllByProIdIn(proIds);
        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProId, Function.identity()));

        List<ProductTranslation> translations = productTranslationRepository.findByProduct_ProIdInAndIsDeletedFalse(proIds);
        Map<Integer, ProductTranslation> translationMap = translations.stream()
                .collect(Collectors.toMap(pt -> pt.getProduct().getProId(), Function.identity()));

        // B3: Xử lý
        List<TotalCountFavorite> totalCountFavorites = rawFavouriteCounts.stream()
                .map(row -> {
                    Integer proId = (Integer) row[0];
                    Integer totalCount = ((Number) row[1]).intValue();
                    Product product = productMap.get(proId);

                    String name = "";
                    String des = "";

                    if (language == Language.EN) {
                        ProductTranslation pt = translationMap.get(proId);
                        if (pt != null) {
                            name = pt.getProName();
                            des = pt.getDescription();
                        } else {
                            name = pt.getProName();
                            des = pt.getDescription();


                        }
                    } else {
                        name = product.getProName();
                        des = product.getDescription();
                    }

                    return new TotalCountFavorite(proId, name, totalCount);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ListAllTotalCountFavorite(totalCountFavorites.size(), totalCountFavorites));
    }



}
