package com.hmdrinks.Request;

import com.hmdrinks.Enum.Language;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FilterProductBox {
    private int c;
    List<Integer> p;
    private  int o;
    private  String page;
    private  String limit;
    Language language;
}
