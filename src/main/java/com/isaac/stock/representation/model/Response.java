package com.isaac.stock.representation.model;

import lombok.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties
public class Response {
    private Meta meta;
    private Map<String, Map<String,String>> items = new LinkedHashMap<>();

    @JsonAnySetter
    void setItems(String key, Map<String,String> value) {
        items.put(key, value);
    }

}
