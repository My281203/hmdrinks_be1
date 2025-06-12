package com.hmdrinks.Controller;

import com.hmdrinks.Entity.Product;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Repository.ProductRepository;
import com.hmdrinks.Repository.UserRepository;
import com.hmdrinks.Request.*;
import com.hmdrinks.Service.ProductService;
import com.hmdrinks.Service.Recommender;
import com.hmdrinks.Service.ReviewService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private Recommender recommender;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    @PostMapping(value = "/create")
    public ResponseEntity<?> createProduct(@RequestBody @Valid CreateProductReq req){
        return productService.crateProduct(req);
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> update(@RequestBody @Valid  CRUDProductReq req){
        return productService.updateProduct(req);
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestParam Language language){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("id", id);
        if (validation != null) return validation;
        return productService.getOneProduct(id,language);
    }

    @GetMapping(value = "/list-product")
    public ResponseEntity<?> listAllProduct(
            @RequestParam(name = "page") String page,
            @RequestParam(name = "limit") String limit,
            @RequestParam(name = "language") Language language

    )
    {
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return productService.listProduct(page, limit,language);
    }

    @GetMapping(value = "/list-product-android")
    public ResponseEntity<?> listAllProductAndroid(
            @RequestParam(name = "language") Language language
    )
    {
        return productService.listProductTypeList(language);
    }

    @GetMapping(value = "/list-rating")
    public ResponseEntity<?> listAllProductRating()
    {
        return productService.listAvgRatingProduct();
    }

    @GetMapping( value = "/variants/{id}")
    public ResponseEntity<?> viewProduct(@PathVariable Integer id){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("id", id);
        if (validation != null) return validation;
        return productService.getAllProductVariantFromProduct(id);
    }

    @GetMapping(value = "/search")
    public ResponseEntity<?> searchByCategoryName(@RequestParam(name = "keyword") String keyword,
                                                  @RequestParam(name = "page") String page,
                                                  @RequestParam(name = "limit") String limit,
                                                  @RequestParam(name = "language") Language language) {
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return productService.totalSearchProduct(keyword,page,limit,language);
    }

    @GetMapping(value = "/cate/search")
    public ResponseEntity<?> searchByCategoryName1(@RequestParam(name = "keyword") String keyword,
                                                  @RequestParam(name ="cateId") Integer cateId,
                                                  @RequestParam(name = "page") String page,
                                                  @RequestParam(name = "limit") String limit,
                                                   @RequestParam(name = "language") Language language) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("cateId", cateId);
        if (validation != null) return validation;
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return productService.totalSearchProductByCategory(keyword,cateId,page,limit,language);
    }

    @GetMapping(value = "/list-review")
    public ResponseEntity<?> listReview(@RequestParam(name = "proId") Integer proId,
                                        @RequestParam(name = "page") String page,
                                        @RequestParam(name = "limit") String limit) {
        ResponseEntity<?> validation = supportFunction.validatePositiveId("proId", proId);
        if (validation != null) return validation;
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return reviewService.getAllReview(page,limit,proId);
    }


    @GetMapping("/list-image/{proId}")
    public ResponseEntity<?> getListImage(@PathVariable Integer proId){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("proId", proId);
        if (validation != null) return validation;
        return productService.getAllProductImages(proId);
    }
    @GetMapping(value = "/list-with-avg-rating")
    public ResponseEntity<?> listProductsWithAverageRating(
            @RequestParam(name = "page") String pageFromParam,
            @RequestParam(name = "limit") String limitFromParam,
            @RequestParam(name = "language") Language language
    ) {
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(pageFromParam, limitFromParam);

        if (validationResult != null) {
            return validationResult;
        }
        return productService.listProductsWithAverageRating(pageFromParam, limitFromParam, language);
    }
    @PostMapping("/filter-product")
    public ResponseEntity<?> filterProduct(
           @RequestBody FilterProductBox req
            ) {

        return productService.filterProduct(req);
    }

    @DeleteMapping(value = "/image/deleteOne")
    public ResponseEntity<?> deleteAllItem(@RequestBody @Valid  DeleteProductImgReq req, HttpServletRequest httpRequest) {
        return productService.deleteImageFromProduct(req.getProId(), req.getId());
    }

    @DeleteMapping(value = "/image/deleteAll")
    public ResponseEntity<?> deleteOneItem(@RequestBody @Valid  DeleteAllProductImgReq req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());
        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return productService.deleteAllImageFromProduct(req.getProId());
    }

    @PutMapping(value = "/enable")
    public ResponseEntity<?> enableProduct(@RequestBody @Valid  IdReq req, HttpServletRequest httpRequest) {
        return  productService.enableProduct(req.getId());
    }

    @PutMapping(value = "/disable")
    public ResponseEntity<?> disableProduct(@RequestBody @Valid  IdReq req, HttpServletRequest httpRequest) {
        return  productService.disableProduct(req.getId());
    }

    @GetMapping(value = "/reset")
    public ResponseEntity<?> resetQuantityProduct() {
        return productService.resetAllQuantityProduct();
    }

//    @GetMapping(value = "/recommended/{userId}")
//    public ResponseEntity<?> getRecommendedBooksByUserId(@PathVariable   Long userId)
//            throws ResourceNotFoundException {
//        return recommender.recommendedBooks(userId,userRepository,productRepository);
//    }

    @GetMapping(value = "/recommend")
    public ResponseEntity<?> regenerate( @RequestParam(name = "language") Language language,HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        token = token.replaceFirst("Bearer ", "").trim();
        System.out.println(token);
        return productService.sendRecommendationRequest(token,4,language);
    }

}
