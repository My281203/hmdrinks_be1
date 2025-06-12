package com.hmdrinks.Controller;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Type_Post;
import com.hmdrinks.Request.CRUDPostReq;
import com.hmdrinks.Request.CreateNewPostReq;
import com.hmdrinks.Request.DeleteProductImgReq;
import com.hmdrinks.Request.IdReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.Service.PostService;
import com.hmdrinks.SupportFunction.SupportFunction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import scala.language;

@CrossOrigin
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/post")
public class PostController {
    @Autowired
    private PostService postService;
    @Autowired
    private SupportFunction supportFunction;

    @PostMapping(value = "/create")
    public ResponseEntity<?> createPost(@RequestBody @Valid  CreateNewPostReq req, HttpServletRequest httpRequest){
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return postService.createPost(req);
    }

    @GetMapping(value ="/view/{id}")
    public ResponseEntity<?> getOnePost(
            @PathVariable Integer id,
            @RequestParam Language language
    ){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("id", id);
        if (validation != null) return validation;
        return ResponseEntity.ok(postService.getPostById(id,language));
    }

    @GetMapping(value = "/view/all")
    public ResponseEntity<?> getAllPosts(@RequestParam(name = "page") String page,
                                                           @RequestParam(name = "limit") String limit,
                                                           @RequestParam(name = "language") Language language){
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return  ResponseEntity.ok(postService.getAllPost(page,limit,language));
    }

    @GetMapping(value = "/view/all/desc")
    public ResponseEntity<?> getAllPostsDESC(@RequestParam(name = "page") String page,
                                                           @RequestParam(name = "limit") String limit,
                                                               @RequestParam(name = "language") Language language){
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return  ResponseEntity.ok(postService.getAllPostByDESC(page,limit,language));
    }

    @GetMapping(value = "/view/type/all")
    public ResponseEntity<?> getAllPostsByTye(@RequestParam(name = "page") String page,
                                                                @RequestParam(name = "limit") String limit,
                                                                @RequestParam(name = "type")Type_Post typePost,
                                                                @RequestParam(name = "language") Language language){
        ResponseEntity<?> validationResult = supportFunction.validatePaginationParams(page, limit);

        if (validationResult != null) {
            return validationResult;
        }
        return  ResponseEntity.ok(postService.getAllPostByType(page,limit,typePost,language));
    }

    @GetMapping(value = "/view/author/{userId}")
    public ResponseEntity<?> getOnePostByUserId(
            @PathVariable Integer userId,
            @RequestParam Language language
    ){
        ResponseEntity<?> validation = supportFunction.validatePositiveId("userId", userId);
        if (validation != null) return validation;
        return  postService.listAllPostByUserId(userId,language);
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> updatePost(
            @RequestBody @Valid  CRUDPostReq req, HttpServletRequest httpRequest
    )
    {
        ResponseEntity<?> authResponse = supportFunction.checkUserAuthorization(httpRequest, req.getUserId());

        if (!authResponse.getStatusCode().equals(HttpStatus.OK)) {
            return authResponse;
        }
        return  postService.updatePost(req);
    }

    @PutMapping(value = "/enable")
    public ResponseEntity<?> enablePost(@RequestBody @Valid  IdReq req, HttpServletRequest httpRequest) {
        return  postService.enablePost(req.getId());
    }

    @PutMapping(value = "/disable")
    public ResponseEntity<?> disablePost(@RequestBody @Valid  IdReq req, HttpServletRequest httpRequest) {
        return  postService.disablePost(req.getId());
    }

    @DeleteMapping(value = "/image/deleteOne")
    public ResponseEntity<?> deleteAllItem(@RequestBody @Valid  IdReq req, HttpServletRequest httpRequest) {
        return postService.deleteImageFromPost(req.getId());
    }
}