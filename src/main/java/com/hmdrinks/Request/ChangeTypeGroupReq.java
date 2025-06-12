package com.hmdrinks.Request;

import com.hmdrinks.Enum.TypeGroupOrder;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeTypeGroupReq {
    private TypeGroupOrder typeGroupOrder;
    @Min(value = 1, message = "GroupId phải lớn hơn 0")
    private Integer groupId;
    @Min(value = 1, message = "leaderId phải lớn hơn 0")
    private  Integer leaderId;
}
