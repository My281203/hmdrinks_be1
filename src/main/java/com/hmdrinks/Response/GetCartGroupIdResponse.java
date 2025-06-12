package com.hmdrinks.Response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
public class GetCartGroupIdResponse {
    private int userId;
    private Integer cartGroupId;
}

