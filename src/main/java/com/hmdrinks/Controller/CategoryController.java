package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Request.CRUDCategoryRequest;
import com.hmdrinks.Request.CreateCategoryRequest;
import com.hmdrinks.Request.EnableCategoryRequest;
import com.hmdrinks.Request.IdReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.Service.CategoryService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/cate")
@RequiredArgsConstructor
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SupportFunction supportFunction;

    @GetMapping(value = "/list-category")
    public ResponseEntity<?> listAllCategory(
            @RequestParam(name = "page") String page,
            @RequestParam(name = "limit") String limit,
            @RequestParam(name = "language")Language language
    ) {
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return ResponseEntity.ok(categoryService.listCategory(page, limit,language));
    }

    @PostMapping(value = "/create-category")
    public ResponseEntity<?> createCategory(@RequestBody @Valid  CreateCategoryRequest req){
        return categoryService.crateCategory(req);
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> updateCategory(@RequestBody @Valid  CRUDCategoryRequest req){

        return categoryService.updateCategory(req);
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<?> getOneCategory( @PathVariable Integer id,@RequestParam Language language){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("id", id);
        if (validation != null) return validation;
        return categoryService.getOneCategory(id,language);
    }

    @GetMapping("/view/{id}/product")
    public ResponseEntity<?> getALLProductFromCategory(@PathVariable Integer id,@RequestParam(name = "page") String page, @RequestParam(name = "limit") String limit,@RequestParam(name = "language")Language language){
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        ResponseEntity<?> validation = supportFunction.validatePositiveId("id", id);
        if (validation != null) return validation;
        if (validationResult != null) {
            return validationResult;
        }
        return categoryService.getAllProductFromCategory(id,page,limit,language);
    }

    @GetMapping(value = "/search")
    public ResponseEntity<?> searchByCategoryName(@RequestParam(name = "keyword") String keyword, @RequestParam(name = "page") int page, @RequestParam(name = "limit") int limit, @RequestParam(name = "language")Language language) {
        ResponseEntity<?> check = supportFunction.validatePositiveIntegers(
                Map.of("page", page, "limit", limit)
        );
        if (check != null) return check;
        return ResponseEntity.ok(categoryService.totalSearchCategory(keyword,page,limit,language));
    }

    @PutMapping(value = "/enable")
    public ResponseEntity<?> enableCategory(@RequestBody @Valid  IdReq req, HttpServletRequest httpRequest) throws Exception {
        return  categoryService.enableCategory(req.getId());
    }

    @PutMapping(value = "/disable")
    public ResponseEntity<?> disableCategory(@RequestBody @Valid  IdReq req, HttpServletRequest httpRequest) {
        return  categoryService.disableCategory(req.getId());
    }

}