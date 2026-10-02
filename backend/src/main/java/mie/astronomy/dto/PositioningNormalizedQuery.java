package mie.astronomy.dto;

import lombok.Data;

import java.util.List;

/**
 * /api/positioning/normalized/search 与 /export 共用的请求体。
 * 字段名严格保持与前端约定一致(target_id 等用 snake_case, jd1Min/jd1Max 用驼峰)。
 * 全部字段允许为空,空值在 SQL 构建时跳过。
 */
@Data
public class PositioningNormalizedQuery {
    private String target_id;
    private Double jd1Min;
    private Double jd1Max;
    private String obs_type;
    private String obs_site;
    private String coord_status;

    /** 仅 export 接口使用:导出列白名单,为空表示导出全部列 */
    private List<String> columns;
}
