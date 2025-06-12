package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Repository.*;
import com.hmdrinks.RepositoryElasticsearch.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElasticsearchSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchSyncService.class);

    private final ProductRepository productRepository;
    private final ProductElasticsearchRepository productElasticsearchRepository;
    private final ProductTranslationRepository productTranslationRepository;
    private final ProductTranslationElasticsearchRepository productTranslationElasticsearchRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryElasticsearchRepository categoryElasticsearchRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final CategoryTranslationElasticsearchRepository categoryTranslationElasticsearchRepository;
    private final ProductVariantsRepository productVariantsRepository;
    private final ProductVariantsElasticsearchRepository productVariantsElasticsearchRepository;
    private  final UserRepository userRepository;
    private final UserElasticsearchRepository userElasticsearchRepository;


    public void syncProducts() {
        try {
            List<ProductElasticsearch> products = productRepository.findAll().stream()
                    .map(this::convertToElasticsearch)
                    .collect(Collectors.toList());

            productElasticsearchRepository.saveAll(products);
            logger.info("✅ Đồng bộ Product thành công! - Tổng số: {}", products.size());
        } catch (Exception e) {
            logger.error("❌ Lỗi khi đồng bộ Product: {}", e.getMessage());
        }
    }


    public void syncProductTranslations() {
        try {
            List<ProductTranslationElasticsearch> translations = productTranslationRepository.findAll().stream()
                    .map(this::convertToElasticsearch)
                    .collect(Collectors.toList());

            productTranslationElasticsearchRepository.saveAll(translations);
            logger.info("✅ Đồng bộ Product Translation thành công! - Tổng số: {}", translations.size());
        } catch (Exception e) {
            logger.error("❌ Lỗi khi đồng bộ Product Translation: {}", e.getMessage());
        }
    }


    public void syncCategories() {
        try {
            List<CategoryElasticsearch> categories = categoryRepository.findAll().stream()
                    .map(this::convertToElasticsearch)
                    .collect(Collectors.toList());

            categoryElasticsearchRepository.saveAll(categories);
            logger.info("✅ Đồng bộ Category thành công! - Tổng số: {}", categories.size());
        } catch (Exception e) {
            logger.error("❌ Lỗi khi đồng bộ Category: {}", e.getMessage());
        }
    }

    public void syncUser()
    {
        try {
            List<UserDocument> categories = userRepository.findAll().stream()
                    .map(this::convertToElasticsearch)
                    .collect(Collectors.toList());

            userElasticsearchRepository.saveAll(categories);
            logger.info("✅ Đồng bộ User thành công! - Tổng số: {}", categories.size());
        } catch (Exception e) {
            logger.error("❌ Lỗi khi đồng bộ User: {}", e.getMessage());
        }
    }


    public void syncCategoryTranslations() {
        try {
            List<CategoryTranslationElasticsearch> translations = categoryTranslationRepository.findAll().stream()
                    .map(this::convertToElasticsearch)
                    .collect(Collectors.toList());

            categoryTranslationElasticsearchRepository.saveAll(translations);
            logger.info("✅ Đồng bộ Category Translation thành công! - Tổng số: {}", translations.size());
        } catch (Exception e) {
            logger.error("❌ Lỗi khi đồng bộ Category Translation: {}", e.getMessage());
        }
    }


    public void syncProductVariants() {
        try {
            List<ProductVariantsElasticsearch> variants = productVariantsRepository.findAll().stream()
                    .map(this::convertToElasticsearch)
                    .collect(Collectors.toList());

            productVariantsElasticsearchRepository.saveAll(variants);
            logger.info("✅ Đồng bộ Product Variants thành công! - Tổng số: {}", variants.size());
        } catch (Exception e) {
            logger.error("❌ Lỗi khi đồng bộ Product Variants: {}", e.getMessage());
        }
    }


    public void scheduledSync() {
        logger.info("🔄 Bắt đầu đồng bộ dữ liệu...");
        syncProducts();
        syncProductTranslations();
        syncCategories();
        syncCategoryTranslations();
        syncProductVariants();
        syncUser();
        logger.info("✅ Đồng bộ hoàn tất!");
    }


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

    private  UserDocument convertToElasticsearch(User user) {
        return new UserDocument(
                user.getUserId(),
                user.getType(),
                user.getUserName(),
                user.getEmail(),
                user.getRole(),
                user.getSex(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getBirthDate(),
                user.getAvatar(),
                user.getCity(),
                user.getDistrict(),
                user.getWard(),
                user.getStreet(),
                user.getIsDeleted(),
                user.getDateUpdated(),
                user.getDateCreated()

        );
    }

    private ProductTranslationElasticsearch convertToElasticsearch(ProductTranslation translation) {
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

    private CategoryTranslationElasticsearch convertToElasticsearch(CategoryTranslation translation) {
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

    private ProductVariantsElasticsearch convertToElasticsearch(ProductVariants variant) {
        return new ProductVariantsElasticsearch(
                String.valueOf(variant.getVarId()),
                variant.getProduct().getProId(),
                variant.getSize(),
                variant.getPrice(),
                variant.getStock(),
                variant.getIsDeleted(),
                variant.getDateCreated(),
                variant.getDateUpdated(),
                variant.getDateDeleted()
        );
    }

    public List<ProductElasticsearch> searchProducts(String keyword) {
        List<ProductElasticsearch> results = productElasticsearchRepository.findByProNameContainingOrDescriptionContainingAndIsDeletedFalse(keyword,keyword);
        System.out.println("🔍 Tìm thấy " + results.size() + " sản phẩm với từ khóa: " + keyword);

        for (ProductElasticsearch product : results) {
            System.out.println("📌 " + product.getProName() + " - " + product.getDescription());
        }

        return results;
    }

    @Scheduled(fixedRate = 5400000)
    public void testSyncAndSearch() {
        scheduledSync();
//
//        List<ProductElasticsearch> products = searchProducts("trà");
//        // Hiển thị kết quả tìm kiếm
//        products.forEach(product ->
//                System.out.println("✅ Sản phẩm: " + product.getProName() + " | Mô tả: " + product.getDescription())
//        );

    }




}
