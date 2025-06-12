package com.hmdrinks.Request;

import com.hmdrinks.Enum.Language;
import com.hmdrinks.Enum.Type_Post;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Data
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CRUDPostReq {
    @Min(value = 1, message = "postId phải lớn hơn 0")
    private int postId;
    private String url;
    private String description;
    private String title;
    private String shortDescription;
    private Type_Post typePost;
    @Min(value = 1, message = "userId phải lớn hơn 0")
    private int userId;
    private Language language;
}
