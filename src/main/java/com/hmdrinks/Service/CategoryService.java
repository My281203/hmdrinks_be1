package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.RepositoryElasticsearch.CategoryElasticsearchRepository;
import com.hmdrinks.RepositoryElasticsearch.CategoryTranslationElasticsearchRepository;
import com.hmdrinks.RepositoryElasticsearch.ProductElasticsearchRepository;
import com.hmdrinks.RepositoryElasticsearch.ProductTranslationElasticsearchRepository;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageImpl;
import com.hmdrinks.Repository.*;
import com.hmdrinks.Request.CRUDCategoryRequest;
import com.hmdrinks.Request.CreateCategoryRequest;
import com.hmdrinks.Response.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantsRepository productVariantsRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private FavouriteItemRepository favouriteItemRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private ProductTranslationRepository productTranslationRepository;
    @Autowired
    private  CategoryTranslationRepository categoryTranslationRepository;
    @Autowired
    private CategoryElasticsearchRepository categoryElasticsearchRepository;
    @Autowired
    private CategoryTranslationElasticsearchRepository categoryTranslationElasticsearchRepository;

    @Autowired
    private ProductElasticsearchRepository productElasticsearchRepository;
    @Autowired
    private ProductTranslationElasticsearchRepository productTranslationElasticsearchRepository;

    @Autowired
    private  CartGroupRepository cartGroupRepository;
    @Autowired
    private GroupOrderMembersRepository groupOrderMembersRepository;
    @Autowired
    private  GroupOrdersRepository groupOrdersRepository;
    @Autowired
    private CartItemGroupRepository cartItemGroupRepository;

    private ProductElasticsearch convertToElasticsearchProduct(Product product) {
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

    private ProductTranslationElasticsearch convertToElasticsearchTranslationProduct(ProductTranslation translation) {
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

    private CategoryElasticsearch convertToElasticsearch(Category category) {
        return new CategoryElasticsearch(
                String.valueOf(category.getCateId()),
                category.getCateName(),
                category.getCateImg(),
                category.getIsDeleted(),
                category.getDateCreated(),
                category.getDateUpdated(),
                category.getDateDeleted(),
                category.getCateId()
        );
    }

    private CategoryTranslationElasticsearch convertToElasticsearchTranslation(CategoryTranslation translation) {
        return new CategoryTranslationElasticsearch(
                String.valueOf(translation.getCateTransId()),
                translation.getCateName(),
                translation.getIsDeleted(),
                translation.getDateCreated(),
                translation.getDateUpdated(),
                translation.getDateDeleted(),
                translation.getLanguage(),
                translation.getCategory().getCateId()
        );
    }

    @Transactional
    public ResponseEntity<?> crateCategory(CreateCategoryRequest req) {

        if(req.getLanguage() == Language.VN)
        {
            Category category = categoryRepository.findByCateName(req.getCateName());
            if (category != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("cateName exists");
            }
            LocalDateTime now = LocalDateTime.now();
            Category cate = new Category();
            cate.setCateName(supportFunction.convertLanguage(req.getCateName(),Language.VN));
            cate.setCateImg(req.getCateImg());
            cate.setIsDeleted(false);
            cate.setDateCreated(LocalDateTime.now());


            categoryRepository.save(cate);
            categoryElasticsearchRepository.save(convertToElasticsearch(cate));
            String name_trans = supportFunction.convertLanguage(req.getCateName(),Language.EN);
            CategoryTranslation categoryTranslation = new CategoryTranslation();
            categoryTranslation.setCategory(cate);
            categoryTranslation.setLanguage(Language.EN);
            categoryTranslation.setDateCreated(LocalDateTime.now());
            categoryTranslation.setIsDeleted(false);
            categoryTranslation.setCateName(name_trans);
            categoryTranslationRepository.save(categoryTranslation);
            categoryTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(categoryTranslation));




            return ResponseEntity.status(HttpStatus.OK).body(new CRUDCategoryResponse(
                    cate.getCateId(),
                    req.getCateName(),
                    cate.getCateImg(),
                    cate.getIsDeleted(),
                    cate.getDateCreated(),
                    cate.getDateUpdated(),
                    cate.getDateDeleted()
            ));
        } else if (req.getLanguage() == Language.EN) {
            CategoryTranslation categoryTranslation1 = categoryTranslationRepository.findByCateName(req.getCateName());
            if (categoryTranslation1 != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("cateName exists");
            }
            String name_trans = supportFunction.convertLanguage(req.getCateName(),Language.VN);
            Category cate = new Category();
            cate.setCateName(name_trans);
            cate.setCateImg(req.getCateImg());
            cate.setIsDeleted(false);
            cate.setDateCreated(LocalDateTime.now());


            categoryRepository.save(cate);
            categoryElasticsearchRepository.save(convertToElasticsearch(cate));

            CategoryTranslation categoryTranslation = new CategoryTranslation();
            categoryTranslation.setCategory(cate);
            categoryTranslation.setLanguage(Language.EN);
            categoryTranslation.setDateCreated(LocalDateTime.now());
            categoryTranslation.setIsDeleted(false);
            categoryTranslation.setCateName(supportFunction.convertLanguage(req.getCateName(),Language.EN));
            categoryTranslationRepository.save(categoryTranslation);
            categoryTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(categoryTranslation));


            return ResponseEntity.status(HttpStatus.OK).body(new CRUDCategoryResponse(
                    cate.getCateId(),
                    categoryTranslation.getCateName(),
                    cate.getCateImg(),
                    cate.getIsDeleted(),
                    cate.getDateCreated(),
                    cate.getDateUpdated(),
                    cate.getDateDeleted()
            ));
        }
        return  ResponseEntity.ok().build();

    }

    public ResponseEntity<?> getOneCategory(Integer id, Language language) {
        Category category = categoryRepository.findByCateId(id);
        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not found");
        }
        String name_trans = "";
        if(language == Language.VN)
        {
            name_trans = category.getCateName();

        }
        else if (language == Language.EN) {

            CategoryTranslation categoryTranslation = categoryTranslationRepository.findByCategory_CateIdAndIsDeletedFalse(category.getCateId());
            if(categoryTranslation != null) {
                name_trans = categoryTranslation.getCateName();
            }

        }
        if(name_trans.equals(""))
        {
            name_trans = supportFunction.convertLanguage(category.getCateName(),language);
            CategoryTranslation categoryTranslation = new CategoryTranslation();
            categoryTranslation.setCategory(category);
            categoryTranslation.setLanguage(Language.EN);
            categoryTranslation.setDateCreated(LocalDateTime.now());
            categoryTranslation.setIsDeleted(false);
            categoryTranslation.setCateName(name_trans);
            categoryTranslationRepository.save(categoryTranslation);
            categoryTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(categoryTranslation));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDCategoryResponse(
                category.getCateId(),
                name_trans,
                category.getCateImg(),
                category.getIsDeleted(),
                category.getDateCreated(),
                category.getDateUpdated(),
                category.getDateDeleted()
        ));

    }


    @Transactional
    public ResponseEntity<?> updateCategory(CRUDCategoryRequest req) {

        if(req.getLanguage() == Language.EN)
        {
            Category category = categoryRepository.findByCateIdAndIsDeletedFalse(req.getCateId());
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not found");
            }
            CategoryTranslation category1 = categoryTranslationRepository.findByCateNameAndCategory_CateIdNot(req.getCateName(), req.getCateId());
            if (category1 != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("category name already exists");
            }
            category.setCateName(supportFunction.convertLanguage(req.getCateName(),Language.VN));
            category.setCateImg(category.getCateImg());
            category.setDateUpdated(LocalDateTime.now());
            categoryRepository.save(category);
            categoryElasticsearchRepository.save(convertToElasticsearch(category));
            String catename_trans = "";
            CategoryTranslation categoryTranslation = categoryTranslationRepository.findByCategory_CateIdAndIsDeletedFalse(category.getCateId());
            String name_trans = supportFunction.convertLanguage(req.getCateName(),Language.EN);
            if(categoryTranslation == null)
            {
                CategoryTranslation categoryTranslation1 = new CategoryTranslation();
                categoryTranslation1.setCategory(category);
                categoryTranslation1.setLanguage(Language.EN);
                categoryTranslation1.setDateCreated(LocalDateTime.now());
                categoryTranslation1.setIsDeleted(false);
                categoryTranslation1.setCateName(name_trans);
                categoryTranslationRepository.save(categoryTranslation1);
                categoryTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(categoryTranslation1));
                catename_trans = name_trans;
            }
            else
            {
                categoryTranslation.setCateName(name_trans);
                categoryTranslation.setDateUpdated(LocalDateTime.now());
                categoryTranslationRepository.save(categoryTranslation);
                categoryTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(categoryTranslation));
                catename_trans = req.getCateName();
            }
            return ResponseEntity.status(HttpStatus.OK).body(new CRUDCategoryResponse(
                    req.getCateId(),
                    catename_trans,
                    req.getCateImg(),
                    category.getIsDeleted(),
                    category.getDateCreated(),
                    category.getDateUpdated(),
                    category.getDateDeleted()
            ));

        } else
        {
            Category category = categoryRepository.findByCateIdAndIsDeletedFalse(req.getCateId());
            if (category == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not found");
            }
            Category category1 = categoryRepository.findByCateNameAndCateIdNot(req.getCateName(), req.getCateId());
            if (category1 != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("category already exists");
            }
            LocalDateTime currentDateTime = LocalDateTime.now();
            category.setCateName(supportFunction.convertLanguage(req.getCateName(),Language.VN));
            category.setCateImg(category.getCateImg());
            category.setDateUpdated(LocalDateTime.now());
            categoryRepository.save(category);
            categoryElasticsearchRepository.save(convertToElasticsearch(category));
            String catename_trans = "";
            CategoryTranslation categoryTranslation = categoryTranslationRepository.findByCategory_CateIdAndIsDeletedFalse(category.getCateId());
            if (categoryTranslation != null
            ) {
                String name_trans = supportFunction.convertLanguage(req.getCateName(),Language.EN);
                categoryTranslation.setCateName(name_trans);
                categoryTranslation.setDateUpdated(LocalDateTime.now());
                categoryTranslationRepository.save(categoryTranslation);
                categoryTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(categoryTranslation));
                catename_trans = name_trans;

            }
            if(catename_trans.equals(""))
            {
                String name_trans = supportFunction.convertLanguage(req.getCateName(),Language.EN);
                CategoryTranslation categoryTranslation1 = new CategoryTranslation();
                categoryTranslation1.setCategory(category);
                categoryTranslation1.setLanguage(Language.EN);
                categoryTranslation1.setDateCreated(LocalDateTime.now());
                categoryTranslation1.setIsDeleted(false);
                categoryTranslation1.setCateName(name_trans);
                categoryTranslationRepository.save(categoryTranslation1);
                categoryTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(categoryTranslation1));
                catename_trans = name_trans;
            }
            return ResponseEntity.status(HttpStatus.OK).body(new CRUDCategoryResponse(
                    req.getCateId(),
                    catename_trans,
                    req.getCateImg(),
                    category.getIsDeleted(),
                    category.getDateCreated(),
                    category.getDateUpdated(),
                    category.getDateDeleted()
            ));

        }


    }

    @Transactional
    public ListCategoryResponse listCategory(String pageFromParam, String limitFromParam,Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Category> categoryList = categoryRepository.findAllByIsDeletedFalse(pageable);
        List<Category> categoryList1 = categoryRepository.findAllByIsDeletedFalse();
        List<CRUDCategoryResponse> crudCategoryResponseList = new ArrayList<>();
        int total = 0;
        for (Category category : categoryList) {
            String name_trans = "";
            if(language == Language.VN)
            {
                name_trans = category.getCateName();
            }
            else if (language == Language.EN)
            {
                CategoryTranslation categoryTranslation = categoryTranslationRepository.findByCategory_CateIdAndIsDeletedFalse(category.getCateId());
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
                categoryTranslationElasticsearchRepository.save(convertToElasticsearchTranslation(categoryTranslation1));
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
            total++;
        }
        return new ListCategoryResponse(page, categoryList.getTotalPages(), limit, categoryList1.size(), crudCategoryResponseList);
    }

    @Transactional
    public ResponseEntity<?> getAllProductFromCategory(int id, String pageFromParam, String limitFromParam,Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        Category category = categoryRepository.findByCateIdAndIsDeletedFalse(id);
        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("category not found");
        }
        List<Product> productList1 = List.of();
        Page<Product> productList = productRepository.findByCategory_CateIdAndIsDeletedFalse(id, pageable);
        if(language == Language.VN) {
            productList1 = productRepository.findByCategory_CateIdAndIsDeletedFalse(id);
        }

        List<CRUDProductResponse> crudProductResponseList = new ArrayList<>();
        int total = 0;
        for (Product product1 : productList) {
            List<ProductImageResponse> productImageResponses = new ArrayList<>();
            String currentProImg = product1.getListProImg();
            if (currentProImg != null && !currentProImg.trim().isEmpty()) {
                String[] imageEntries1 = currentProImg.split(", ");
                for (String imageEntry : imageEntries1) {
                    String[] parts = imageEntry.split(": ");  // Phân tách stt và url
                    int stt = Integer.parseInt(parts[0]);      // Lấy số thứ tự hiện tại
                    String url = parts[1];                     // Lấy URL
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
                ProductTranslation productTranslations = productTranslationRepository.findByProduct_ProIdAndIsDeletedFalse(product1.getProId());
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
                productTranslationElasticsearchRepository.save(convertToElasticsearchTranslationProduct(productTranslation));


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

    public static Page<Category> mapToCategoryPage(Page<CategoryElasticsearch> elasticsearchPage, Pageable pageable) {
        List<Category> products = elasticsearchPage.getContent().stream().map(elastic -> {
            Category category = new Category();
            category.setCateId(elastic.getCategoryId());
            category.setCateName(elastic.getCateName());
            category.setCateImg(elastic.getCateImg());
            category.setIsDeleted(elastic.getIsDeleted());
            category.setDateDeleted(elastic.getDateDeleted());
            category.setDateUpdated(elastic.getDateUpdated());
            category.setDateCreated(elastic.getDateCreated());
            return category;
        }).collect(Collectors.toList());

        return new PageImpl<>(products, pageable, elasticsearchPage.getTotalElements());
    }

    public static Page<CategoryTranslation> mapToCategoryTranslationPage(Page<CategoryTranslationElasticsearch> elasticsearchPage, Pageable pageable) {
        List<CategoryTranslation> products = elasticsearchPage.getContent().stream().map(elastic -> {
            CategoryTranslation product = new CategoryTranslation();
            Category category = new Category();
            category.setCateId(elastic.getCategoryId());
            product.setCateTransId(Integer.parseInt(elastic.getId()));
            product.setLanguage(elastic.getLanguage());
            product.setCateName(elastic.getCateName());
            product.setIsDeleted(elastic.getIsDeleted());
            product.setDateDeleted(elastic.getDateDeleted());
            product.setDateUpdated(elastic.getDateUpdated());
            product.setDateCreated(elastic.getDateCreated());
            product.setCategory(category);
            return product;
        }).collect(Collectors.toList());

        return new PageImpl<>(products, pageable, elasticsearchPage.getTotalElements());
    }

    public static List<Category> mapToCategoryList(List<CategoryElasticsearch> elasticsearchList) {
        return elasticsearchList.stream().map(elastic -> {
            Category category = new Category();
            category.setCateId(elastic.getCategoryId());
            category.setCateName(elastic.getCateName());
            category.setCateImg(elastic.getCateImg());
            category.setIsDeleted(elastic.getIsDeleted());
            category.setDateDeleted(elastic.getDateDeleted());
            category.setDateUpdated(elastic.getDateUpdated());
            category.setDateCreated(elastic.getDateCreated());
            return category;
        }).collect(Collectors.toList());
    }

    public static List<CategoryTranslation> mapToCategoryTranslationList(List<CategoryTranslationElasticsearch> elasticsearchList) {
        return elasticsearchList.stream().map(elastic -> {
            CategoryTranslation product = new CategoryTranslation();
            Category category = new Category();
            category.setCateId(elastic.getCategoryId());
            product.setCateTransId(Integer.parseInt(elastic.getId()));
            product.setLanguage(elastic.getLanguage());
            product.setIsDeleted(elastic.getIsDeleted());
            product.setDateDeleted(elastic.getDateDeleted());
            product.setDateUpdated(elastic.getDateUpdated());
            product.setDateCreated(elastic.getDateCreated());
            product.setCategory(category);
            return product;
        }).collect(Collectors.toList());
    }


    @Transactional
    public ResponseEntity<?> totalSearchCategory(String keyword, int page, int limit, Language language) {
        if (limit > 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        List<CRUDCategoryResponse> crudCategoryResponseList = new ArrayList<>();

        Page<?> categoryPage;
        List<?> categoryList1;

        if (language == Language.VN) {
            Page<CategoryElasticsearch> productElasticsearches = categoryElasticsearchRepository.searchByCateName(keyword, pageable);
            categoryPage = mapToCategoryPage(productElasticsearches,pageable);
            List<CategoryElasticsearch> productElasticsearches1 = categoryElasticsearchRepository.searchByCateName(keyword);
            categoryList1 = mapToCategoryList(productElasticsearches1);
        } else if (language == Language.EN) {
            Page<CategoryTranslationElasticsearch> productElasticsearches = categoryTranslationElasticsearchRepository.searchByCateName(keyword, pageable);
            categoryPage = mapToCategoryTranslationPage(productElasticsearches,pageable);
            List<CategoryTranslationElasticsearch> productElasticsearches1 = categoryTranslationElasticsearchRepository.searchByCateName(keyword);
            categoryList1 = mapToCategoryTranslationList(productElasticsearches1);
        } else {
            return ResponseEntity.ok(new TotalSearchCategoryResponse(page, 0, limit, 0, crudCategoryResponseList));

        }

        for (Object obj : categoryPage) {
            if (language == Language.VN) {
                Category category = (Category) obj;
                crudCategoryResponseList.add(new CRUDCategoryResponse(
                        category.getCateId(),
                        category.getCateName(),
                        category.getCateImg(),
                        category.getIsDeleted(),
                        category.getDateCreated(),
                        category.getDateUpdated(),
                        category.getDateDeleted()
                ));
            } else {
                CategoryTranslation categoryTranslation = (CategoryTranslation) obj;
                Category category = categoryRepository.findByCateId(categoryTranslation.getCategory().getCateId());
                crudCategoryResponseList.add(new CRUDCategoryResponse(
                        categoryTranslation.getCategory().getCateId(),
                        categoryTranslation.getCateName(),
                        category.getCateImg(),
                        category.getIsDeleted(),
                        category.getDateCreated(),
                        category.getDateUpdated(),
                        category.getDateDeleted()
                ));
            }
        }

        return ResponseEntity.ok(new TotalSearchCategoryResponse(page, categoryPage.getTotalPages(), limit, categoryList1.size(), crudCategoryResponseList));

    }


    @Transactional
    public ResponseEntity<?> disableCategory(int cateId) {
        Category category = categoryRepository.findByCateId(cateId);
        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found");
        }
        if (category.getIsDeleted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Category already disabled");
        }

        // Fetch all products in the category
        List<Product> productList = productRepository.findByCategory_CateId(cateId);
        if (!productList.isEmpty()) {
            List<Integer> productIds = productList.stream().map(Product::getProId).toList();

            // Fetch all variants for products
            List<ProductVariants> productVariants = productVariantsRepository.findByProduct_ProIdIn(productIds);
            if (!productVariants.isEmpty()) {
                productVariants.forEach(variant -> {
                    variant.setIsDeleted(true);
                    variant.setDateDeleted(LocalDateTime.now());
                });
                productVariantsRepository.saveAll(productVariants);
            }

            // Batch update favourite items
            List<FavouriteItem> favouriteItems = favouriteItemRepository.findByProductVariantsIn(productVariants);
            if (!favouriteItems.isEmpty()) {
                favouriteItems.forEach(item -> {
                    item.setIsDeleted(true);
                    item.setDateDeleted(LocalDateTime.now());
                });
                favouriteItemRepository.saveAll(favouriteItems);
            }

            // Batch update cart items
            List<CartItem> cartItems = cartItemRepository.findByProductVariantsIn(productVariants);
            if (!cartItems.isEmpty()) {
                cartItems.forEach(item -> {
                    item.setIsDisabled(true);
                    item.setDateDeleted(LocalDateTime.now());
                });
                cartItemRepository.saveAll(cartItems);

            }

            List<CartItemGroup> cartItemGroups = cartItemGroupRepository.findByProductVariantsIn(productVariants);
            if (!cartItemGroups.isEmpty()) {
                cartItemGroups.forEach(item -> {
                    item.setIsDisabled(true);
                    item.setDateDeleted(LocalDateTime.now());
                });
                cartItemGroupRepository.saveAll(cartItemGroups);
            }

            // Recalculate cart totals
            List<Cart> carts = cartRepository.findAll();
            carts.forEach(cart -> {
                List<CartItem> activeCartItems = cartItemRepository.findByCart_CartIdAndIsDeletedFalseAndIsDisabledFalse(cart.getCartId());
                double total = activeCartItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
                int quantity = activeCartItems.stream().mapToInt(CartItem::getQuantity).sum();

                cart.setTotalPrice(total);
                cart.setTotalProduct(quantity);
            });
            cartRepository.saveAll(carts);

            // Batch update reviews
            List<Review> reviews = reviewRepository.findAllByProduct_ProIdIn(productIds);
            if (!reviews.isEmpty()) {
                reviews.forEach(review -> {
                    review.setIsDeleted(true);
                    review.setDateDeleted(LocalDateTime.now());
                });
                reviewRepository.saveAll(reviews);
            }

            List<ProductElasticsearch> elasticProducts = new ArrayList<>();
            List<ProductTranslationElasticsearch> elasticTranslations = new ArrayList<>();

            productList.forEach(product -> {
                product.setIsDeleted(true);
                product.setDateDeleted(LocalDateTime.now());

                elasticProducts.add(convertToElasticsearchProduct(product));

                product.getProductTranslations().forEach(translation -> {
                    translation.setIsDeleted(true);
                    translation.setDateDeleted(LocalDateTime.now());

                    elasticTranslations.add(convertToElasticsearchTranslationProduct(translation));
                });
            });

            productRepository.saveAll(productList);
            productTranslationRepository.saveAll(productList.stream()
                    .flatMap(p -> p.getProductTranslations().stream())
                    .toList());

            productElasticsearchRepository.saveAll(elasticProducts);
            productTranslationElasticsearchRepository.saveAll(elasticTranslations);
        }

        List<CategoryTranslation> categoryTranslations = category.getCategoryTranslations();
        if (!categoryTranslations.isEmpty()) {
            categoryTranslations.forEach(item -> {
                item.setIsDeleted(true);
                item.setDateDeleted(LocalDateTime.now());
            });

            categoryTranslationRepository.saveAll(categoryTranslations);

            List<CategoryTranslationElasticsearch> elasticTranslations = categoryTranslations.stream()
                    .map(this::convertToElasticsearchTranslation)
                    .toList();

            // Lưu vào Elasticsearch
            categoryTranslationElasticsearchRepository.saveAll(elasticTranslations);
        }


        List<GroupOrders> groupOrders = groupOrdersRepository.findAllByIsDeletedFalse();


        List<GroupOrderMember> allMembers = new ArrayList<>();
        Set<Integer> allCartIds = new HashSet<>();
        for (GroupOrders groupOrder : groupOrders) {
            List<GroupOrderMember> members = groupOrder.getGroupOrderMembers();
            allMembers.addAll(members);
            for (GroupOrderMember member : members) {
                allCartIds.add(member.getCartGroup().getCartId());
            }
        }


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



        category.setIsDeleted(true);
        category.setDateDeleted(LocalDateTime.now());
        categoryRepository.save(category);
        categoryElasticsearchRepository.save(convertToElasticsearch(category));

        // Trả về response
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDCategoryResponse(
                category.getCateId(),
                category.getCateName(),
                category.getCateImg(),
                category.getIsDeleted(),
                category.getDateCreated(),
                category.getDateUpdated(),
                category.getDateDeleted()
        ));
    }



    @Transactional
    public ResponseEntity<?> enableCategory(int cateId) throws SQLException {

        Category category = categoryRepository.findByCateId(cateId);
        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Category not found");
        }

        if (!category.getIsDeleted()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Category already enabled");
        }

        // Fetch all products in the category
        List<Product> productList = productRepository.findByCategory_CateId(cateId);
        if (!productList.isEmpty()) {
            List<Integer> productIds = productList.stream()
                    .map(Product::getProId)
                    .collect(Collectors.toList());

            // Fetch product variants related to the products
            List<ProductVariants> productVariants = productVariantsRepository.findByProduct_ProIdIn(productIds);
            if (!productVariants.isEmpty()) {
                // Batch update variants
                productVariants.forEach(variant -> {
                    variant.setIsDeleted(false);
                    variant.setDateDeleted(null);
                });
                productVariantsRepository.saveAll(productVariants);

                // Fetch and batch update favourite items
                List<FavouriteItem> favouriteItems = favouriteItemRepository.findByProductVariantsIn(productVariants);
                if (!favouriteItems.isEmpty()) {
                    favouriteItems.forEach(item -> {
                        item.setIsDeleted(false);
                        item.setDateDeleted(null);
                    });
                    favouriteItemRepository.saveAll(favouriteItems);
                }

                // Fetch and batch update cart items
                List<CartItem> cartItems = cartItemRepository.findByProductVariantsIn(productVariants);
                if (!cartItems.isEmpty()) {
                    cartItems.forEach(item -> {
                        item.setIsDisabled(false);
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

            // Recalculate cart totals
            List<Cart> carts = cartRepository.findAll();
            carts.forEach(cart -> {
                List<CartItem> activeCartItems = cartItemRepository.findByCart_CartIdAndIsDeletedFalseAndIsDisabledFalse(cart.getCartId());
                double total = activeCartItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
                int quantity = activeCartItems.stream().mapToInt(CartItem::getQuantity).sum();

                cart.setTotalPrice(total);
                cart.setTotalProduct(quantity);
            });
            cartRepository.saveAll(carts);



            // Batch update reviews
            List<Review> reviews = reviewRepository.findAllByProduct_ProIdIn(productIds);
            if (!reviews.isEmpty()) {
                reviews.forEach(review -> {
                    review.setIsDeleted(false);
                    review.setDateDeleted(null);
                });
                reviewRepository.saveAll(reviews);
            }
            List<ProductElasticsearch> elasticProducts = new ArrayList<>();
            List<ProductTranslationElasticsearch> elasticTranslations = new ArrayList<>();

            productList.forEach(product -> {
                product.setIsDeleted(false);
                product.setDateDeleted(null);

                elasticProducts.add(convertToElasticsearchProduct(product));

                product.getProductTranslations().forEach(translation -> {
                    translation.setIsDeleted(false);
                    translation.setDateDeleted(null);
                    elasticTranslations.add(convertToElasticsearchTranslationProduct(translation));
                });
            });

            // Lưu thay đổi vào MySQL
            productRepository.saveAll(productList);
            productTranslationRepository.saveAll(productList.stream()
                    .flatMap(p -> p.getProductTranslations().stream())
                    .toList());


            productElasticsearchRepository.saveAll(elasticProducts);
            productTranslationElasticsearchRepository.saveAll(elasticTranslations);
        }




        List<CategoryTranslation> categoryTranslations = category.getCategoryTranslations();
        if (!categoryTranslations.isEmpty()) {
            categoryTranslations.forEach(item -> {
                item.setIsDeleted(false);
                item.setDateDeleted(null);
            });

            // Lưu vào database
            categoryTranslationRepository.saveAll(categoryTranslations);

            // Chuyển đổi sang Elasticsearch model
            List<CategoryTranslationElasticsearch> elasticTranslations = categoryTranslations.stream()
                    .map(this::convertToElasticsearchTranslation)
                    .toList();

            // Lưu vào Elasticsearch
            categoryTranslationElasticsearchRepository.saveAll(elasticTranslations);
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



        // Update category
        category.setIsDeleted(false);
        category.setDateDeleted(null);
        categoryRepository.save(category);
        categoryElasticsearchRepository.save(convertToElasticsearch(category));

        return ResponseEntity.status(HttpStatus.OK).body(new CRUDCategoryResponse(
                category.getCateId(),
                category.getCateName(),
                category.getCateImg(),
                category.getIsDeleted(),
                category.getDateCreated(),
                category.getDateUpdated(),
                category.getDateDeleted()
        ));
    }
}