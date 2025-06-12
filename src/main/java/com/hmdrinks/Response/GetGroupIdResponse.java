package com.hmdrinks.Response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
public class GetGroupIdResponse {
    private int userId;
    private Integer groupId;
}

