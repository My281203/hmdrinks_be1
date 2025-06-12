package com.hmdrinks.Request;

import com.hmdrinks.Enum.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsertItemToFavourite {
    @Min(value = 1, message = "UserId phải lớn hơn 0")
    private int userId;
    @Min(value = 1, message = "FavId phải lớn hơn 0")
    private int favId;
    @Min(value = 1, message = "ProId phải lớn hơn 0")
    private int proId;
    private Size size;
}
