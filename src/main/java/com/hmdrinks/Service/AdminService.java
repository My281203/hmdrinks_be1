package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.*;
import com.hmdrinks.Exception.BadRequestException;
import com.hmdrinks.Repository.*;
import com.hmdrinks.RepositoryElasticsearch.ProductElasticsearchRepository;
import com.hmdrinks.RepositoryElasticsearch.ProductTranslationElasticsearchRepository;
import com.hmdrinks.Request.CreateAccountUserReq;
import com.hmdrinks.Request.FilterProductBox;
import com.hmdrinks.Request.UpdateAccountUserReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ProductVariantsRepository productVariantsRepository;
    @Autowired
    private  CategoryTranslationRepository categoryTranslationRepository;
    @Autowired
    private  ProductTranslationRepository productTranslationRepository;
    @Autowired
    private  PostTranSlationRepository postTranSlationRepository;
    @Autowired
    private  ShipperDetailRepository shipperDetailRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductElasticsearchRepository productElasticsearchRepository;
    @Autowired
    private ProductTranslationElasticsearchRepository productTranslationElasticsearchRepository;

    public ResponseEntity<?> createAccountUser(CreateAccountUserReq req){
        Optional<User> user = userRepository.findByUserNameAndIsDeletedFalse(req.getUserName());

        if (user.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User name already exists");
        }

        if (!supportFunction.checkRole(req.getRole().toString())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Role is wrong");
        }

        User userWithEmail = userRepository.findByEmail(req.getEmail());
        if (userWithEmail != null ) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }

        LocalDate currentDate = LocalDate.now();
        User user1 = new User();
        user1.setType(TypeLogin.BASIC);
        user1.setEmail(req.getEmail());
        user1.setRole(req.getRole());
        user1.setIsDeleted(false);
        user1.setUserName(req.getUserName());
        user1.setAvatar("");
        user1.setDistrict("");
        user1.setWard("");
        user1.setCity("");
        user1.setStreet("");
        user1.setSex(Sex.OTHER);
        user1.setDateCreated(Date.valueOf(currentDate));
        user1.setPhoneNumber("");
        user1.setPassword(passwordEncoder.encode(req.getPassword()));
        user1.setFullName(req.getFullName());
        userRepository.save(user1);
        if(user1.getRole() == Role.SHIPPER)
        {
            ShipperDetail shipperDetail = new ShipperDetail();
            shipperDetail.setUser(user1);
            shipperDetail.setOnLeave(false);
            shipperDetail.setTotalOrdersToday(0);
            shipperDetail.setStatus("available");
            shipperDetail.setIsReset(true);
            shipperDetail.setDateReset(LocalDate.now());
            shipperDetailRepository.save(shipperDetail);

        }
        Optional<User> userNewq = userRepository.findByUserNameAndIsDeletedFalse(req.getUserName());
        User userNew = userNewq.get();
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDAccountUserResponse(
                userNew.getUserId(),
                userNew.getUserName(),
                userNew.getFullName(),
                userNew.getAvatar(),
                userNew.getBirthDate(),
                "",
                userNew.getEmail(),
                userNew.getPhoneNumber(),
                userNew.getSex().toString(),
                userNew.getType().toString(),
                userNew.getIsDeleted(),
                userNew.getDateDeleted(),
                userNew.getDateUpdated(),
                userNew.getDateCreated(),
                userNew.getRole().toString()
        ));
    }
    @Transactional
    public ResponseEntity<?>  updateAccountUser(UpdateAccountUserReq req) {
        Optional<User> existingUserOptional = userRepository.findById(req.getUserId());
        if (existingUserOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");

        }
        User existingUser = existingUserOptional.get();
        if (req.getIsDeleted() != null && !req.getIsDeleted() && existingUser.getIsDeleted()) {
            existingUser.setIsDeleted(false);
        }

        if (req.getFullName() != null && !req.getFullName().isEmpty()) {
            existingUser.setFullName(req.getFullName());
        }

        if (req.getUserName() != null && !req.getUserName().isEmpty()) {
            Optional<User> userWithSameUserName = userRepository.findByUserNameAndIsDeletedFalse(req.getUserName());
            if (userWithSameUserName.isPresent() && userWithSameUserName.get().getUserId() != req.getUserId()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User name already exists");
            }
            existingUser.setUserName(req.getUserName());
        }

        if (req.getEmail() != null && !req.getEmail().isEmpty()) {
            User userWithEmail = userRepository.findByEmail(req.getEmail());
            if (userWithEmail != null && userWithEmail.getUserId() != req.getUserId()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
            }
            existingUser.setEmail(req.getEmail());
        }

        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        if (req.getRole() != null) {
            if (!supportFunction.checkRole(req.getRole().toString())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid role");
            }
            existingUser.setRole(req.getRole());
        }

        if (req.getPhoneNumber() != null && !req.getPhoneNumber().isEmpty()) {
            existingUser.setPhoneNumber(req.getPhoneNumber());
        }
        if (req.getIsDeleted() != null) {
            existingUser.setIsDeleted(req.getIsDeleted());
        }
        if (req.getDateUpdated() != null && !req.getDateUpdated().isEmpty()) {
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                java.util.Date utilDate = isoFormat.parse(req.getDateUpdated());

                // Chuyển sang java.sql.Date
                java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());

                existingUser.setDateUpdated(sqlDate);
            } catch (ParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid dateUpdated format");
            }
        }
        userRepository.save(existingUser);
        String fullLocation = existingUser.getStreet() + "," + existingUser.getWard() +
                existingUser.getDistrict() + ","+ existingUser.getCity();
        // Return updated user information as response
        return ResponseEntity.status(HttpStatus.OK).body( new CRUDAccountUserResponse(
                existingUser.getUserId(),
                existingUser.getUserName(),
                existingUser.getFullName(),
                existingUser.getAvatar(),
                existingUser.getBirthDate(),
                fullLocation,
                existingUser.getEmail(),
                existingUser.getPhoneNumber(),
                existingUser.getSex().toString(),
                existingUser.getType().toString(),
                existingUser.getIsDeleted(),
                existingUser.getDateDeleted(),
                existingUser.getDateUpdated(),
                existingUser.getDateCreated(),
                existingUser.getRole().toString()
        ));
    }
    public String deleteOneReview(int reviewId)
    {
        Review review = reviewRepository.findByReviewIdAndIsDeletedFalse(reviewId);
        if (review == null)
        {
            throw new BadRequestException("Review not found");
        }
        review.setIsDeleted(true);
        review.setDateDeleted(LocalDateTime.now());
        reviewRepository.save(review);
        return "Review deleted";
    }

    public String deleteALlReviewProduct(int proId){
        List<Review> reviewList = reviewRepository.findByProduct_ProIdAndIsDeletedFalse(proId);
        if(reviewList != null)
        {
            for(Review review : reviewList){
                review.setIsDeleted(true);
                review.setDateDeleted(LocalDateTime.now());
                reviewRepository.save(review);
            }
        }

        return "All review product deleted";
    }

    public ResponseEntity<?> getAllProductImages(int proId) {
        List<ProductImageResponse> productImageResponses = new ArrayList<>();
        Product product = productRepository.findByProId(proId);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found product with ID: " + proId);
        }
        String currentProImg = product.getListProImg();
        int total = 0;
        if(currentProImg != null && !currentProImg.trim().isEmpty())
        {
            String[] imageEntries = currentProImg.split(", ");
            for (String imageEntry : imageEntries) {
                String[] parts = imageEntry.split(": ");
                int stt = Integer.parseInt(parts[0]);
                String url = parts[1];
                productImageResponses.add(new ProductImageResponse(stt, url));
                total++;
            }
        }
        return  ResponseEntity.status(HttpStatus.OK).body(new ListProductImageResponse(proId,total,productImageResponses));
    }


    public static Page<Product> mapToProductPage(Page<ProductElasticsearch> elasticsearchPage, Pageable pageable) {
        List<Product> products = elasticsearchPage.getContent().stream().map(elastic -> {
            Product product = new Product();
            Category category = new Category();
            category.setCateId(elastic.getCategoryId());
            product.setCategory(category);
            product.setProId(Integer.parseInt(elastic.getId()));
            product.setProName(elastic.getProName());
            product.setListProImg(elastic.getListProImg());
            product.setDescription(elastic.getDescription());
            product.setIsDeleted(elastic.getIsDeleted());
            product.setDateDeleted(elastic.getDateDeleted());
            product.setDateUpdated(elastic.getDateUpdated());
            product.setDateCreated(elastic.getDateCreated());
            return product;
        }).collect(Collectors.toList());

        return new PageImpl<>(products, pageable, elasticsearchPage.getTotalElements());
    }

    public static Page<ProductTranslation> mapToProductTranslationPage(Page<ProductTranslationElasticsearch> elasticsearchPage, Pageable pageable) {
        List<ProductTranslation> products = elasticsearchPage.getContent().stream().map(elastic -> {
            ProductTranslation product = new ProductTranslation();
            Product product1 = new Product();
            product1.setProId(elastic.getProId());
            product.setProduct(product1);
            product.setProName(elastic.getProName());
            product.setProTransId(Integer.parseInt(elastic.getId()));
            product.setLanguage(elastic.getLanguage());
            product.setDescription(elastic.getDescription());
            product.setIsDeleted(elastic.getIsDeleted());
            product.setDateDeleted(elastic.getDateDeleted());
            product.setDateUpdated(elastic.getDateUpdated());
            product.setDateCreated(elastic.getDateCreated());
            return product;
        }).collect(Collectors.toList());

        return new PageImpl<>(products, pageable, elasticsearchPage.getTotalElements());
    }

    @Transactional
    public ResponseEntity<?> totalSearchProduct(String keyword, String pageFromParam, String limitFromParam,Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;

        Pageable pageable = PageRequest.of(page - 1, limit);

        if(language == Language.VN)
        {
            // Lấy danh sách sản phẩm và tổng số phần tử
            Page<ProductElasticsearch> productElasticsearches = productElasticsearchRepository.searchByProNameAndIsDeletedFalse(keyword,pageable);
            Page<Product> productList = mapToProductPage(productElasticsearches, pageable);
            int total = (int) productList.getTotalElements(); // Đếm tổng số sản phẩm

            List<CRUDProductResponse> crudProductResponseList = new ArrayList<>();
            for (Product product1 : productList) {

                List<ProductImageResponse> productImageResponses = new ArrayList<>();
                String currentProImg = product1.getListProImg();

                if (currentProImg != null && !currentProImg.trim().isEmpty()) {
                    String[] imageEntries1 = currentProImg.split(", ");

                    for (String imageEntry : imageEntries1) {
                        String[] parts = imageEntry.split(": ");

                        // Kiểm tra xem mảng có đúng 2 phần tử không
                        if (parts.length == 2) {
                            try {
                                int stt = Integer.parseInt(parts[0].trim());
                                String url = parts[1].trim();
                                productImageResponses.add(new ProductImageResponse(stt, url));
                            } catch (NumberFormatException e) {
                                System.err.println("Lỗi chuyển đổi số: " + parts[0]);
                            }
                        } else {
                            System.err.println("Chuỗi không hợp lệ: " + imageEntry);
                        }
                    }
                }


                List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(productVariantsRepository.findByProduct_ProId(product1.getProId()))
                        .orElse(Collections.emptyList()) // Trả về danh sách rỗng nếu là null
                        .stream()
                        .map(variant -> new CRUDProductVarResponse(
                                variant.getVarId(),
                                variant.getProduct().getProId(),
                                variant.getSize(),
                                variant.getPrice(),
                                variant.getStock(),
                                variant.getIsDeleted(),
                                variant.getDateDeleted(),
                                variant.getDateCreated(),
                                variant.getDateUpdated()
                        ))
                        .toList();

                crudProductResponseList.add(new CRUDProductResponse(
                        product1.getProId(),
                        product1.getCategory().getCateId(),
                        product1.getProName(),
                        productImageResponses,
                        product1.getDescription(),
                        product1.getIsDeleted(),
                        product1.getDateDeleted(),
                        product1.getDateCreated(),
                        product1.getDateUpdated(),
                        variantResponses
                ));
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new TotalSearchProductResponse(page, productList.getTotalPages(), limit, total, crudProductResponseList));
        }
        else {
            // Lấy danh sách sản phẩm và tổng số phần tử


            Page<ProductTranslationElasticsearch> productElasticsearches = productTranslationElasticsearchRepository.searchByProName(keyword,keyword,pageable);
            Page<ProductTranslation> productList = mapToProductTranslationPage(productElasticsearches, pageable);
            int total = (int) productList.getTotalElements(); // Đếm tổng số sản phẩm

            List<CRUDProductResponse> crudProductResponseList = new ArrayList<>();
            for (ProductTranslation product1 : productList) {
                List<ProductImageResponse> productImageResponses = new ArrayList<>();
                Product product_original = productRepository.findByProId(product1.getProduct().getProId());

                String currentProImg = product_original.getListProImg();
                if (currentProImg != null && !currentProImg.trim().isEmpty()) {
                    String[] imageEntries1 = currentProImg.split(", ");
                    for (String imageEntry : imageEntries1) {
                        String[] parts = imageEntry.split(": ");
                        int stt = Integer.parseInt(parts[0]);
                        String url = parts[1];
                        productImageResponses.add(new ProductImageResponse(stt, url));
                    }
                }

                List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(productVariantsRepository.findByProduct_ProId(product_original.getProId()))
                        .orElse(Collections.emptyList()) // Trả về danh sách rỗng nếu là null
                        .stream()
                        .map(variant -> new CRUDProductVarResponse(
                                variant.getVarId(),
                                variant.getProduct().getProId(),
                                variant.getSize(),
                                variant.getPrice(),
                                variant.getStock(),
                                variant.getIsDeleted(),
                                variant.getDateDeleted(),
                                variant.getDateCreated(),
                                variant.getDateUpdated()
                        ))
                        .toList();

                crudProductResponseList.add(new CRUDProductResponse(
                        product_original.getProId(),
                        (product_original.getCategory() != null) ? product_original.getCategory().getCateId() : null,
                        product1.getProName(),
                        productImageResponses,
                        product1.getDescription(),
                        (product_original.getIsDeleted() != null) ? product_original.getIsDeleted() : null,
                        product_original.getDateDeleted(),
                        product_original.getDateCreated(),
                        product_original.getDateUpdated(),
                        variantResponses
                ));
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new TotalSearchProductResponse(page, productList.getTotalPages(), limit, total, crudProductResponseList));
        }
    }


    public ResponseEntity<?> getAllProductVariantFromProduct(int id) {
        Product product = productRepository.findByProId(id);
        if (product == null) {
            throw new BadRequestException("proId not exists");
        }
        List<ProductVariants> productList = productVariantsRepository.findByProduct_ProId(id);
        List<CRUDProductVarResponse> crudProductVarResponseList = new ArrayList<>();
        for (ProductVariants product1 : productList) {
            crudProductVarResponseList.add(new CRUDProductVarResponse(
                    product1.getVarId(),
                    product1.getProduct().getProId(),
                    product1.getSize(),
                    product1.getPrice(),
                    product1.getStock(),
                    product1.getIsDeleted(),
                    product1.getDateDeleted(),
                    product1.getDateCreated(),
                    product1.getDateUpdated()
            ));
        }
        return ResponseEntity.ok(new GetProductVariantFromProductIdResponse(
                id,
                crudProductVarResponseList
        ));



    }

    public ResponseEntity<?> filterProduct(FilterProductBox req) {
//        List<CRUDProductVarFilterResponse> crudProductVarFilterResponseList = new ArrayList<>();
//        Category category = categoryRepository.findByCateId(req.getC());
//        if (category == null) {
//            throw new BadRequestException("cateId not exists");
//        }
//        for (Integer id : req.getP()) {
//            Product product = productRepository.findByProId(id);
//            if (product == null) {
//                throw new BadRequestException("productId not exists");
//            }
//        }
//
//        if (req.getO() <= 0) {
//            throw new BadRequestException("o must be greater than 0");
//        }
//
//        Sort sort;
//        int total = 0;
//        if (req.getO() == 1) {
//            sort = Sort.by(Sort.Direction.DESC, "price");
//            List<ProductVariants> productVariants = productVariantsRepository.findByProduct_Category_CateIdAndProduct_ProIdIn(req.getC(), req.getP(), sort);
//            for (ProductVariants productVariant : productVariants) {
//                Double avgRating = productRepository.findAverageRatingByProductIdAdmin(req.getC(),productVariant.getProduct().getProId());
//                if(avgRating == null) {
//                    avgRating = 0.0;
//                }
//                crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
//                        Math.round(avgRating * 10) / 10.0,
//                        productVariant.getVarId(),
//                        productVariant.getProduct().getProId(),
//                        productVariant.getSize(),
//                        productVariant.getPrice(),
//                        productVariant.getStock(),
//                        productVariant.getIsDeleted(),
//                        productVariant.getDateDeleted(),
//                        productVariant.getDateCreated(),
//                        productVariant.getDateUpdated()
//                ));
//                total += 1;
//            }
//        } else if (req.getO() == 2) {
//            sort = Sort.by(Sort.Direction.ASC, "price");
//            List<ProductVariants> productVariants = productVariantsRepository.findByProduct_Category_CateIdAndProduct_ProIdIn(req.getC(), req.getP(), sort);
//            for (ProductVariants productVariant : productVariants) {
//                Double avgRating = productRepository.findAverageRatingByProductIdAdmin(req.getC(),productVariant.getProduct().getProId());
//                if(avgRating == null) {
//                    avgRating = 0.0;
//                }
//                crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
//                        Math.round(avgRating * 10) / 10.0,
//                        productVariant.getVarId(),
//                        productVariant.getProduct().getProId(),
//                        productVariant.getSize(),
//                        productVariant.getPrice(),
//                        productVariant.getStock(),
//                        productVariant.getIsDeleted(),
//                        productVariant.getDateDeleted(),
//                        productVariant.getDateCreated(),
//                        productVariant.getDateUpdated()
//                ));
//                total += 1;
//            }
//        } else if (req.getO() == 3) {
//            List<ProductVariants> productVariants = productVariantsRepository
//                    .findByProduct_Category_CateIdAndProduct_ProIdIn(
//                            req.getC(),
//                            req.getP(),
//                            Sort.by(Sort.Direction.DESC, "dateCreated")
//                    );
//
//            for (ProductVariants productVariant : productVariants) {
//                Double avgRating = productRepository.findAverageRatingByProductIdAdmin(req.getC(),productVariant.getProduct().getProId());
//                if(avgRating == null) {
//                    avgRating = 0.0;
//                }
//                crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
//                        Math.round(avgRating * 10) / 10.0,
//                        productVariant.getVarId(),
//                        productVariant.getProduct().getProId(),
//                        productVariant.getSize(),
//                        productVariant.getPrice(),
//                        productVariant.getStock(),
//                        productVariant.getIsDeleted(),
//                        productVariant.getDateDeleted(),
//                        productVariant.getDateCreated(),
//                        productVariant.getDateUpdated()
//                ));
//                total += 1;
//            }
//        } else if (req.getO() == 4) {
//            List<Product> product = productRepository.findTopRatedProductsDescByAdmin(req.getC(), req.getP());
//            for (Product product1 : product) {
//                List<ProductVariants> productVariants = productVariantsRepository.findByProduct_ProId(product1.getProId());
//                Double  avgRating = productRepository.findAverageRatingByProductIdAdmin(req.getC(),product1.getProId());
//                for (ProductVariants productVariant : productVariants) {
//                    if(avgRating == null) {
//                        avgRating = 0.0;
//                    }
//                    crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
//                            Math.round(avgRating * 10) / 10.0,
//                            productVariant.getVarId(),
//                            productVariant.getProduct().getProId(),
//                            productVariant.getSize(),
//                            productVariant.getPrice(),
//                            productVariant.getStock(),
//                            productVariant.getIsDeleted(),
//                            productVariant.getDateDeleted(),
//                            productVariant.getDateCreated(),
//                            productVariant.getDateUpdated()
//                    ));
//                    total += 1;
//                }
//
//            }
//        } else if (req.getO() == 5) {
//            List<Product> product = productRepository.findTopRatedProductsAscByAdmin(req.getC(), req.getP());
//            for (Product product1 : product) {
//                List<ProductVariants> productVariants = productVariantsRepository.findByProduct_ProId(product1.getProId());
//                for (ProductVariants productVariant : productVariants) {
//                    Double avgRating = productRepository.findAverageRatingByProductIdAdmin(req.getC(),product1.getProId());
//                    if(avgRating == null) {
//                        avgRating = 0.0;
//                    }
//                    crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
//                            Math.round(avgRating * 10) / 10.0,
//                            productVariant.getVarId(),
//                            productVariant.getProduct().getProId(),
//                            productVariant.getSize(),
//                            productVariant.getPrice(),
//                            productVariant.getStock(),
//                            productVariant.getIsDeleted(),
//                            productVariant.getDateDeleted(),
//                            productVariant.getDateCreated(),
//                            productVariant.getDateUpdated()
//                    ));
//                    total += 1;
//                }
//            }
//        }

        return ResponseEntity.ok(new FilterProductBoxResponse());


    }






    public List<ProductImageResponse> parseProductImages(String listProImg) {
        List<ProductImageResponse> productImageResponses = new ArrayList<>();
        if (listProImg != null && !listProImg.trim().isEmpty()) {
            // Giả sử format của listProImg là: "1: url1, 2: url2, 3: url3"
            String[] imageEntries = listProImg.split(", ");
            for (String imageEntry : imageEntries) {
                String[] parts = imageEntry.split(": ");
                if (parts.length == 2) {
                    try {
                        int stt = Integer.parseInt(parts[0]);
                        String url = parts[1];
                        productImageResponses.add(new ProductImageResponse(stt, url));
                    } catch (NumberFormatException e) {
                        // Bỏ qua hoặc log lỗi
                    }
                }
            }
        }
        return productImageResponses;
    }


    @Transactional
    public ResponseEntity<?> listProduct(String pageFromParam, String limitFromParam, Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);



        int offset = (page - 1) * limit;

        List<Product> products1= productRepository.findAllNotDeletedNativeManual(limit, offset);
        long total = productRepository.countAllNotDeleted();

        Page<Product> productPage = new PageImpl<>(products1, PageRequest.of(page - 1, limit), total);
        List<Product> products = productPage.getContent();

        List<Integer> productIds = products.stream().map(Product::getProId).toList();


        // Batch fetch variants
        List<ProductVariants> allVariants = productVariantsRepository.findByProduct_ProIdInAndIsDeletedFalse(productIds);

        // Batch fetch translations (only EN)
        Map<Long, ProductTranslation> translationMap = new HashMap<>();
        if (language == Language.EN) {
            List<ProductTranslation> translations = productTranslationRepository.findByProduct_ProIdInAndIsDeletedFalse(productIds);
            for (ProductTranslation t : translations) {
                translationMap.put((long) t.getProduct().getProId(), t);
            }
        }

        List<CRUDProductResponse> crudProductResponseList = new ArrayList<>();

        for (Product product : products) {
            // Parse images
            List<ProductImageResponse> imageResponses = new ArrayList<>();
            String imgRaw = product.getListProImg();
            if (imgRaw != null && !imgRaw.isBlank()) {
                String[] imageEntries = imgRaw.split(", ");
                for (String entry : imageEntries) {
                    String[] parts = entry.split(": ");
                    if (parts.length == 2) {
                        imageResponses.add(new ProductImageResponse(Integer.parseInt(parts[0]), parts[1]));
                    }
                }
            }

            // Filter variants
            List<CRUDProductVarResponse> variantResponses = allVariants.stream()
                    .filter(v -> v.getProduct().getProId() == product.getProId())
                    .map(variant -> new CRUDProductVarResponse(
                            variant.getVarId(),
                            product.getProId(),
                            variant.getSize(),
                            variant.getPrice(),
                            variant.getStock(),
                            variant.getIsDeleted(),
                            variant.getDateDeleted(),
                            variant.getDateCreated(),
                            variant.getDateUpdated()
                    ))
                    .toList();

            String nameTrans = product.getProName();
            String desTrans = product.getDescription();

            if (language == Language.EN) {
                ProductTranslation trans = translationMap.get(product.getProId());
                if (trans != null) {
                    nameTrans = trans.getProName();
                    desTrans = trans.getDescription();
                } else {
                    nameTrans = supportFunction.convertLanguage(product.getProName(), language);
                    desTrans = supportFunction.convertLanguage(product.getDescription(), language);

                    ProductTranslation newTrans = new ProductTranslation();
                    newTrans.setProName(nameTrans);
                    newTrans.setDescription(desTrans);
                    newTrans.setIsDeleted(false);
                    newTrans.setDateCreated(LocalDateTime.now());
                    newTrans.setProduct(product);
                    productTranslationRepository.save(newTrans);
                }
            }

            CRUDProductResponse response = new CRUDProductResponse(
                    product.getProId(),
                    product.getCategory().getCateId(),
                    nameTrans,
                    imageResponses,
                    desTrans,
                    product.getIsDeleted(),
                    product.getDateDeleted(),
                    product.getDateCreated(),
                    product.getDateUpdated(),
                    variantResponses
            );
            crudProductResponseList.add(response);
        }

        return ResponseEntity.ok(
                new ListProductResponse(
                        page,
                        productPage.getTotalPages(),
                        limit,
                        (int) productPage.getTotalElements(),
                        crudProductResponseList
                )
        );
    }


    @Transactional
    public ResponseEntity<?> getOneProduct(Integer id,Language language) {
        Product product1 = productRepository.findByProId(id);
        if (product1 == null) {
            throw new BadRequestException("production id not exists");
        }
        List<ProductImageResponse> productImageResponses = new ArrayList<>();
        String currentProImg = product1.getListProImg();
        if(currentProImg != null && !currentProImg.trim().isEmpty())
        {
            String[] imageEntries1 = currentProImg.split(", ");
            for (String imageEntry : imageEntries1) {
                String[] parts = imageEntry.split(": ");
                int stt = Integer.parseInt(parts[0]);
                String url = parts[1];
                productImageResponses.add(new ProductImageResponse(stt, url));
            }
        }
        List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(product1.getProductVariants())
                .orElse(Collections.emptyList()) // Trả về danh sách rỗng nếu là null
                .stream()
                .map(variant -> new CRUDProductVarResponse(
                        variant.getVarId(),
                        variant.getProduct().getProId(),
                        variant.getSize(),
                        variant.getPrice(),
                        variant.getStock(),
                        variant.getIsDeleted(),
                        variant.getDateDeleted(),
                        variant.getDateCreated(),
                        variant.getDateUpdated()
                ))
                .toList();
        String name_product_trans = "";
        String des = "";
        if(language == Language.EN)
        {
            ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProIdAndIsDeletedFalse(product1.getProId());
            if(productTranslation != null)
            {
                name_product_trans = productTranslation.getProName();
                des = productTranslation.getDescription();
            }
            else {
                name_product_trans = product1.getProName();
                des = product1.getDescription();
            }
            if (Objects.equals(name_product_trans, "") || Objects.equals(des, "")
            ) {
                name_product_trans = supportFunction.convertLanguage(product1.getProName(),Language.EN);
                des = supportFunction.convertLanguage(product1.getDescription(),Language.EN);
                ProductTranslation productTranslation1 = new ProductTranslation();
                productTranslation1.setProName(name_product_trans);
                productTranslation1.setDescription(des);
                productTranslation1.setIsDeleted(Boolean.FALSE);
                productTranslation1.setDateCreated(LocalDateTime.now());
                productTranslation1.setProduct(product1);
                productTranslationRepository.save(productTranslation1);
            }
        }
        else {
            name_product_trans = product1.getProName();
            des = product1.getDescription();
        }
        return ResponseEntity.ok(new CRUDProductResponse(
                        product1.getProId(),
                        product1.getCategory().getCateId(),
                        name_product_trans,
                        productImageResponses,
                        des,
                        product1.getIsDeleted(),
                        product1.getDateDeleted(),
                        product1.getDateCreated(),
                        product1.getDateUpdated(),
                        variantResponses

        ));


    }

    @Transactional
    public ResponseEntity<?> getAllProductFromCategory(int id,String pageFromParam, String limitFromParam,Language language)
    {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        Category category = categoryRepository.findByCateId(id);
        if(category == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("cateId not exists");
        }
        Page<Product> productList = productRepository.findByCategory_CateId(id,pageable);
        List<Product> productList1 = productRepository.findByCategory_CateId(id);
        List<CRUDProductResponse> crudProductResponseList = new ArrayList<>();
        int total =0 ;
        for(Product product1: productList)
        {
            List<ProductImageResponse> productImageResponses = new ArrayList<>();
            String currentProImg = product1.getListProImg();
            if(currentProImg != null && !currentProImg.trim().isEmpty())
            {
                String[] imageEntries1 = currentProImg.split(", ");
                for (String imageEntry : imageEntries1) {
                    String[] parts = imageEntry.split(": ");
                    int stt = Integer.parseInt(parts[0]);
                    String url = parts[1];
                    productImageResponses.add(new ProductImageResponse(stt, url));
                }
            }
            List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(product1.getProductVariants())
                    .orElse(Collections.emptyList()) // Trả về danh sách rỗng nếu là null
                    .stream()
                    .map(variant -> new CRUDProductVarResponse(
                            variant.getVarId(),
                            variant.getProduct().getProId(),
                            variant.getSize(),
                            variant.getPrice(),
                            variant.getStock(),
                            variant.getIsDeleted(),
                            variant.getDateDeleted(),
                            variant.getDateCreated(),
                            variant.getDateUpdated()
                    ))
                    .toList();
            String name_trans_pro = "";
            String des_pro = "";
            if(language == Language.VN)
            {
                name_trans_pro = product1.getProName();
                des_pro = product1.getDescription();

            }
            else if (language == Language.EN)
            {
                ProductTranslation productTranslations = productTranslationRepository.findByProduct_ProId(product1.getProId());
                if(productTranslations != null) {
                    name_trans_pro = productTranslations.getProName();
                    des_pro = productTranslations.getDescription();
                }

            }
            if(name_trans_pro.equals(""))
            {
                name_trans_pro = supportFunction.convertLanguage(product1.getProName(),language);
                ProductTranslation productTranslation = new ProductTranslation();
                productTranslation.setProName(name_trans_pro);
                productTranslation.setProduct(product1);
                productTranslation.setDateCreated(LocalDateTime.now());
                productTranslation.setIsDeleted(false);
                productTranslation.setDescription(supportFunction.convertLanguage(product1.getDescription(),Language.EN));
                productTranslationRepository.save(productTranslation);

                des_pro = productTranslation.getDescription();
            }
            crudProductResponseList.add(new CRUDProductResponse(
                    product1.getProId(),
                    product1.getCategory().getCateId(),
                    name_trans_pro,
                    productImageResponses,
                    des_pro,
                    product1.getIsDeleted(),
                    product1.getDateDeleted(),
                    product1.getDateCreated(),
                    product1.getDateUpdated(),
                    variantResponses
            ));
            total++;
        }

        return ResponseEntity.status(HttpStatus.OK).body(new GetViewProductCategoryResponse(
                page,
                productList.getTotalPages(),
                limit,
                productList1.size(),
                crudProductResponseList
        ));
    }

    @Transactional
    public ResponseEntity<?> getAllPostByType(String pageFromParam, String limitFromParam, Type_Post typePost,Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Post> posts = postRepository.findAllByType(typePost,pageable);
        List<Post> posts1 = postRepository.findAllByType(typePost);
        List<CRUDPostAndVoucherResponse> responses = new ArrayList<>();
        for(Post post : posts) {
            Voucher voucher = post.getVoucher();
            String title = "";
            String description = "";
            String shortDes = "";

            if (language == Language.EN) {
                PostTranslation postTranslation = postTranSlationRepository.findByPost_PostId(post.getPostId());
                if(postTranslation != null)
                {
                    title = postTranslation.getTitle();
                    description = postTranslation.getDescription();
                    shortDes = postTranslation.getShortDes();
                }
            } else if (language == Language.VN) {
                title = post.getTitle();
                description = post.getDescription();
                shortDes = post.getShortDes();
            }
            if (Objects.equals(title, "") || Objects.equals(description, "") || Objects.equals(shortDes, "")) {

                PostTranslation postTranslation1 = new PostTranslation();
                postTranslation1.setPost(post);
                postTranslation1.setLanguage(Language.EN);
                postTranslation1.setDescription(supportFunction.convertLanguage(post.getDescription(), Language.EN));
                postTranslation1.setShortDes(supportFunction.convertLanguage(post.getShortDes(), Language.EN));
                postTranslation1.setTitle(supportFunction.convertLanguage(post.getTitle(), Language.EN));
                postTranslation1.setDateCreate(LocalDateTime.now());
                postTranslation1.setIsDeleted(Boolean.FALSE);
                postTranSlationRepository.save(postTranslation1);

                title = postTranslation1.getTitle();
                description = postTranslation1.getDescription();
                shortDes = postTranslation1.getShortDes();

            }
            responses.add(new CRUDPostAndVoucherResponse(
                    post.getPostId(),
                    post.getType(),
                    post.getBannerUrl(),
                    description,
                    title,
                    shortDes,
                    post.getUser().getUserId(),
                    post.getIsDeleted(),
                    post.getDateDeleted(),
                    post.getDateCreate(),
                    new CRUDVoucherResponse(
                            voucher.getVoucherId(),
                            voucher.getKey(),
                            voucher.getNumber(),
                            voucher.getStartDate(),
                            voucher.getEndDate(),
                            voucher.getDiscount(),
                            voucher.getStatus(),
                            voucher.getPost().getPostId()
                    )
            ));
        }
        return ResponseEntity.ok(new ListAllPostResponse(
                page,
                posts.getTotalPages(),
                limit,
                posts1.size(),
                responses
        ));


    }

    @Transactional
    public ResponseEntity<?> getAllPost(String pageFromParam, String limitFromParam,Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Post> posts = postRepository.findAll(pageable);
        List<Post> posts1 = postRepository.findAllByIsDeletedFalse();

        List<CRUDPostAndVoucherResponse> responses = new ArrayList<>();
        int total = 0;
        for(Post post : posts) {
            Voucher voucher = post.getVoucher();
            String title = "";
            String description = "";
            String shortDes = "";
            if (language == Language.EN) {
                PostTranslation postTranslation = postTranSlationRepository.findByPost_PostId(post.getPostId());
                if(postTranslation != null)
                {
                    title = postTranslation.getTitle();
                    description = postTranslation.getDescription();
                    shortDes = postTranslation.getShortDes();
                }

            } else if (language == Language.VN) {
                title = post.getTitle();
                description = post.getDescription();
                shortDes = post.getShortDes();
            }
            if (Objects.equals(title, "") || Objects.equals(description, "") || Objects.equals(shortDes, "")) {

                PostTranslation postTranslation1 = new PostTranslation();
                postTranslation1.setPost(post);
                postTranslation1.setLanguage(Language.EN);
                postTranslation1.setDescription(supportFunction.convertLanguage(post.getDescription(), Language.EN));
                postTranslation1.setShortDes(supportFunction.convertLanguage(post.getShortDes(), Language.EN));
                postTranslation1.setTitle(supportFunction.convertLanguage(post.getTitle(), Language.EN));
                postTranslation1.setDateCreate(LocalDateTime.now());
                postTranslation1.setIsDeleted(Boolean.FALSE);
                postTranSlationRepository.save(postTranslation1);

                title = postTranslation1.getTitle();
                description = postTranslation1.getDescription();
                shortDes = postTranslation1.getShortDes();

            }
            responses.add(new CRUDPostAndVoucherResponse(
                    post.getPostId(),
                    post.getType(),
                    post.getBannerUrl(),
                    description,
                    title,
                    shortDes,
                    post.getUser().getUserId(),
                    post.getIsDeleted(),
                    post.getDateDeleted(),
                    post.getDateCreate(),
                    new CRUDVoucherResponse(
                            voucher.getVoucherId(),
                            voucher.getKey(),
                            voucher.getNumber(),
                            voucher.getStartDate(),
                            voucher.getEndDate(),
                            voucher.getDiscount(),
                            voucher.getStatus(),
                            voucher.getPost().getPostId()
                    )
            ));
            total++;
        }
        return ResponseEntity.ok(new ListAllPostResponse(
                page,
                posts.getTotalPages(),
                limit,
                posts1.size(),
                responses
        ));
    }

    public ResponseEntity<?> listCategory(String pageFromParam, String limitFromParam, Language language)
    {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Category> categoryList = categoryRepository.findAll(pageable);
        List<Category> categoryList1 = categoryRepository.findAllByIsDeletedFalse();
        List<CRUDCategoryResponse> crudCategoryResponseList = new ArrayList<>();
        for(Category category: categoryList){
            String name_trans = "";
            if(language == Language.VN)
            {
                name_trans = category.getCateName();
            }
            else if (language == Language.EN)
            {
                CategoryTranslation categoryTranslation = categoryTranslationRepository.findByCategory_CateId(category.getCateId());
                if(categoryTranslation != null) {
                    name_trans = categoryTranslation.getCateName();
                }

            }
            if(name_trans.equals(""))
            {
                String name_trans1 = supportFunction.convertLanguage(category.getCateName(),Language.EN);
                CategoryTranslation categoryTranslation1 = new CategoryTranslation();
                categoryTranslation1.setCategory(category);
                categoryTranslation1.setLanguage(Language.EN);
                categoryTranslation1.setDateCreated(LocalDateTime.now());
                categoryTranslation1.setIsDeleted(false);
                categoryTranslation1.setCateName(name_trans1);
                categoryTranslationRepository.save(categoryTranslation1);
                name_trans = name_trans1;
            }
            crudCategoryResponseList.add(new CRUDCategoryResponse(
                    category.getCateId(),
                    name_trans,
                    category.getCateImg(),
                    category.getIsDeleted(),
                    category.getDateCreated(),
                    category.getDateUpdated(),
                    category.getDateDeleted()
            ));
        }
        return ResponseEntity.ok(new ListCategoryResponse(page,categoryList.getTotalPages(),limit,categoryList1.size(),crudCategoryResponseList));

    }
}
