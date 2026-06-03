package mie.astronomy.dto;

import lombok.Data;

import java.util.List;

@Data
public class CatalogBiasCorrectionQuery {
    private Long ipixMin;
    private Long ipixMax;

    private List<String> columns;
}
