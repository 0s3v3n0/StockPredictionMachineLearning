package com.isaac.stock.representation.model;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties
public class StockStream {
    private Meta meta;
    private List<TimeFrame> timeFrameList;
}
