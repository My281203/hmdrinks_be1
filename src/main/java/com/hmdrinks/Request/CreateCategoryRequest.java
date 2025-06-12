package com.hmdrinks.Request;

import com.hmdrinks.Enum.Language;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryRequest {
    private String cateName;
    private String cateImg;
    Language language;
}
