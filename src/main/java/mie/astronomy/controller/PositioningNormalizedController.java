package mie.astronomy.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import mie.astronomy.common.Result;
import mie.astronomy.dto.PositioningNormalizedQuery;
import mie.astronomy.entity.PositioningNormalized;
import mie.astronomy.service.PositioningNormalizedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/positioning/normalized")
public class PositioningNormalizedController {

    private static final Logger log = LoggerFactory.getLogger(PositioningNormalizedController.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private PositioningNormalizedService service;

    /**
     * 列表查询。Body 全部可选,空值跳过。返回 records 包含全部 51 列。
     */
    @PostMapping("/search")
    public Result<Map<String, Object>> search(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long pageSize,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestBody(required = false) PositioningNormalizedQuery body) {

        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 50;
        if (pageSize > 1000) pageSize = 1000;

        PositioningNormalizedQuery q = body != null ? body : new PositioningNormalizedQuery();
        IPage<PositioningNormalized> p = service.search(q, page, pageSize, sortField, sortOrder);

        Map<String, Object> data = new HashMap<>(4);
        data.put("records", p.getRecords());
        data.put("total", p.getTotal());
        data.put("current", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 列候选值。field 必须在 service 白名单内,否则返回空数组。
     */
    @GetMapping("/options")
    public Result<List<String>> options(@RequestParam(required = false) String field) {
        return Result.success(service.options(field));
    }

    /**
     * CSV 流式导出。不分页;按筛选全量导出。
     * 注意:不能用 Result 包裹,直接写响应体。异常时尝试改写状态码为 500。
     */
    @PostMapping("/export")
    public void export(@RequestBody(required = false) PositioningNormalizedQuery body,
                       HttpServletResponse response) throws IOException {
        PositioningNormalizedQuery q = body != null ? body : new PositioningNormalizedQuery();

        String filename = "positioning_normalized_" + LocalDateTime.now().format(TS_FMT) + ".csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType("text/csv; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        // streaming: 关闭 Spring 默认的 content-length 缓存
        response.setHeader("Cache-Control", "no-cache");

        try {
            OutputStream out = response.getOutputStream();
            service.exportCsv(q, out);
        } catch (Exception e) {
            log.error("CSV 导出失败", e);
            // 响应已开始无法切换到 JSON,这里尽量写一段错误信息让前端能看到
            if (!response.isCommitted()) {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json; charset=utf-8");
                response.getWriter().write(
                        "{\"code\":0,\"message\":\"导出失败: "
                                + e.getMessage().replace("\"", "'") + "\"}");
            }
        }
    }
}
