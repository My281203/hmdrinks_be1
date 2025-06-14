package com.hmdrinks.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ListAllGroupOrderCompletedResponse {
    private int currentPage;
    private long totalPages;
    private int limit;
    private int total;
    List<getGroupOrderResponse> listGroup;
}
