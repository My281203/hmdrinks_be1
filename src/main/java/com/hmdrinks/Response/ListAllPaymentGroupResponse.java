package com.hmdrinks.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class ListAllPaymentGroupResponse {

        private int page;
        private int totalPages;
        private int limit;
        private long totalItems;
        private List<CRUDPaymentGroupResponse> data;
    }

