package mie.astronomy.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AnalyzeResponse {
    private String sql;
    private String summary;
    private List<Map<String, Object>> data;
    private String error;
}

