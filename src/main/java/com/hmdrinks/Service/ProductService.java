package com.hmdrinks.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Size;
import com.hmdrinks.Repository.*;
import com.hmdrinks.RepositoryElasticsearch.ProductElasticsearchRepository;
import com.hmdrinks.RepositoryElasticsearch.ProductTranslationElasticsearchRepository;
import com.hmdrinks.Request.*;
import com.hmdrinks.Response.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductVariantsRepository productVariantsRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private FavouriteItemRepository favouriteItemRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private ProductTranslationRepository productTranslationRepository;
    @Autowired
    private  CartItemGroupRepository cartItemGroupRepository;
    @Autowired
    private  CartGroupRepository cartGroupRepository;
    @Autowired
    private  GroupOrderMembersRepository groupOrderMembersRepository;
    @Autowired
    private GroupOrdersRepository groupOrdersRepository;

    @Autowired
    private ProductElasticsearchRepository productElasticsearchRepository;
    @Autowired
    private ProductTranslationElasticsearchRepository productTranslationElasticsearchRepository;

    @Value("${api.user-service.url}")
    private String userServiceUrl;

    @Value("${api.group-order.url}")
    private String groupOrderUrl;




    private ProductElasticsearch convertToElasticsearch(Product product) {
        return new ProductElasticsearch(
                String.valueOf(product.getProId()),
                product.getProName(),
                product.getDescription(),
                product.getListProImg(),
                product.getIsDeleted(),
                product.getDateCreated(),
                product.getDateUpdated(),
                product.getDateDeleted(),
                product.getCategory().getCateId()
        );
    }

    private ProductTranslationElasticsearch convertToElasticsearchTranslation(ProductTranslation translation) {
        return new ProductTranslationElasticsearch(
                String.valueOf(translation.getProTransId()),
                translation.getProName(),
                translation.getProduct().getProId(),
                translation.getDescription(),
                translation.getIsDeleted(),
                translation.getDateCreated(),
                translation.getDateUpdated(),
                translation.getDateDeleted(),
                translation.getLanguage()
        );
    }

    @Transactional
    public ResponseEntity<?> crateProduct(CreateProductReq req) {
        String result_proname = "" ;
        String result_des = "";
        if(req.getLanguage() == Language.VN)
        {
            Category category = categoryRepository.findByCateIdAndIsDeletedFalse(req.getCateId());
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("cateId not exists");
            }
            Product product = productRepository.findByProNameAndIsDeletedFalse(req.getProName());
            if (product != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("product already exists");
            }

            Product product1 = new Product();
            product1.setCategory(category);
            product1.setDescription(supportFunction.convertLanguage(req.getDescription(),Language.VN));
            product1.setProName(supportFunction.convertLanguage(req.getProName(),Language.VN));
            product1.setListProImg("");
            product1.setIsDeleted(false);
            product1.setDateCreated(LocalDateTime.now());
            productRepository.save(product1);
            productElasticsearchRepository.save(convertToElasticsearch(product1));

            ProductTranslation productTranslation = new ProductTranslation();
            productTranslation.setProName(supportFunction.convertLanguage(req.getProName(),Language.EN));
            productTranslation.setProduct(product);
            productTranslation.setIsDeleted(Boolean.FALSE);
            productTranslation.setDateCreated(LocalDateTime.now());
            productTranslation.setProduct(product1);
            productTranslation.setLanguage(Language.EN);
            productTranslation.setDescription(supportFunction.convertLanguage(req.getDescription(),Language.EN));
            productTranslationRepository.save(productTranslation);
            productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation));

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
                    .orElse(Collections.emptyList())
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

            return ResponseEntity.status(HttpStatus.OK).body(new CRUDProductResponse(
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
        if(req.getLanguage() == Language.EN)
        {
            Category category = categoryRepository.findByCateIdAndIsDeletedFalse(req.getCateId());
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("cateId not exists");
            }
            String product_name_trans = supportFunction.convertLanguage(req.getProName(),Language.VN);
            ProductTranslation product_trans = productTranslationRepository.findByProName(req.getProName());
            if (product_trans != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("product already exists");
            }

            Product product1 = new Product();
            product1.setCategory(category);
            product1.setDescription(supportFunction.convertLanguage(req.getDescription(),Language.VN));
            product1.setProName(supportFunction.convertLanguage(req.getProName(),Language.VN));
            product1.setListProImg("");
            product1.setIsDeleted(false);
            product1.setDateCreated(LocalDateTime.now());
            productRepository.save(product1);
            productElasticsearchRepository.save(convertToElasticsearch(product1));

            ProductTranslation productTranslation = new ProductTranslation();
            productTranslation.setProName(supportFunction.convertLanguage(req.getProName(),Language.EN));
            productTranslation.setProduct(product1);
            productTranslation.setIsDeleted(Boolean.FALSE);
            productTranslation.setDateCreated(LocalDateTime.now());
            productTranslation.setLanguage(Language.EN);
            productTranslation.setDescription(supportFunction.convertLanguage(req.getDescription(),Language.EN));
            productTranslationRepository.save(productTranslation);
            productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation));

            result_proname = productTranslation.getProName();
            result_des = productTranslation.getDescription();
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
                    .orElse(Collections.emptyList())
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

            return ResponseEntity.status(HttpStatus.OK).body(new CRUDProductResponse(
                    product1.getProId(),
                    product1.getCategory().getCateId(),
                    result_proname,
                    productImageResponses,
                    result_des,
                    product1.getIsDeleted(),
                    product1.getDateDeleted(),
                    product1.getDateCreated(),
                    product1.getDateUpdated(),
                    variantResponses
            ));
        }
        return  ResponseEntity.ok().build();

    }

    public interface ProductDetailProjection {
        Integer getProId();
        Integer getCateId();
        String getProName();
        String getDescription();
        String getListProImg();
        Boolean getIsDeleted();
        LocalDateTime getDateDeleted();
        LocalDateTime getDateCreated();
        LocalDateTime getDateUpdated();
    }


    private List<ProductImageResponse> parseImageList(String imageList) {
        if (imageList == null || imageList.isBlank()) return List.of();
        return Arrays.stream(imageList.split(", "))
                .map(entry -> {
                    String[] parts = entry.split(": ");
                    return new ProductImageResponse(Integer.parseInt(parts[0]), parts[1]);
                })
                .toList();
    }

    @Transactional
    public ResponseEntity<?> getOneProduct(Integer id, Language language) {
        ProductDetailProjection product = productRepository.findProductDetailProjectionById(id);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not exists");
        }

        String name = product.getProName();
        String desc = product.getDescription();


        if (language == Language.EN) {
            ProductTranslation trans = productTranslationRepository
                    .findByProduct_ProIdAndIsDeletedFalse(product.getProId());

            if (trans != null) {
                name = trans.getProName();
                desc = trans.getDescription();
            } else {
                name = supportFunction.convertLanguage(name, Language.EN);
                desc = supportFunction.convertLanguage(desc, Language.EN);


            }
        }
        List<ProductImageResponse> imageList = parseImageList(product.getListProImg());

        List<ProductVariantProjection1> variants =
                productVariantsRepository.findVariantByProductId(product.getProId());

        List<CRUDProductVarResponse> variantResponses = variants.stream()
                .map(v -> new CRUDProductVarResponse(
                        v.getVarId(), product.getProId(), v.getSize(), v.getPrice(),
                        v.getStock(), v.getIsDeleted(),
                        v.getDateDeleted(), v.getDateCreated(), v.getDateUpdated()
                ))
                .toList();

        return ResponseEntity.ok(new CRUDProductResponse(
                product.getProId(),
                product.getCateId(),
                name,
                imageList,
                desc,
                product.getIsDeleted(),
                product.getDateDeleted(),
                product.getDateCreated(),
                product.getDateUpdated(),
                variantResponses
        ));
    }



//    @Transactional
//    public ResponseEntity<?> getOneProduct(Integer id, Language language) {
//        Product product1 = productRepository.findByProIdAndIsDeletedFalse(id);
//        if (product1 == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product not exists");
//        }
//        List<ProductImageResponse> productImageResponses = new ArrayList<>();
//        String currentProImg = product1.getListProImg();
//        String name_product_trans = "";
//        String des = "";
//        if(language == Language.EN)
//        {
//            ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProIdAndIsDeletedFalse(product1.getProId());
//            if(productTranslation != null)
//            {
//                name_product_trans = productTranslation.getProName();
//                des = productTranslation.getDescription();
//            }
//            else {
//                name_product_trans = product1.getProName();
//                des = product1.getDescription();
//            }
//            System.out.println("Tên sp1: " + name_product_trans);
//            if (Objects.equals(name_product_trans, "") || Objects.equals(des, "")
//            ) {
//                name_product_trans = supportFunction.convertLanguage(product1.getProName(),Language.EN);
//                des = supportFunction.convertLanguage(product1.getDescription(),Language.EN);
//                ProductTranslation productTranslation1 = new ProductTranslation();
//                productTranslation1.setProName(name_product_trans);
//                productTranslation1.setDescription(des);
//                productTranslation1.setIsDeleted(Boolean.FALSE);
//                productTranslation1.setDateCreated(LocalDateTime.now());
//                productTranslation1.setProduct(product1);
//                productTranslationRepository.save(productTranslation1);
//                productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation1));
//            }
//        }
//        else{
//            des = product1.getDescription();
//        }
//        if(currentProImg != null && !currentProImg.trim().isEmpty())
//        {
//            String[] imageEntries1 = currentProImg.split(", ");
//            for (String imageEntry : imageEntries1) {
//                String[] parts = imageEntry.split(": ");
//                int stt = Integer.parseInt(parts[0]);
//                String url = parts[1];
//                productImageResponses.add(new ProductImageResponse(stt, url));
//            }
//        }
//        List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(product1.getProductVariants())
//                .orElse(Collections.emptyList()) // Trả về danh sách rỗng nếu là null
//                .stream()
//                .map(variant -> new CRUDProductVarResponse(
//                        variant.getVarId(),
//                        variant.getProduct().getProId(),
//                        variant.getSize(),
//                        variant.getPrice(),
//                        variant.getStock(),
//                        variant.getIsDeleted(),
//                        variant.getDateDeleted(),
//                        variant.getDateCreated(),
//                        variant.getDateUpdated()
//                ))
//                .toList();
//
//        if (name_product_trans == "") {
//            name_product_trans = product1.getProName();
//        }
//        return ResponseEntity.status(HttpStatus.OK).body(new CRUDProductResponse(
//                product1.getProId(),
//                product1.getCategory().getCateId(),
//                name_product_trans,
//                productImageResponses,
//                des,
//                product1.getIsDeleted(),
//                product1.getDateDeleted(),
//                product1.getDateCreated(),
//                product1.getDateUpdated(),
//                variantResponses
//        ));
//    }

    @Transactional
    public ResponseEntity<?> updateProduct(CRUDProductReq req) {
        String result_proname = "" ;
        String result_des = "";
        if(req.getLanguage() == Language.VN)
        {
            Category category = categoryRepository.findByCateIdAndIsDeletedFalse(req.getCateId());
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not exists");
            }
            Product product = productRepository.findByProNameAndProIdNot(req.getProName(), req.getProId());
            if (product != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("product already exists");
            }
            Product product1 = productRepository.findByProIdAndIsDeletedFalse(req.getProId());
            if (product1 == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product not exists");
            }
            product1.setCategory(category);
            product1.setDescription(supportFunction.convertLanguage(req.getDescription(),Language.VN));
            product1.setProName(supportFunction.convertLanguage(req.getProName(),Language.VN));
            product1.setDateUpdated(LocalDateTime.now());
            productRepository.save(product1);
            productElasticsearchRepository.save(convertToElasticsearch(product1));

            ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProIdAndIsDeletedFalse(product1.getProId());
            if(productTranslation != null)
            {
                productTranslation.setDateUpdated(LocalDateTime.now());
                productTranslation.setProName(supportFunction.convertLanguage(req.getProName(),Language.EN));
                productTranslation.setDescription(supportFunction.convertLanguage(req.getDescription(),Language.EN));
                productTranslationRepository.save(productTranslation);
                productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation));
            }
            result_proname = product1.getProName();
            result_des = product1.getDescription();
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

            return ResponseEntity.status(HttpStatus.OK).body(new CRUDProductResponse(
                    product1.getProId(),
                    product1.getCategory().getCateId(),
                    result_proname,
                    productImageResponses,
                    result_des,
                    product1.getIsDeleted(),
                    product1.getDateDeleted(),
                    product1.getDateCreated(),
                    product1.getDateUpdated(),
                    variantResponses
            ));
        }
        else {
            Category category = categoryRepository.findByCateIdAndIsDeletedFalse(req.getCateId());
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not exists");
            }
            ProductTranslation product_trans = productTranslationRepository.findByProNameAndProduct_ProIdNot(req.getProName(), req.getProId());
            if (product_trans != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("product already exists");
            }
            Product product1 = productRepository.findByProIdAndIsDeletedFalse(req.getProId());
            if (product1 == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product not exists");
            }
            product1.setCategory(category);
            product1.setDescription(supportFunction.convertLanguage(req.getDescription(),Language.VN));
            product1.setProName(supportFunction.convertLanguage(req.getProName(),Language.VN));
            product1.setDateUpdated(LocalDateTime.now());

            productRepository.save(product1);
            productElasticsearchRepository.save(convertToElasticsearch(product1));

            ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProIdAndIsDeletedFalse(product1.getProId());
            if(productTranslation != null)
            {
                productTranslation.setDateUpdated(LocalDateTime.now());
                productTranslation.setProName(supportFunction.convertLanguage(req.getProName(),Language.EN));
                productTranslation.setDescription(supportFunction.convertLanguage(req.getDescription(),Language.EN));
                productTranslationRepository.save(productTranslation);
                productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation));
                result_proname = productTranslation.getProName();
                result_des = productTranslation.getDescription();
            }
            else
            {

                    result_proname= supportFunction.convertLanguage(product1.getProName(),Language.EN);
                    result_des = supportFunction.convertLanguage(product1.getDescription(),Language.EN);
                    ProductTranslation productTranslation1 = new ProductTranslation();
                    productTranslation1.setProName(result_proname);
                    productTranslation1.setDescription(result_des);
                    productTranslation1.setIsDeleted(Boolean.FALSE);
                    productTranslation1.setDateCreated(LocalDateTime.now());
                    productTranslation1.setProduct(product1);
                    productTranslationRepository.save(productTranslation1);
                    productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation1));
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

            return ResponseEntity.status(HttpStatus.OK).body(new CRUDProductResponse(
                    product1.getProId(),
                    product1.getCategory().getCateId(),
                    result_proname,
                    productImageResponses,
                    result_des,
                    product1.getIsDeleted(),
                    product1.getDateDeleted(),
                    product1.getDateCreated(),
                    product1.getDateUpdated(),
                    variantResponses
            ));
        }

    }

    @Transactional
    public ResponseEntity<?> listProduct(String pageFromParam, String limitFromParam, Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);
        Pageable pageable = PageRequest.of(page - 1, limit);

        Page<Product> productPage = productRepository.findAvailableProducts(pageable);
        List<Product> products = productPage.getContent();
        int total = (int) productPage.getTotalElements();
        int totalPages = productPage.getTotalPages();

        // Preload translations
        List<Integer> productIds = products.stream().map(Product::getProId).toList();
        Map<Long, ProductTranslation> translations = productTranslationRepository
                .findByProduct_ProIdInAndIsDeletedFalse(productIds)
                .stream()
                .collect(Collectors.toMap(
                        t -> (long) t.getProduct().getProId(),
                        Function.identity()
                ));

        List<CRUDProductResponse> responseList = new ArrayList<>();
        for (Product product : products) {
            List<ProductImageResponse> productImageResponses = new ArrayList<>();
            String imgStr = product.getListProImg();
            if (imgStr != null && !imgStr.isBlank()) {
                Arrays.stream(imgStr.split(", "))
                        .map(img -> img.split(": "))
                        .filter(parts -> parts.length == 2)
                        .forEach(parts -> productImageResponses.add(
                                new ProductImageResponse(Integer.parseInt(parts[0]), parts[1])
                        ));
            }

            List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(product.getProductVariants())
                    .orElse(Collections.emptyList())
                    .stream()
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

            String name = "";
            String desc = "";

            if (language == Language.EN) {
                ProductTranslation translation = productTranslationRepository.findByProduct_ProId(product.getProId());
                if (translation != null) {
                    name = translation.getProName();
                    desc = translation.getDescription();
                }
            } else {
                name = product.getProName();
                desc = product.getDescription();
            }

            CRUDProductResponse response = new CRUDProductResponse(
                    product.getProId(),
                    product.getCategory().getCateId(),
                    name,
                    productImageResponses,
                    desc,
                    product.getIsDeleted(),
                    product.getDateDeleted(),
                    product.getDateCreated(),
                    product.getDateUpdated(),
                    variantResponses
            );

            responseList.add(response);
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ListProductResponse(page, totalPages, limit, total, responseList));
    }



//    @Transactional
//    public ResponseEntity<?> listProduct(String pageFromParam, String limitFromParam,Language language) {
//        int page = Integer.parseInt(pageFromParam);
//        int limit = Integer.parseInt(limitFromParam);
//        if (limit >= 100) limit = 100;
//        Pageable pageable = PageRequest.of(page - 1, limit);
//        List<Product> allAvailableProducts = productRepository.findAvailableProducts();
//        int total = allAvailableProducts.size();
//        int fromIndex = Math.min((page - 1) * limit, total);
//        int toIndex = Math.min(fromIndex + limit, total);
//        List<Product> pagedProducts = allAvailableProducts.subList(fromIndex, toIndex);
//
//
//
//
//        List<CRUDProductResponse> crudProductResponseList = new ArrayList<>();
//        for (Product product1 : pagedProducts) {
//
//            List<ProductImageResponse> productImageResponses = new ArrayList<>();
//            String currentProImg = product1.getListProImg();
//            if(currentProImg != null && !currentProImg.isBlank()) {
//                Arrays.stream(currentProImg.split(", "))
//                        .map(img -> img.split(": "))
//                        .filter(parts -> parts.length == 2)
//                        .forEach(parts -> productImageResponses.add(
//                                new ProductImageResponse(Integer.parseInt(parts[0]), parts[1])
//                        ));
//            }
//
//            List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(product1.getProductVariants())
//                    .orElse(Collections.emptyList()) // Trả về danh sách rỗng nếu là null
//                    .stream()
//                    .map(variant -> new CRUDProductVarResponse(
//                            variant.getVarId(),
//                            variant.getProduct().getProId(),
//                            variant.getSize(),
//                            variant.getPrice(),
//                            variant.getStock(),
//                            variant.getIsDeleted(),
//                            variant.getDateDeleted(),
//                            variant.getDateCreated(),
//                            variant.getDateUpdated()
//                    ))
//                    .toList();
//            String name_product_trans = "";
//            String des = "";
//            if(language == Language.EN)
//            {
//                ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProIdAndIsDeletedFalse(product1.getProId());
//                if(productTranslation != null)
//                {
//                    name_product_trans = productTranslation.getProName();
//                    des = productTranslation.getDescription();
//                }
//            }
//            else {
//                name_product_trans = product1.getProName();
//                des = product1.getDescription();
//            }
//            if (Objects.equals(name_product_trans, "") || Objects.equals(des, "")
//            ) {
//                name_product_trans = supportFunction.convertLanguage(product1.getProName(),language);
//                des = supportFunction.convertLanguage(product1.getDescription(),language);
//                ProductTranslation productTranslation = new ProductTranslation();
//                productTranslation.setProName(name_product_trans);
//                productTranslation.setDescription(des);
//                productTranslation.setIsDeleted(Boolean.FALSE);
//                productTranslation.setDateCreated(LocalDateTime.now());
//                productTranslation.setProduct(product1);
//                productTranslationRepository.save(productTranslation);
//                productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation));
//            }
//            crudProductResponseList.add(new CRUDProductResponse(
//                    product1.getProId(),
//                    product1.getCategory().getCateId(),
//                    name_product_trans,
//                    productImageResponses,
//                    des,
//                    product1.getIsDeleted(),
//                    product1.getDateDeleted(),
//                    product1.getDateCreated(),
//                    product1.getDateUpdated(),
//                    variantResponses
//            ));
//            total++;
//        }
//        return ResponseEntity.status(HttpStatus.OK).body(new ListProductResponse(page, pagedProducts., limit, crudProductResponseList.size(),crudProductResponseList));
//    }

    @Transactional
    public ResponseEntity<?> listProductTypeList(Language language) {

        List<Product> productList = productRepository.findByIsDeletedFalse();
        List<CRUDProductResponseAndroid> crudProductResponseList = new ArrayList<>();
        int total =0;
        for (Product product1 : productList) {
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
            }
            else {
                name_product_trans = product1.getProName();
                des = product1.getDescription();
            }
            if (Objects.equals(name_product_trans, "") || Objects.equals(des, "")
            ) {
                name_product_trans = supportFunction.convertLanguage(product1.getProName(),language);
                des = supportFunction.convertLanguage(product1.getDescription(),language);
                ProductTranslation productTranslation = new ProductTranslation();
                productTranslation.setProName(name_product_trans);
                productTranslation.setDescription(des);
                productTranslation.setIsDeleted(Boolean.FALSE);
                productTranslation.setDateCreated(LocalDateTime.now());
                productTranslation.setProduct(product1);
                productTranslationRepository.save(productTranslation);
                productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation));
            }
            Double avgRating = productRepository.findAverageRatingByProductId(product1.getCategory().getCateId(), product1.getProId());
            if (avgRating == null) avgRating = 0.0;
            crudProductResponseList.add(new CRUDProductResponseAndroid(
                    product1.getProId(),
                    Math.round(avgRating * 10) / 10.0,
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
            total++;
        }
        return ResponseEntity.status(HttpStatus.OK).body(new ListProductResponsetypeList(total,crudProductResponseList));
    }





    public interface ProductVariantProjection {
        Integer getVarId();
        Integer getProductId();
        Size getSize();
        Double getPrice();
        Integer getStock();
        Boolean getIsDeleted();
        LocalDateTime getDateDeleted();
        LocalDateTime getDateCreated();
        LocalDateTime getDateUpdated();
    }

    @Transactional
    public ResponseEntity<?> getAllProductVariantFromProduct(int id) {
        if (!productRepository.existsByProIdAndIsDeletedFalse(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product not exists");
        }

        List<ProductVariantProjection> projections =
                productRepository.findVariantProjectionByProductId(id);

        List<CRUDProductVarResponse> responseList = projections.stream()
                .map(p -> new CRUDProductVarResponse(
                        p.getVarId(),
                        p.getProductId(),
                        p.getSize(),
                        p.getPrice(),
                        p.getStock(),
                        p.getIsDeleted(),
                        p.getDateDeleted(),
                        p.getDateCreated(),
                        p.getDateUpdated()
                ))
                .toList();

        return ResponseEntity.ok(new GetProductVariantFromProductIdResponse(id, responseList));
    }



//    @Transactional
//    public ResponseEntity<?> getAllProductVariantFromProduct(int id) {
//        Product product = productRepository.findByProIdAndIsDeletedFalse(id);
//        if (product == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product not exists");
//        }
//        List<ProductVariants> productList = productVariantsRepository.findByProduct_ProId(id);
//        List<CRUDProductVarResponse> crudProductVarResponseList = new ArrayList<>();
//        for (ProductVariants product1 : productList) {
//
//            crudProductVarResponseList.add(new CRUDProductVarResponse(
//                    product1.getVarId(),
//                    product1.getProduct().getProId(),
//                    product1.getSize(),
//                    product1.getPrice(),
//                    product1.getStock(),
//                    product1.getIsDeleted(),
//                    product1.getDateDeleted(),
//                    product1.getDateCreated(),
//                    product1.getDateUpdated()
//            ));
//        }
//        return ResponseEntity.status(HttpStatus.OK).body(new GetProductVariantFromProductIdResponse(
//                id,
//                crudProductVarResponseList
//        ));
//    }

    @Transactional
    public ResponseEntity<?> totalSearchProductByCategory(String keyword, Integer cateId,  String pageFromParam, String limitFromParam,Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        if(language == Language.VN)
        {

            Page<ProductElasticsearch> productElasticsearches = productElasticsearchRepository.searchByProNameAndCategoryIdAndIsDeletedFalse(keyword,Long.valueOf(cateId),pageable,Boolean.FALSE);
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
                        int stt = Integer.parseInt(parts[0]);
                        String url = parts[1];
                        productImageResponses.add(new ProductImageResponse(stt, url));
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
                }
                else {
                    name_product_trans = product1.getProName();
                    des = product1.getDescription();
                }
                if (Objects.equals(name_product_trans, "") || Objects.equals(des, "")
                ) {
                    name_product_trans = supportFunction.convertLanguage(product1.getProName(),Language.EN);
                    des = supportFunction.convertLanguage(product1.getDescription(),Language.EN);
                    ProductTranslation productTranslation = new ProductTranslation();
                    productTranslation.setProName(name_product_trans);
                    productTranslation.setDescription(des);
                    productTranslation.setIsDeleted(Boolean.FALSE);
                    productTranslation.setDateCreated(LocalDateTime.now());
                    productTranslation.setProduct(product1);
                    productTranslationRepository.save(productTranslation);
                    productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation));
                }
                crudProductResponseList.add(new CRUDProductResponse(
                        product1.getProId(),
                        product1.getCategory().getCateId(),
                        name_product_trans,
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
            Page<ProductTranslationElasticsearch> productElasticsearches = productTranslationElasticsearchRepository.searchByProName(keyword,keyword,pageable);
            Page<ProductTranslation> productList = mapToProductTranslationPage(productElasticsearches, pageable);
            int total = 0;// Đếm tổng số sản phẩm

            List<CRUDProductResponse> crudProductResponseList = new ArrayList<>();
            for (ProductTranslation product1 : productList) {
                Product product_original = productRepository.findByProId(product1.getProduct().getProId());
                Category category = product_original.getCategory();
                if(category.getCateId() == cateId)
                {
                    List<ProductImageResponse> productImageResponses = new ArrayList<>();
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
                            product_original.getCategory().getCateId(),
                            product1.getProName(),
                            productImageResponses,
                            product1.getDescription(),
                            product1.getIsDeleted(),
                            product1.getDateDeleted(),
                            product1.getDateCreated(),
                            product1.getDateUpdated(),
                            variantResponses
                    ));
                    total += 1;
                }
                }


            return ResponseEntity.status(HttpStatus.OK)
                    .body(new TotalSearchProductResponse(page, productList.getTotalPages(), limit, total, crudProductResponseList));
        }

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


            Page<ProductTranslationElasticsearch> productElasticsearches = productTranslationElasticsearchRepository.searchByProNameAndIsDeletedFalse(keyword,pageable);
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

                List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(product_original.getProductVariants())
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

    @Transactional
    public ResponseEntity<?> deleteImageFromProduct(int proId, int deleteStt ) {
        Product product = productRepository.findByProIdAndIsDeletedFalse(proId);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product not exists");
        }
        String currentProImg = product.getListProImg();
        if (currentProImg == null || currentProImg.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No images found for this product.");
        }
        String[] imageEntries = currentProImg.split(", ");
        List<String> updatedImageEntries = new ArrayList<>();
        int currentStt = 1;
        for (String imageEntry : imageEntries) {
            String[] parts = imageEntry.split(": ");
            int stt = Integer.parseInt(parts[0]);
            String url = parts[1];

            if (stt == deleteStt) {
                continue;
            }
            updatedImageEntries.add(currentStt + ": " + url);
            currentStt++;
        }
        String updatedProImg = String.join(", ", updatedImageEntries);
        product.setListProImg(updatedProImg);
        product.setDateUpdated(LocalDateTime.now());
        productRepository.save(product);
        String currentProImg1 = product.getListProImg();
        if (currentProImg == null || currentProImg.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No images found for this product.");
        }
        List<ProductImageResponse> productImageResponses = new ArrayList<>();
        String[] imageEntries1 = currentProImg.split(", ");
        for (String imageEntry : imageEntries1) {
            String[] parts = imageEntry.split(": ");
            int stt = Integer.parseInt(parts[0]);
            String url = parts[1];
            productImageResponses.add(new ProductImageResponse(stt, url));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new ListProductImageResponse(proId,productImageResponses.size(),productImageResponses));
    }

    public ResponseEntity<?> deleteAllImageFromProduct(int proId) {
        Product product = productRepository.findByProIdAndIsDeletedFalse(proId);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product not exists");
        }
        product.setListProImg("");
        product.setDateUpdated(LocalDateTime.now());

        productRepository.save(product);
        return ResponseEntity.status(HttpStatus.OK).body(new ImgResponse(product.getListProImg()));
    }

    @Transactional
    public ResponseEntity<?> getAllProductImages(int proId) {
        List<ProductImageResponse> productImageResponses = new ArrayList<>();
        Product product = productRepository.findByProId(proId);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product not exists");
        }
        String currentProImg = product.getListProImg();
        if(currentProImg != null && !currentProImg.trim().isEmpty())
        {
            String[] imageEntries = currentProImg.split(", ");
            for (String imageEntry : imageEntries) {
                String[] parts = imageEntry.split(": ");
                int stt = Integer.parseInt(parts[0]);
                String url = parts[1];
                productImageResponses.add(new ProductImageResponse(stt, url));
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(new ListProductImageResponse(proId,productImageResponses.size(),productImageResponses));
    }

    @Transactional
    public ResponseEntity<?> filterProduct(FilterProductBox req) {
        int page = Integer.parseInt(req.getPage());
        int limit = Integer.parseInt(req.getLimit());

        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        // c -1: lay tat ca
        List<CRUDProductVarFilterResponse> crudProductVarFilterResponseList = new ArrayList<>();
        if(req.getC() != -1)
        {
            Category category = categoryRepository.findByCateIdAndIsDeletedFalse(req.getC());
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not exists");
            }
        }
        if(req.getP() != null && !req.getP().isEmpty())
        {
            for (Integer id : req.getP() ) {
                Product product = productRepository.findByProIdAndIsDeletedFalse(id);
                if (product == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product not exists");
                }
            }
        }

        if (req.getO() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("o must be greater than 0");
        }
        //option:
        // 1: gia thap den cao
        // 2 gia cao den thap
        // 3: theo ngay moi nhat
        // 4: theo rating thap den cao
        // 5: theo rating cao den thap
        // 6: Sản phẩm bán chạy
        int total = 0;
        long totalPages = 0;
        if (req.getO() == 1) {
            Page<Product> productWithPrices;
            if(req.getC() == -1){
                productWithPrices = productRepository.findAllProductsWithMinPriceNoCategory(pageable);
                total = (int) productWithPrices.getTotalElements();
            } else if (req.getP() == null || req.getP().isEmpty()) {
                productWithPrices = productRepository.findProductsWithMinPriceNoProduct(req.getC(),pageable);
                total = (int) productWithPrices.getTotalElements();
            }
            else {
                productWithPrices = productRepository.findProductsWithMinPrice(req.getC(), req.getP(),pageable);
                total = (int) productWithPrices.getTotalElements();
            }
            totalPages = productWithPrices.getTotalPages();
            for (Product product: productWithPrices) {
                List<CRUDProductVarResponse> variantResponses = product.getProductVariants().stream()
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

                Double avgRating = productRepository.findAverageRatingByProductId(product.getCategory().getCateId(), product.getProId());
                if (avgRating == null) avgRating = 0.0;
                List<ProductImageResponse> productImageResponses = new ArrayList<>();
                String currentProImg = product.getListProImg();
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
                String name_product_trans = "";
                String des = "";
                if(req.getLanguage() == Language.EN)
                {
                    ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                    if(productTranslation != null)
                    {
                        name_product_trans = productTranslation.getProName();
                        des = productTranslation.getDescription();
                    }
                    else {
                        name_product_trans = product.getProName();
                        des = product.getDescription();
                    }
                    if (Objects.equals(name_product_trans, "")
                    ) {
                        name_product_trans = supportFunction.convertLanguage(product.getProName(),Language.EN);
                        des = supportFunction.convertLanguage(product.getDescription(),Language.EN);
                        ProductTranslation productTranslation1 = new ProductTranslation();
                        productTranslation1.setProName(name_product_trans);
                        productTranslation1.setDescription(des);
                        productTranslation1.setIsDeleted(Boolean.FALSE);
                        productTranslation1.setDateCreated(LocalDateTime.now());
                        productTranslation1.setProduct(product);
                        productTranslationRepository.save(productTranslation1);
                        productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation1));
                    }
                }
                else{
                    name_product_trans = product.getProName();
                }
                crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
                        Math.round(avgRating * 10) / 10.0,
                        product.getProId(),
                        name_product_trans,
                        productImageResponses,
                        product.getIsDeleted(),
                        product.getDateDeleted(),
                        product.getDateCreated(),
                        product.getDateUpdated(),
                        variantResponses
                ));
            }

        } else if (req.getO() == 2) {
            Page<Product> productWithPrices;
            if(req.getC() == -1) {

                productWithPrices = productRepository.findAllProductsWithMaxPriceNoCategory(pageable);
                total = (int) productWithPrices.getTotalElements();
            }
            else if (req.getP() == null || req.getP().isEmpty()) {

                productWithPrices = productRepository.findProductsWithMaxPriceNoProduct(req.getC(),pageable);
                total = (int) productWithPrices.getTotalElements();
            } else {
                productWithPrices = productRepository.findProductsWithMaxPrice(req.getC(), req.getP(),pageable);
                total = (int) productWithPrices.getTotalElements();
            }
            totalPages = productWithPrices.getTotalPages();
            for (Product product : productWithPrices) {
                List<CRUDProductVarResponse> variantResponses = product.getProductVariants().stream()
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

                Double avgRating = productRepository.findAverageRatingByProductId(product.getCategory().getCateId(), product.getProId());
                if (avgRating == null) avgRating = 0.0;
                List<ProductImageResponse> productImageResponses = new ArrayList<>();
                String currentProImg = product.getListProImg();
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
                String name_product_trans = "";
                String des = "";
                if(req.getLanguage() == Language.EN)
                {
                    ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                    if(productTranslation != null)
                    {
                        name_product_trans = productTranslation.getProName();
                        des = productTranslation.getDescription();
                    }
                    else {
                        name_product_trans = product.getProName();
                        des = product.getDescription();
                    }
                    if (Objects.equals(name_product_trans, "")
                    ) {
                        name_product_trans = supportFunction.convertLanguage(product.getProName(),Language.EN);
                        des = supportFunction.convertLanguage(product.getDescription(),Language.EN);
                        ProductTranslation productTranslation1 = new ProductTranslation();
                        productTranslation1.setProName(name_product_trans);
                        productTranslation1.setDescription(des);
                        productTranslation1.setIsDeleted(Boolean.FALSE);
                        productTranslation1.setDateCreated(LocalDateTime.now());
                        productTranslation1.setProduct(product);
                        productTranslationRepository.save(productTranslation1);
                        productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation1));
                    }
                }
                else{
                    name_product_trans = product.getProName();
                }

                crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
                        Math.round(avgRating * 10) / 10.0,
                        product.getProId(),
                        name_product_trans,
                        productImageResponses,
                        product.getIsDeleted(),
                        product.getDateDeleted(),
                        product.getDateCreated(),
                        product.getDateUpdated(),
                        variantResponses
                ));
            }
        }
         else if (req.getO() == 3) {
            Page<Product> productWithPrices;

            Sort sort = Sort.by(Sort.Direction.DESC, "dateCreated");
            if (req.getC() == -1) {
                Pageable pageable1 = PageRequest.of(page, limit, sort);
                productWithPrices = productRepository.findByIsDeletedFalse(pageable1);
                total = (int) productWithPrices.getTotalElements();
            } else if (req.getP() == null || req.getP().isEmpty()) {
                Pageable pageable1 = PageRequest.of(page, limit, sort);
                productWithPrices = productRepository.findByCategory_CateIdAndIsDeletedFalse(req.getC(), pageable1);
                total = (int) productWithPrices.getTotalElements();
            } else {
                Pageable pageable1 = PageRequest.of(page, limit, sort);
                productWithPrices = productRepository.findByCategory_CateIdAndProIdInAndIsDeletedFalse(req.getC(), req.getP(), pageable1);
                total = (int) productWithPrices.getTotalElements();
            }

            totalPages = productWithPrices.getTotalPages();
            for (Product product: productWithPrices) {
                List<CRUDProductVarResponse> variantResponses = product.getProductVariants().stream()
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

                Double avgRating = productRepository.findAverageRatingByProductId(product.getCategory().getCateId(), product.getProId());
                if (avgRating == null) avgRating = 0.0;
                List<ProductImageResponse> productImageResponses = new ArrayList<>();
                String currentProImg = product.getListProImg();
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
                String name_product_trans = "";
                String des = "";
                if(req.getLanguage() == Language.EN)
                {
                    ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                    if(productTranslation != null)
                    {
                        name_product_trans = productTranslation.getProName();
                        des = productTranslation.getDescription();
                    }
                    else {
                        name_product_trans = product.getProName();
                        des = product.getDescription();
                    }
                    if (Objects.equals(name_product_trans, "")
                    ) {
                        name_product_trans = supportFunction.convertLanguage(product.getProName(),Language.EN);
                        des = supportFunction.convertLanguage(product.getDescription(),Language.EN);
                        ProductTranslation productTranslation1 = new ProductTranslation();
                        productTranslation1.setProName(name_product_trans);
                        productTranslation1.setDescription(des);
                        productTranslation1.setIsDeleted(Boolean.FALSE);
                        productTranslation1.setDateCreated(LocalDateTime.now());
                        productTranslation1.setProduct(product);
                        productTranslationRepository.save(productTranslation1);
                        productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation1));
                    }
                }
                else {
                    name_product_trans = product.getProName();
                    des = product.getDescription();
                }
                crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
                        Math.round(avgRating * 10) / 10.0,
                        product.getProId(),
                        name_product_trans,
                        productImageResponses,
                        product.getIsDeleted(),
                        product.getDateDeleted(),
                        product.getDateCreated(),
                        product.getDateUpdated(),
                        variantResponses
                ));
            }
        }
        else if (req.getO() == 4) {
            Page<Product> productWithPrices;
            if(req.getC() == -1)
            {
                productWithPrices = productRepository.findTopRatedProductsDesc(pageable);
                total = (int) productWithPrices.getTotalElements();
            }
            else if (req.getP() == null || req.getP().isEmpty()) {
                productWithPrices = productRepository.findTopRatedProductsDescByCategory(req.getC(),pageable);
                total = (int) productWithPrices.getTotalElements();
            } else {
                productWithPrices = productRepository.findTopRatedProductsDesc(req.getC(), req.getP(),pageable);
                total = (int) productWithPrices.getTotalElements();
            }
            totalPages = productWithPrices.getTotalPages();
            for (Product product: productWithPrices) {
                List<CRUDProductVarResponse> variantResponses = product.getProductVariants().stream()
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

                Double avgRating = productRepository.findAverageRatingByProductId(product.getCategory().getCateId(), product.getProId());
                if (avgRating == null) avgRating = 0.0;
                List<ProductImageResponse> productImageResponses = new ArrayList<>();
                String currentProImg = product.getListProImg();
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
                String name_product_trans = "";
                String des = "";
                if(req.getLanguage() == Language.EN)
                {
                    ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                    if(productTranslation != null)
                    {
                        name_product_trans = productTranslation.getProName();
                        des = productTranslation.getDescription();
                    }
                    else {
                        name_product_trans = product.getProName();
                        des = product.getDescription();
                    }
                    if (Objects.equals(name_product_trans, "")
                    ) {
                        name_product_trans = supportFunction.convertLanguage(product.getProName(),Language.EN);
                        des = supportFunction.convertLanguage(product.getDescription(),Language.EN);
                        ProductTranslation productTranslation1 = new ProductTranslation();
                        productTranslation1.setProName(name_product_trans);
                        productTranslation1.setDescription(des);
                        productTranslation1.setIsDeleted(Boolean.FALSE);
                        productTranslation1.setDateCreated(LocalDateTime.now());
                        productTranslation1.setProduct(product);
                        productTranslationRepository.save(productTranslation1);
                        productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation1));
                    }
                }
                else {
                    name_product_trans = product.getProName();
                    des = product.getDescription();
                }
                crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
                        Math.round(avgRating * 10) / 10.0,
                        product.getProId(),
                        name_product_trans,
                        productImageResponses,
                        product.getIsDeleted(),
                        product.getDateDeleted(),
                        product.getDateCreated(),
                        product.getDateUpdated(),
                        variantResponses
                ));
            }
        }
         else if (req.getO() == 5) {
            Page<Product> productWithPrices;
            if(req.getC() == -1)
            {
                productWithPrices = productRepository.findTopRatedProductsAsc(pageable);
                total = (int) productWithPrices.getTotalElements();
            }
            else  if (req.getP() == null || req.getP().isEmpty()) {
                productWithPrices = productRepository.findTopRatedProductsAscByCategory(req.getC(),pageable);
                total = (int) productWithPrices.getTotalElements();

            } else {
                productWithPrices = productRepository.findTopRatedProductsAsc(req.getC(), req.getP(),pageable);
                total = (int) productWithPrices.getTotalElements();
            }
            totalPages = productWithPrices.getTotalPages();
            for (Product product: productWithPrices) {
                List<CRUDProductVarResponse> variantResponses = product.getProductVariants().stream()
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

                Double avgRating = productRepository.findAverageRatingByProductId(product.getCategory().getCateId(), product.getProId());
                if (avgRating == null) avgRating = 0.0;
                List<ProductImageResponse> productImageResponses = new ArrayList<>();
                String currentProImg = product.getListProImg();
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
                String name_product_trans = "";
                String des = "";
                if(req.getLanguage() == Language.EN)
                {
                    ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProId(product.getProId());
                    if(productTranslation != null)
                    {
                        name_product_trans = productTranslation.getProName();
                        des = productTranslation.getDescription();
                    }
                    else {
                        name_product_trans = product.getProName();
                        des = product.getDescription();
                    }
                    if (Objects.equals(name_product_trans, "")
                    ) {
                        name_product_trans = supportFunction.convertLanguage(product.getProName(),Language.EN);
                        des = supportFunction.convertLanguage(product.getDescription(),Language.EN);
                        ProductTranslation productTranslation1 = new ProductTranslation();
                        productTranslation1.setProName(name_product_trans);
                        productTranslation1.setDescription(des);
                        productTranslation1.setIsDeleted(Boolean.FALSE);
                        productTranslation1.setDateCreated(LocalDateTime.now());
                        productTranslation1.setProduct(product);
                        productTranslationRepository.save(productTranslation1);
                        productTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(productTranslation1));
                    }
                }
                else {
                    name_product_trans = product.getProName();
                    des = product.getDescription();
                }
                crudProductVarFilterResponseList.add(new CRUDProductVarFilterResponse(
                        Math.round(avgRating * 10) / 10.0,
                        product.getProId(),
                        name_product_trans,
                        productImageResponses,
                        product.getIsDeleted(),
                        product.getDateDeleted(),
                        product.getDateCreated(),
                        product.getDateUpdated(),
                        variantResponses
                ));
            }
        } else if (req.getO() == 6) {
            Page<Object[]> productWithPrices;
            if (req.getC() == -1) {
                productWithPrices = productRepository.findBestSellingProducts(pageable);
                total = (int) productWithPrices.getTotalElements();
            } else if (req.getP() == null || req.getP().isEmpty()) {
                productWithPrices = productRepository.findBestSellingProductsByCategory(req.getC(), pageable);
                total = (int) productWithPrices.getTotalElements();

            } else {
                productWithPrices = productRepository.findBestSellingProductsByCategoryAndProductIds(req.getC(), req.getP(), pageable);
                total = (int) productWithPrices.getTotalElements();
            }
            totalPages = productWithPrices.getTotalPages();
            List<CRUDProductVarFilterBestSellResponse> productFilterResponses = new ArrayList<>();
            for (Object[] row : productWithPrices) {
                int varId = (Integer) row[0];
                long totalSold = (long) row[2];
                ProductVariants productVariants = productVariantsRepository.findByVarId(varId);
                Product product = productVariants.getProduct();
                List<ProductImageResponse> productImageResponses = new ArrayList<>();
                String currentProImg = product.getListProImg();
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
                Double avgRating = productRepository.findAverageRatingByProductId(product.getCategory().getCateId(), product.getProId());
                if (avgRating == null) avgRating = 0.0;
                CRUDProductVarFilterBestSellResponse crudProductVarFilterBestSellResponse = new CRUDProductVarFilterBestSellResponse(
                        avgRating,
                        totalSold,
                        productVariants.getVarId(),
                        productVariants.getSize(),
                        productVariants.getPrice(),
                        productVariants.getStock(),
                        product.getProId(),
                        supportFunction.convertLanguage(product.getProName(),req.getLanguage()),
                        productImageResponses

                );
                productFilterResponses.add(crudProductVarFilterBestSellResponse);

            }
            return ResponseEntity.status(HttpStatus.OK).body(new FilterProductBestSellBoxResponse(
                    page,
                    totalPages,
                    limit,
                    total,
                    productFilterResponses
            ));
        }
         return ResponseEntity.status(HttpStatus.OK).body(new FilterProductBoxResponse(
                    page,
                    totalPages,
                    limit,
                    total,
                    crudProductVarFilterResponseList
            ));
        }

    @Transactional
    public ResponseEntity<?> resetAllQuantityProduct()
    {
        List<Product> productList = productRepository.findAll();
        for(Product product : productList)
        {
            List<ProductVariants> productVariants = productVariantsRepository.findByProduct_ProId(product.getProId());
            for(ProductVariants productVariant : productVariants)
            {
                productVariant.setStock(50);
                productVariantsRepository.save(productVariant);
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body("Reset success");
    }

//    @Transactional
//    public ResponseEntity<?> disableProduct(int proId){
//        Product product = productRepository.findByProId(proId);
//        if(product == null)
//        {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
//        }
//        if(product.getIsDeleted()){
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Product is deleted");
//        }
//        List<ProductVariants> productVariants = productVariantsRepository.findByProduct_ProId(product.getProId());
//        for(ProductVariants productVariant: productVariants)
//        {
//            productVariant.setIsDeleted(true);
//            productVariant.setDateDeleted(LocalDateTime.now());
//            productVariantsRepository.save(productVariant);
//            List<FavouriteItem> favouriteItems = favouriteItemRepository.findByProductVariants_VarId(productVariant.getVarId());
//            for(FavouriteItem favouriteItem: favouriteItems)
//            {
//                favouriteItem.setIsDeleted(true);
//                favouriteItem.setDateDeleted(LocalDateTime.now());
//                favouriteItemRepository.save(favouriteItem);
//            }
//            List<CartItem> cartItems = cartItemRepository.findByProductVariants_VarId(productVariant.getVarId());
//            for(CartItem cartItem : cartItems)
//            {
//                cartItem.setIsDeleted(true);
//                cartItem.setDateDeleted(LocalDateTime.now());
//                cartItemRepository.save(cartItem);
//            }
//            List<Cart> carts = cartRepository.findAll();
//            for(Cart cart : carts){
//                List<CartItem> cartItems1 = cartItemRepository.findByCart_CartIdAndIsDeletedFalse(cart.getCartId());
//                double total = 0.0 ;
//                int quantity = 0;
//                for(CartItem cartItem1 : cartItems1)
//                {
//                    total += cartItem1.getTotalPrice();
//                    quantity += cartItem1.getQuantity();
//                }
//                cart.setTotalPrice(total);
//                cart.setTotalProduct(quantity);
//
//                cartRepository.save(cart);
//            }
//        }
//        List<Review> reviewList = reviewRepository.findAllByProduct_ProId(product.getProId());
//        for(Review review : reviewList)
//        {
//            review.setIsDeleted(true);
//            review.setDateDeleted(LocalDateTime.now());
//            reviewRepository.save(review);
//        }
//        product.setIsDeleted(true);
//        product.setDateDeleted(LocalDateTime.now());
//        productRepository.save(product);
//        List<ProductImageResponse> productImageResponses = new ArrayList<>();
//        String currentProImg = product.getListProImg();
//        if(currentProImg != null && !currentProImg.trim().isEmpty())
//        {
//            String[] imageEntries1 = currentProImg.split(", ");
//            for (String imageEntry : imageEntries1) {
//                String[] parts = imageEntry.split(": ");
//                int stt = Integer.parseInt(parts[0]);
//                String url = parts[1];
//                productImageResponses.add(new ProductImageResponse(stt, url));
//            }
//        }
//        List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(product.getProductVariants())
//                .orElse(Collections.emptyList()) // Trả về danh sách rỗng nếu là null
//                .stream()
//                .map(variant -> new CRUDProductVarResponse(
//                        variant.getVarId(),
//                        variant.getProduct().getProId(),
//                        variant.getSize(),
//                        variant.getPrice(),
//                        variant.getStock(),
//                        variant.getIsDeleted(),
//                        variant.getDateDeleted(),
//                        variant.getDateCreated(),
//                        variant.getDateUpdated()
//                ))
//                .toList();
//
//        return ResponseEntity.status(HttpStatus.OK).body( new CRUDProductResponse(
//                product.getProId(),
//                product.getCategory().getCateId(),
//                product.getProName(),
//                productImageResponses,
//                product.getDescription(),
//                product.getIsDeleted(),
//                product.getDateDeleted(),
//                product.getDateCreated(),
//                product.getDateUpdated(),
//                variantResponses
//        ));
//    }

    @Transactional
    public ResponseEntity<?> disableProduct(int proId) {
        Product product = productRepository.findByProId(proId);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }
        if (product.getIsDeleted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Product is already deleted");
        }

        LocalDateTime now = LocalDateTime.now();

        // Batch update ProductVariants
        List<ProductVariants> productVariants = productVariantsRepository.findByProduct_ProId(product.getProId());
        if (!productVariants.isEmpty()) {
            productVariants.forEach(variant -> {
                variant.setIsDeleted(true);
                variant.setDateDeleted(now);
            });
            productVariantsRepository.saveAll(productVariants);

            // Batch update FavouriteItems
            List<FavouriteItem> favouriteItems = favouriteItemRepository.findByProductVariantsIn(productVariants);
            if (!favouriteItems.isEmpty()) {
                favouriteItems.forEach(item -> {
                    item.setIsDeleted(true);
                    item.setDateDeleted(now);
                });
                favouriteItemRepository.saveAll(favouriteItems);
            }

            // Batch update CartItems
            List<CartItem> cartItems = cartItemRepository.findByProductVariantsIn(productVariants);
            if (!cartItems.isEmpty()) {
                cartItems.forEach(item -> {
                    item.setIsDeleted(true);
                    item.setDateDeleted(now);
                });
                cartItemRepository.saveAll(cartItems);
            }

            List<CartItemGroup> cartItemGroups = cartItemGroupRepository.findByProductVariantsIn(productVariants);
            if (!cartItemGroups.isEmpty()) {
                cartItemGroups.forEach(item -> {
                    item.setIsDisabled(true);
                    item.setDateDeleted(now);
                });
                cartItemGroupRepository.saveAll(cartItemGroups);
            }
        }


        List<GroupOrders> groupOrders = groupOrdersRepository.findAllByIsDeletedFalse();

// Lấy toàn bộ groupOrderMembers và cartIds
        List<GroupOrderMember> allMembers = new ArrayList<>();
        Set<Integer> allCartIds = new HashSet<>();
        for (GroupOrders groupOrder : groupOrders) {
            List<GroupOrderMember> members = groupOrder.getGroupOrderMembers();
            allMembers.addAll(members);
            for (GroupOrderMember member : members) {
                allCartIds.add(member.getCartGroup().getCartId());
            }
        }

// Lấy toàn bộ cartItemGroup chỉ 1 query
        List<CartItemGroup> allItems = cartItemGroupRepository.findAllByCartGroupIds(new ArrayList<>(allCartIds));

// Gộp theo cartId
        Map<Integer, List<CartItemGroup>> itemGroupMap = allItems.stream()
                .collect(Collectors.groupingBy(c -> c.getCartGroup().getCartId()));

// Cập nhật lại từng đơn
        for (GroupOrders groupOrder : groupOrders) {
            double groupTotalPrice = 0.0;
            int groupTotalQuantity = 0;

            for (GroupOrderMember member : groupOrder.getGroupOrderMembers()) {
                CartGroup cartGroup = member.getCartGroup();
                List<CartItemGroup> items = itemGroupMap.getOrDefault(cartGroup.getCartId(), Collections.emptyList());

                int quantity = items.stream().mapToInt(CartItemGroup::getQuantity).sum();
                double price = items.stream().mapToDouble(CartItemGroup::getTotalPrice).sum();

                cartGroup.setTotalProduct(quantity);
                cartGroup.setTotalPrice(price);

                member.setQuantity(quantity);
                member.setAmount(price);

                groupTotalPrice += price;
                groupTotalQuantity += quantity;
            }

            groupOrder.setTotalPrice(groupTotalPrice);
            groupOrder.setTotalQuantity(groupTotalQuantity);
        }

// Batch save (1 lần duy nhất)
        cartGroupRepository.saveAll(allMembers.stream().map(GroupOrderMember::getCartGroup).toList());
        groupOrdersRepository.saveAll(groupOrders);


        // Batch update Cart totals
        List<Cart> carts = cartRepository.findAll();
        carts.forEach(cart -> {
            List<CartItem> activeCartItems = cartItemRepository.findByCart_CartIdAndIsDeletedFalseAndIsDisabledFalse(cart.getCartId());
            double total = activeCartItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
            int quantity = activeCartItems.stream().mapToInt(CartItem::getQuantity).sum();

            cart.setTotalPrice(total);
            cart.setTotalProduct(quantity);
        });
        cartRepository.saveAll(carts);

        // Batch update Reviews
        List<Review> reviews = reviewRepository.findAllByProduct_ProId(product.getProId());
        if (!reviews.isEmpty()) {
            reviews.forEach(review -> {
                review.setIsDeleted(true);
                review.setDateDeleted(now);
            });
            reviewRepository.saveAll(reviews);
        }

        List<ProductTranslation> productTranslations = product.getProductTranslations();
        if (!productTranslations.isEmpty()) {
            productTranslations.forEach(translation -> {
                translation.setIsDeleted(true);
                translation.setDateDeleted(LocalDateTime.now());
            });

            // Lưu vào database
            productTranslationRepository.saveAll(productTranslations);

            // Chuyển đổi sang Elasticsearch model
            List<ProductTranslationElasticsearch> elasticTranslations = productTranslations.stream()
                    .map(this::convertToElasticsearchTranslation)
                    .toList();

            // Lưu vào Elasticsearch
            productTranslationElasticsearchRepository.saveAll(elasticTranslations);
        }

        // Update product itself
        product.setIsDeleted(true);
        product.setDateDeleted(now);
        productRepository.save(product);
        productElasticsearchRepository.save(convertToElasticsearch(product));

        // Process product images
        List<ProductImageResponse> productImageResponses = new ArrayList<>();
        String currentProImg = product.getListProImg();
        if (currentProImg != null && !currentProImg.trim().isEmpty()) {
            String[] imageEntries = currentProImg.split(", ");
            for (String imageEntry : imageEntries) {
                String[] parts = imageEntry.split(": ");
                int stt = Integer.parseInt(parts[0]);
                String url = parts[1];
                productImageResponses.add(new ProductImageResponse(stt, url));
            }
        }

        // Prepare variant responses
        List<CRUDProductVarResponse> variantResponses = productVariants.stream()
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

        // Return response
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDProductResponse(
                product.getProId(),
                product.getCategory().getCateId(),
                product.getProName(),
                productImageResponses,
                product.getDescription(),
                product.getIsDeleted(),
                product.getDateDeleted(),
                product.getDateCreated(),
                product.getDateUpdated(),
                variantResponses
        ));
    }




    @Transactional
    public ResponseEntity<?> enableProduct(int proId) {
        Product product = productRepository.findByProId(proId);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }
        if (!product.getIsDeleted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Product is already enabled");
        }

        // Batch update ProductVariants
        List<ProductVariants> productVariants = productVariantsRepository.findByProduct_ProId(product.getProId());
        if (!productVariants.isEmpty()) {
            productVariants.forEach(variant -> {
                variant.setIsDeleted(false);
                variant.setDateDeleted(null);
            });
            productVariantsRepository.saveAll(productVariants);

            // Batch update FavouriteItems
            List<FavouriteItem> favouriteItems = favouriteItemRepository.findByProductVariantsIn(productVariants);
            if (!favouriteItems.isEmpty()) {
                favouriteItems.forEach(item -> {
                    item.setIsDeleted(false);
                    item.setDateDeleted(null);
                });
                favouriteItemRepository.saveAll(favouriteItems);
            }

            // Batch update CartItems
            List<CartItem> cartItems = cartItemRepository.findByProductVariantsIn(productVariants);
            if (!cartItems.isEmpty()) {
                cartItems.forEach(item -> {
                    item.setIsDeleted(false);
                    item.setDateDeleted(null);
                });
                cartItemRepository.saveAll(cartItems);
            }

            List<CartItemGroup> cartItemGroups = cartItemGroupRepository.findByProductVariantsIn(productVariants);
            if (!cartItemGroups.isEmpty()) {
                cartItemGroups.forEach(item -> {
                    item.setIsDisabled(false);
                    item.setDateDeleted(null);
                });
                cartItemGroupRepository.saveAll(cartItemGroups);
            }
        }

        // Recalculate Cart totals
        List<Cart> carts = cartRepository.findAll();
        carts.forEach(cart -> {
            List<CartItem> activeCartItems = cartItemRepository.findByCart_CartIdAndIsDeletedFalseAndIsDisabledFalse(cart.getCartId());
            double total = activeCartItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
            int quantity = activeCartItems.stream().mapToInt(CartItem::getQuantity).sum();

            cart.setTotalPrice(total);
            cart.setTotalProduct(quantity);
        });
        cartRepository.saveAll(carts);

        // Batch update Reviews
        List<Review> reviews = reviewRepository.findAllByProduct_ProId(product.getProId());
        if (!reviews.isEmpty()) {
            reviews.forEach(review -> {
                review.setIsDeleted(false);
                review.setDateDeleted(null);
            });
            reviewRepository.saveAll(reviews);
        }

        List<ProductTranslation> productTranslations = product.getProductTranslations();
        if (!productTranslations.isEmpty()) {
            productTranslations.forEach(translation -> {
                translation.setIsDeleted(false);
                translation.setDateDeleted(null);
            });

            // Lưu vào database
            productTranslationRepository.saveAll(productTranslations);

            // Chuyển đổi sang Elasticsearch model
            List<ProductTranslationElasticsearch> elasticTranslations = productTranslations.stream()
                    .map(this::convertToElasticsearchTranslation)
                    .toList();

            // Lưu vào Elasticsearch
            productTranslationElasticsearchRepository.saveAll(elasticTranslations);
        }


        List<GroupOrders> groupOrders = groupOrdersRepository.findAllByIsDeletedFalse();

// Lấy toàn bộ groupOrderMembers và cartIds
        List<GroupOrderMember> allMembers = new ArrayList<>();
        Set<Integer> allCartIds = new HashSet<>();
        for (GroupOrders groupOrder : groupOrders) {
            List<GroupOrderMember> members = groupOrder.getGroupOrderMembers();
            allMembers.addAll(members);
            for (GroupOrderMember member : members) {
                allCartIds.add(member.getCartGroup().getCartId());
            }
        }

// Lấy toàn bộ cartItemGroup chỉ 1 query
        List<CartItemGroup> allItems = cartItemGroupRepository.findAllByCartGroupIds(new ArrayList<>(allCartIds));

// Gộp theo cartId
        Map<Integer, List<CartItemGroup>> itemGroupMap = allItems.stream()
                .collect(Collectors.groupingBy(c -> c.getCartGroup().getCartId()));

// Cập nhật lại từng đơn
        for (GroupOrders groupOrder : groupOrders) {
            double groupTotalPrice = 0.0;
            int groupTotalQuantity = 0;

            for (GroupOrderMember member : groupOrder.getGroupOrderMembers()) {
                CartGroup cartGroup = member.getCartGroup();
                List<CartItemGroup> items = itemGroupMap.getOrDefault(cartGroup.getCartId(), Collections.emptyList());

                int quantity = items.stream().mapToInt(CartItemGroup::getQuantity).sum();
                double price = items.stream().mapToDouble(CartItemGroup::getTotalPrice).sum();

                cartGroup.setTotalProduct(quantity);
                cartGroup.setTotalPrice(price);

                member.setQuantity(quantity);
                member.setAmount(price);

                groupTotalPrice += price;
                groupTotalQuantity += quantity;
            }

            groupOrder.setTotalPrice(groupTotalPrice);
            groupOrder.setTotalQuantity(groupTotalQuantity);
        }

// Batch save (1 lần duy nhất)
        cartGroupRepository.saveAll(allMembers.stream().map(GroupOrderMember::getCartGroup).toList());
        groupOrdersRepository.saveAll(groupOrders);



        // Update product itself
        product.setIsDeleted(false);
        product.setDateDeleted(null);
        productRepository.save(product);
        productElasticsearchRepository.save(convertToElasticsearch(product));

        // Process product images
        List<ProductImageResponse> productImageResponses = new ArrayList<>();
        String currentProImg = product.getListProImg();
        if (currentProImg != null && !currentProImg.trim().isEmpty()) {
            String[] imageEntries = currentProImg.split(", ");
            for (String imageEntry : imageEntries) {
                String[] parts = imageEntry.split(": ");
                int stt = Integer.parseInt(parts[0]);
                String url = parts[1];
                productImageResponses.add(new ProductImageResponse(stt, url));
            }
        }

        // Prepare variant responses
        List<CRUDProductVarResponse> variantResponses = productVariants.stream()
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

        // Return response
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDProductResponse(
                product.getProId(),
                product.getCategory().getCateId(),
                product.getProName(),
                productImageResponses,
                product.getDescription(),
                product.getIsDeleted(),
                product.getDateDeleted(),
                product.getDateCreated(),
                product.getDateUpdated(),
                variantResponses
        ));
    }

    private List<ProductImageResponse> parseProductImages(String listProImg) {
        List<ProductImageResponse> images = new ArrayList<>();
        if (listProImg == null || listProImg.isBlank()) {
            return images;
        }

        String[] entries = listProImg.split(", ");
        for (String entry : entries) {
            int colonIndex = entry.indexOf(": ");
            if (colonIndex > 0) {
                try {
                    int stt = Integer.parseInt(entry.substring(0, colonIndex));
                    String url = entry.substring(colonIndex + 2);
                    images.add(new ProductImageResponse(stt, url));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return images;
    }

    public interface ProductWithAvgRatingProjection {
        Integer getProId();
        Integer getCategory();
        String getProName();
        String getListProImg();
        String getDescription();
        Boolean getIsDeleted();
        LocalDateTime getDateDeleted();
        LocalDateTime getDateCreated();
        LocalDateTime getDateUpdated();
        Double getAvgRating();
    }


    public interface ProductWithRatingProjection {
        Integer getProId();
        Integer getCateId();
        String getProName();
        String getDescription();
        Double getAverageRating();
        Boolean getIsDeleted();
        LocalDateTime getDateDeleted();
        LocalDateTime getDateCreated();
        LocalDateTime getDateUpdated();
        String getListProImg(); // để parse nhanh
    }


//    @Transactional
//    public ResponseEntity<?> listProductsWithAverageRating(String pageFromParam, String limitFromParam, Language language) {
//        int page = Integer.parseInt(pageFromParam);
//        int limit = Math.min(Integer.parseInt(limitFromParam), 100);
//        Pageable pageable = PageRequest.of(page - 1, limit);
//
//        Page<Product> productList = productRepository.findByIsDeletedFalse(pageable);
//        List<Product> products = productList.getContent();
//
//        // Lấy danh sách id sản phẩm
//        List<Integer> productIds = products.stream()
//                .map(Product::getProId)
//                .collect(Collectors.toList());
//
//        // Lấy rating trung bình 1 phát
//        List<Object[]> avgRatings = reviewRepository.findAverageRatingForProducts(productIds);
//        Map<Integer, Double> avgRatingMap = avgRatings.stream()
//                .collect(Collectors.toMap(
//                        row -> (Integer) row[0],
//                        row -> (Double) row[1]
//                ));
//
//        // Dùng parallelStream() cho đa luồng
//        List<ProductWithAvgRatingResponse> productWithAvgRatingResponses = products.parallelStream()
//                .map(product -> {
//                    double avgRating = avgRatingMap.getOrDefault(product.getProId(), 0.0);
//
//                    // Xử lý ảnh nhanh bằng regex
//                    List<ProductImageResponse> productImageResponses = parseProductImages(product.getListProImg());
//
//                    return new ProductWithAvgRatingResponse(
//                            product.getProId(),
//                            product.getCategory().getCateId(),
//                            supportFunction.convertLanguage(product.getProName(), language),
//                            productImageResponses,
//                            product.getDescription(),
//                            avgRating,
//                            product.getIsDeleted(),
//                            product.getDateDeleted(),
//                            product.getDateCreated(),
//                            product.getDateUpdated()
//                    );
//                })
//                .collect(Collectors.toList());
//
//        return ResponseEntity.status(HttpStatus.OK).body(new ListProductWithAvgRatingResponse(
//                page,
//                productList.getTotalPages(),
//                limit,
//                products.size(),
//                productWithAvgRatingResponses
//        ));
//    }


    public ResponseEntity<?> listProductsWithAverageRating(String pageFromParam, String limitFromParam, Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);
        Pageable pageable = PageRequest.of(page - 1, limit);

        Page<ProductWithRatingProjection> pageData = productRepository.findAllProductsWithAvgRating(pageable);

        List<ProductWithAvgRatingResponse> responses = pageData.getContent().parallelStream()
                .map(p -> new ProductWithAvgRatingResponse(
                        p.getProId(),
                        p.getCateId(),
                        supportFunction.convertLanguage(p.getProName(), language),
                        parseProductImages(p.getListProImg()),
                        p.getDescription(),
                        p.getAverageRating() != null ? p.getAverageRating() : 0.0,
                        p.getIsDeleted(),
                        p.getDateDeleted(),
                        p.getDateCreated(),
                        p.getDateUpdated()
                )).toList();

        return ResponseEntity.ok(new ListProductWithAvgRatingResponse(
                page,
                pageData.getTotalPages(),
                limit,
                (int) pageData.getTotalElements(),
                responses
        ));
    }









    @Transactional
    public ResponseEntity<?> listAvgRatingProduct() {
        List<Product> productList = productRepository.findByIsDeletedFalse();
        List<ProductRatingAvgDTO> ratingAvgs = productRepository.findAllAvgRatings();

        Map<Integer, Double> avgMap = ratingAvgs.stream()
                .collect(Collectors.toMap(ProductRatingAvgDTO::getProductId, ProductRatingAvgDTO::getAvgRating));

        List<AvgRatingProduct> avgRatingProductList = new ArrayList<>();
        for (Product product : productList) {
            double avgRating = avgMap.getOrDefault(product.getProId(), 0.0);
            avgRatingProductList.add(new AvgRatingProduct(product.getProId(), avgRating));
        }

        return new ResponseEntity<>(
                new ListAllProductRating(avgRatingProductList.size(), avgRatingProductList),
                HttpStatus.OK
        );
    }

    @Transactional
    public ResponseEntity<?> sendRecommendationRequest(String token, int number,Language language) {
        String  url = userServiceUrl + "/recommend";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        System.out.println(token);
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        // Tạo JSON body
        String jsonBody = "{ \"number\": " + number + " }";

        // Tạo request
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        // Gửi POST request và nhận ResponseEntity
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        List<CRUDProductResponse> crudProductResponseList = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            RecommendResponse resObj = mapper.readValue(response.getBody(), RecommendResponse.class);
            for (ItemRecommend item : resObj.listItem) {
                Product product = productRepository.findByProIdAndIsDeletedFalse(item.proId);
                if(product != null) {
                    List<ProductImageResponse> productImageResponses = new ArrayList<>();
                    String currentProImg = product.getListProImg();
                    if (currentProImg != null && !currentProImg.trim().isEmpty()) {
                        String[] imageEntries1 = currentProImg.split(", ");
                        for (String imageEntry : imageEntries1) {
                            String[] parts = imageEntry.split(": ");
                            if (parts.length == 2) {
                                int stt = Integer.parseInt(parts[0].trim());
                                String url_image = parts[1].trim();
                                productImageResponses.add(new ProductImageResponse(stt, url_image));
                            } else {
                                System.out.println("⚠️ Entry sai format: \"" + imageEntry + "\"");
                            }
                        }
                    }
                    String name = "";
                    String des = "";
                    if(language == Language.VN)
                    {
                        name = product.getProName();
                        des = product.getDescription();
                    }
                    else {
                        ProductTranslation productTranslation = productTranslationRepository.findByProduct_ProIdAndIsDeletedFalse(product.getProId());
                        name = productTranslation.getProName();
                        des = productTranslation.getDescription();
                    }



                    List<CRUDProductVarResponse> variantResponses = Optional.ofNullable(productVariantsRepository.findByProduct_ProId(product.getProId()))
                            .orElse(Collections.emptyList())
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
                            product.getProId(),
                            product.getCategory().getCateId(),
                            name,
                            productImageResponses,
                            des,
                            product.getIsDeleted(),
                            product.getDateDeleted(),
                            product.getDateCreated(),
                            product.getDateUpdated(),
                            variantResponses
                    ));

                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi parse JSON: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK).body(new ListProductRecommendResponse(
                number,
                crudProductResponseList
        ));
    }

    public static  class RecommendResponse {
        @JsonProperty("user_id")
        public int userId;

        public int total;

        @JsonProperty("list_item")
        public List<ItemRecommend> listItem;
    }

    public static  class ItemRecommend {
        @JsonProperty("pro_id")
        public int proId;

        @JsonProperty("product_name")
        public String productName;
    }
}
