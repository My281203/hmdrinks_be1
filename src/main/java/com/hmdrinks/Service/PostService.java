package com.hmdrinks.Service;

import com.hmdrinks.Entity.*;
import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Role;
import com.hmdrinks.Enum.Status_Voucher;
import com.hmdrinks.Enum.Type_Post;
import com.hmdrinks.Repository.PostRepository;
import com.hmdrinks.Repository.PostTranSlationRepository;
import com.hmdrinks.Repository.UserRepository;
import com.hmdrinks.Repository.VoucherRepository;
import com.hmdrinks.Request.CRUDPostReq;
import com.hmdrinks.Request.CreateNewPostReq;
import com.hmdrinks.Response.*;
import com.hmdrinks.SupportFunction.SupportFunction;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VoucherRepository voucherRepository;
    @Autowired
    private SupportFunction supportFunction;
    @Autowired
    private PostTranSlationRepository postTranSlationRepository;

    public ResponseEntity<?> createPost(CreateNewPostReq req) {
       User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
       if(user == null) {
           return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
       }
       if(user.getRole() != Role.ADMIN)
       {
           return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not allowed to create a post");
       }
       if(req.getLanguage() == Language.VN)
       {
           LocalDateTime currentDate = LocalDateTime.now();
           Post post = new Post();
           post.setTitle(supportFunction.convertLanguage(req.getTitle(), Language.VN));
           post.setDescription(supportFunction.convertLanguage(req.getDescription(), Language.VN));
           post.setUser(user);
           post.setType(req.getTypePost());
           post.setBannerUrl(req.getUrl());
           post.setShortDes(supportFunction.convertLanguage(req.getShortDescription(), Language.VN));
           post.setIsDeleted(false);
           post.setDateCreate(currentDate);
           postRepository.save(post);

           String short_translation = supportFunction.convertLanguage(req.getShortDescription(), Language.EN);
           String des_translation = supportFunction.convertLanguage(req.getDescription(), Language.EN);
           PostTranslation postTranslation = new PostTranslation();
           postTranslation.setPost(post);
           postTranslation.setLanguage(Language.EN);
           postTranslation.setDescription(des_translation);
           postTranslation.setTitle(supportFunction.convertLanguage(req.getTitle(), Language.EN));
           postTranslation.setShortDes(short_translation);
           postTranslation.setDateCreate(currentDate);
           postTranslation.setIsDeleted(Boolean.FALSE);

           postTranSlationRepository.save(postTranslation);

           return ResponseEntity.status(HttpStatus.OK).body(new CRUDPostResponse(
                   post.getPostId(),
                   post.getType(),
                   post.getBannerUrl(),
                   post.getDescription(),
                   post.getTitle(),
                   post.getShortDes(),
                   post.getUser().getUserId(),
                   post.getIsDeleted(),
                   post.getDateDeleted(),
                   post.getDateCreate()
           ));
       } else if (req.getLanguage() == Language.EN) {
           String short_translation = supportFunction.convertLanguage(req.getShortDescription(), Language.VN);
           String des_translation = supportFunction.convertLanguage(req.getDescription(), Language.VN);
           LocalDateTime currentDate = LocalDateTime.now();
           Post post = new Post();
           post.setTitle(supportFunction.convertLanguage(req.getTitle(), Language.VN));
           post.setDescription(des_translation);
           post.setUser(user);
           post.setType(req.getTypePost());
           post.setBannerUrl(req.getUrl());
           post.setShortDes(short_translation);
           post.setIsDeleted(false);
           post.setDateCreate(currentDate);
           postRepository.save(post);


           PostTranslation postTranslation = new PostTranslation();
           postTranslation.setPost(post);
           postTranslation.setLanguage(Language.EN);
           postTranslation.setDescription(supportFunction.convertLanguage(req.getDescription(), Language.EN));
           postTranslation.setShortDes(supportFunction.convertLanguage(req.getShortDescription(), Language.EN));
           postTranslation.setTitle(supportFunction.convertLanguage(req.getTitle(), Language.EN));
           postTranslation.setDateCreate(currentDate);
           postTranslation.setIsDeleted(Boolean.FALSE);

           postTranSlationRepository.save(postTranslation);


           return ResponseEntity.status(HttpStatus.OK).body(new CRUDPostResponse(
                   post.getPostId(),
                   post.getType(),
                   post.getBannerUrl(),
                   postTranslation.getDescription(),
                   postTranslation.getTitle(),
                   postTranslation.getShortDes(),
                   post.getUser().getUserId(),
                   post.getIsDeleted(),
                   post.getDateDeleted(),
                   post.getDateCreate()
           ));
       }

       return  ResponseEntity.ok().build();

    }
    public ResponseEntity<?> getPostById(int postId,Language language) {
        Post post= postRepository.findByPostIdAndIsDeletedFalse(postId);
        if(post == null) {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found post");
        }
        String title = "";
        String description = "";
        String shortDes = "";

        if (language == Language.EN) {
            PostTranslation postTranslation = postTranSlationRepository.findByPost_PostIdAndIsDeletedFalse(postId);
            if(postTranslation != null)
            {
                title = postTranslation.getTitle();
                description = postTranslation.getDescription();
                shortDes = postTranslation.getShortDes();
            }
            else
            {
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

        } else if (language == Language.VN) {
            title = post.getTitle();
            description = post.getDescription();
            shortDes = post.getShortDes();
        }
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDPostResponse(
                post.getPostId(),
                post.getType(),
                post.getBannerUrl(),
                description,
                title,
                shortDes,
                post.getUser().getUserId(),
                post.getIsDeleted(),
                post.getDateDeleted(),
                post.getDateCreate()
        ));
    }

    @Transactional
    public ResponseEntity<?> updatePost(CRUDPostReq req) {
        Post post = postRepository.findByPostIdAndIsDeletedFalse(req.getPostId());
        if(post == null) {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found post");
        }
        User user = userRepository.findByUserIdAndIsDeletedFalse(req.getUserId());
        if(user == null) {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }
        if(user.getRole() != Role.ADMIN)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not allowed to update a post");
        }
        if(req.getLanguage() == Language.VN) {
            post.setTitle(req.getTitle());
            post.setDescription(req.getDescription());
            post.setShortDes(req.getShortDescription());
            post.setBannerUrl(req.getUrl());
            post.setType(req.getTypePost());
            postRepository.save(post);

            List<PostTranslation> postTranslation = post.getPostTranslations();
            for(PostTranslation postTranslation1 : postTranslation) {
                if(postTranslation1 != null)
                {
                    postTranslation1.setShortDes(supportFunction.convertLanguage(post.getShortDes(), Language.EN));
                    postTranslation1.setDescription(supportFunction.convertLanguage(post.getDescription(), Language.EN));
                    postTranslation1.setTitle(supportFunction.convertLanguage(post.getTitle(), Language.EN));
                    postTranSlationRepository.save(postTranslation1);
                }
            }

            return ResponseEntity.status(HttpStatus.OK).body(new CRUDPostResponse(
                    post.getPostId(),
                    post.getType(),
                    post.getBannerUrl(),
                    post.getDescription(),
                    post.getTitle(),
                    post.getShortDes(),
                    post.getUser().getUserId(),
                    post.getIsDeleted(),
                    post.getDateDeleted(),
                    post.getDateCreate()
            ));
        } else if (req.getLanguage() == Language.EN) {

            post.setTitle(supportFunction.convertLanguage(req.getTitle(), Language.VN));
            post.setDescription(supportFunction.convertLanguage(req.getDescription(), Language.VN));
            post.setShortDes(supportFunction.convertLanguage(req.getShortDescription(), Language.VN));
            post.setBannerUrl(req.getUrl());
            post.setType(req.getTypePost());
            postRepository.save(post);

            List<PostTranslation> postTranslation = post.getPostTranslations();
            for(PostTranslation postTranslation1 : postTranslation) {
                if(postTranslation1 != null)
                {
                    postTranslation1.setShortDes(supportFunction.convertLanguage(req.getShortDescription(), Language.EN));
                    postTranslation1.setDescription(supportFunction.convertLanguage(req.getDescription(), Language.EN));
                    postTranslation1.setTitle(supportFunction.convertLanguage(req.getTitle(), Language.EN));
                    postTranSlationRepository.save(postTranslation1);
                }
            }

            return ResponseEntity.status(HttpStatus.OK).body(new CRUDPostResponse(
                    post.getPostId(),
                    post.getType(),
                    post.getBannerUrl(),
                    req.getDescription(),
                    req.getTitle(),
                    req.getShortDescription(),
                    post.getUser().getUserId(),
                    post.getIsDeleted(),
                    post.getDateDeleted(),
                    post.getDateCreate()
            ));
        }

        return  ResponseEntity.ok().build();
    }


    @Transactional
    public ResponseEntity<?> getAllPostByType(String pageFromParam, String limitFromParam, Type_Post typePost,Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Post> posts = postRepository.findAllByTypeAndIsDeletedFalse(typePost, pageable);
        List<Post> posts1 = postRepository.findAllByTypeAndIsDeletedFalse(typePost);
        List<CRUDPostAndVoucherResponse> responses = new ArrayList<>();
        long total = posts.getTotalElements(); // Lấy tổng số bài viết

        for (Post post : posts) {
            String title = "";
            String description = "";
            String shortDes = "";
            Voucher voucher = post.getVoucher();
            if (language == Language.EN) {
                PostTranslation postTranslation = postTranSlationRepository.findByPost_PostIdAndIsDeletedFalse(post.getPostId());
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

            CRUDVoucherResponse voucherResponse = (voucher != null) ? new CRUDVoucherResponse(
                    voucher.getVoucherId(),
                    voucher.getKey(),
                    voucher.getNumber(),
                    voucher.getStartDate(),
                    voucher.getEndDate(),
                    voucher.getDiscount(),
                    voucher.getStatus(),
                    voucher.getPost().getPostId()
            ) : null;

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
                    voucherResponse
            ));
        }

        return ResponseEntity.ok(new ListAllPostResponse(
                page,
                posts.getTotalPages(),
                limit,
                posts1.size(), // Tổng số bài viết
                responses
        ));

    }

    public ResponseEntity<?> getAllPostByDESC(String pageFromParam, String limitFromParam,Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Integer.parseInt(limitFromParam);
        if (limit >= 100) limit = 100;
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Post> posts = postRepository.findAllByIsDeletedFalseOrderByPostIdDesc(pageable);
        List<Post> posts1 = postRepository.findAllByIsDeletedFalseOrderByPostIdDesc();
        List<CRUDPostAndVoucherResponse> responses = new ArrayList<>();
        int total = 0;
        for(Post post : posts) {
            Voucher voucher = post.getVoucher();
            String title = "";
            String description = "";
            String shortDes = "";

            if (language == Language.EN) {
                PostTranslation postTranslation = postTranSlationRepository.findByPost_PostIdAndIsDeletedFalse(post.getPostId());
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
            // Kiểm tra nếu có voucher thì trả về thông tin voucher, nếu không có thì voucher là null
            CRUDVoucherResponse voucherResponse = (voucher != null) ? new CRUDVoucherResponse(
                    voucher.getVoucherId(),
                    voucher.getKey(),
                    voucher.getNumber(),
                    voucher.getStartDate(),
                    voucher.getEndDate(),
                    voucher.getDiscount(),
                    voucher.getStatus(),
                    voucher.getPost().getPostId()
            ) : null;
            responses.add(new CRUDPostAndVoucherResponse(
                    post.getPostId(),
                    post.getType(),
                    post.getBannerUrl(),
                    description, title,
                    shortDes,
                    post.getUser().getUserId(),
                    post.getIsDeleted(),
                    post.getDateDeleted(),
                    post.getDateCreate(),
                    voucherResponse
            ));
            total++;
        }
        return ResponseEntity.ok(new ListAllPostResponse(
                page,
                posts.getTotalPages(),
                limit,
                posts1.size(), // Tổng số bài viết
                responses
        ));
    }



    public interface PostProjection {
        Integer getPostId();
        Type_Post getType();
        String getBannerUrl();
        String getTitle();
        String getDescription();
        String getShortDes();
        LocalDateTime getDateCreate();
        Date getDateDeleted();
        Boolean getIsDeleted();

        // User
        Integer getUserId();

        // Voucher
        Integer getVoucherId();
        String getKey();
        Integer getNumber();
        LocalDateTime getStartDate();
        LocalDateTime getEndDate();
        Double getDiscount();
        Status_Voucher getStatus();


    }


    public ResponseEntity<?> getAllPost(String pageFromParam, String limitFromParam, Language language) {
        int page = Integer.parseInt(pageFromParam);
        int limit = Math.min(Integer.parseInt(limitFromParam), 100);
        Pageable pageable = PageRequest.of(page - 1, limit);

        int offset = (page - 1) * limit;
        Page<PostProjection> pageData = postRepository.findAllPostNative(String.valueOf(language), pageable);




        List<CRUDPostAndVoucherResponse> responses = pageData.getContent().stream()
                .map(p -> {
                    CRUDVoucherResponse voucher = p.getVoucherId() != null ? new CRUDVoucherResponse(
                            p.getVoucherId(), p.getKey(), p.getNumber(),
                            p.getStartDate(), p.getEndDate(), p.getDiscount(), p.getStatus(), p.getPostId()
                    ) : null;

                    return new CRUDPostAndVoucherResponse(
                            p.getPostId(), p.getType(), p.getBannerUrl(),
                            p.getDescription(), p.getTitle(), p.getShortDes(),
                            p.getUserId(), p.getIsDeleted(),
                            p.getDateDeleted(), p.getDateCreate(),
                            voucher
                    );
                })
                .collect(Collectors.toList());



        return ResponseEntity.ok(new ListAllPostResponse(
                page, pageData.getTotalPages(), limit,
                (int) pageData.getTotalElements(), responses
        ));
    }


    public ResponseEntity<?> listAllPostByUserId(int userId,Language language) {
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId);
        if(user == null) {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found user");
        }
        List<Post> posts = postRepository.findByUserUserIdAndIsDeletedFalse(userId);
        List<CRUDPostResponse> responses = new ArrayList<>();
        for(Post post : posts) {
            String title = "";
            String description = "";
            String shortDes = "";
            Voucher voucher = post.getVoucher();
            if (language == Language.EN) {
                PostTranslation postTranslation = postTranSlationRepository.findByPost_PostIdAndIsDeletedFalse(post.getPostId());
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
            responses.add(new CRUDPostResponse(
                    post.getPostId(),
                    post.getType(),
                    post.getBannerUrl(),
                    description,
                    title,
                    shortDes,
                    post.getUser().getUserId(),
                    post.getIsDeleted(),
                    post.getDateDeleted(),
                    post.getDateCreate()
            ));
        }
        return ResponseEntity.status(HttpStatus.OK).body(new ListAllPostByUserIdResponse(userId,posts.size(), responses));
    }

    @Transactional
    public  ResponseEntity<?> disablePost(int postId)
    {
        Post post = postRepository.findByPostId(postId);
        if(post == null) {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found post");
        }
        if(post.getIsDeleted())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Post already disabled");
        }
        Voucher voucher = voucherRepository.findByPostPostId(postId);
        if(voucher!= null)
        {
            voucher.setIsDeleted(true);
            voucher.setDateDeleted(LocalDateTime.now());
            voucher.setStatus(Status_Voucher.EXPIRED);
            voucherRepository.save(voucher);
        }

        List<PostTranslation> postTranslations = post.getPostTranslations();
        for(PostTranslation postTranslation : postTranslations)
        {
            postTranslation.setIsDeleted(true);
            postTranslation.setDateDeleted(LocalDateTime.now());
            postTranSlationRepository.save(postTranslation);
        }
        post.setIsDeleted(true);
        post.setDateDeleted(Date.valueOf(LocalDate.now()));
        postRepository.save(post);

        return ResponseEntity.status(HttpStatus.OK).body(new CRUDPostResponse(
                post.getPostId(),
                post.getType(),
                post.getBannerUrl(),
                post.getDescription(),
                post.getTitle(),
                post.getShortDes(),
                post.getUser().getUserId(),
                post.getIsDeleted(),
                post.getDateDeleted(),
                post.getDateCreate()
        ));
    }
    @Transactional
    public  ResponseEntity<?> enablePost(int postId)
    {
        Post post = postRepository.findByPostId(postId);

        if(post == null) {
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found post");
        }
        if(!post.getIsDeleted())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Post already enable");
        }
        Voucher voucher = voucherRepository.findByPostPostId(postId);
        if(voucher!= null)
        {
            voucher.setIsDeleted(false);
            voucher.setDateDeleted(null);
            voucher.setStatus(Status_Voucher.ACTIVE);
            voucherRepository.save(voucher);
        }
        post.setIsDeleted(false);
        post.setDateDeleted(null);
        postRepository.save(post);
        List<PostTranslation> postTranslations = post.getPostTranslations();
        for(PostTranslation postTranslation : postTranslations)
        {
            postTranslation.setIsDeleted(false);
            postTranslation.setDateDeleted(null);
            postTranSlationRepository.save(postTranslation);
        }
        return ResponseEntity.status(HttpStatus.OK).body(new CRUDPostResponse(
                post.getPostId(),
                post.getType(),
                post.getBannerUrl(),
                post.getDescription(),
                post.getTitle(),
                post.getShortDes(),
                post.getUser().getUserId(),
                post.getIsDeleted(),
                post.getDateDeleted(),
                post.getDateCreate()
        ));
    }

    @Transactional
    public ResponseEntity<?> deleteImageFromPost(int postId ) {
        Post post = postRepository.findByPostIdAndIsDeletedFalse(postId);
        if (post == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("post not exists");
        }
        String currentProImg = post.getBannerUrl();
        if (currentProImg == null || currentProImg.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No images found for this product.");
        }
        post.setBannerUrl("");
        return ResponseEntity.status(HttpStatus.OK).body("Deleted successfully");
    }


}
