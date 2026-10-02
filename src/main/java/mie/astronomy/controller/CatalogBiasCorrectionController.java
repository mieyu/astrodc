package mie.astronomy.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import mie.astronomy.common.Result;
import mie.astronomy.dto.CatalogBiasCorrectionQuery;
import mie.astronomy.service.CatalogBiasCorrectionService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/catalog-bias-correction")
public class CatalogBiasCorrectionController {

    private static final Logger log = LoggerFactory.getLogger(CatalogBiasCorrectionController.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private CatalogBiasCorrectionService service;

    @PostMapping("/search")
    public Result<Map<String, Object>> search(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long pageSize,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestBody(required = false) CatalogBiasCorrectionQuery body) {

        CatalogBiasCorrectionQuery q = body != null ? body : new CatalogBiasCorrectionQuery();
        IPage<Map<String, Object>> p = service.search(q, page, pageSize, sortField, sortOrder);

        Map<String, Object> data = new HashMap<>(4);
        data.put("records", p.getRecords());
        data.put("total", p.getTotal());
        data.put("current", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    @GetMapping("/columns")
    public Result<Map<String, Object>> columns() {
        return Result.success(service.columns());
    }

    @PostMapping("/export")
    public void export(@RequestBody(required = false) CatalogBiasCorrectionQuery body,
                       HttpServletResponse response) throws IOException {
        CatalogBiasCorrectionQuery q = body != null ? body : new CatalogBiasCorrectionQuery();

        String filename = "catalog_bias_correction_" + LocalDateTime.now().format(TS_FMT) + ".csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType("text/csv; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        response.setHeader("Cache-Control", "no-cache");

        try {
            OutputStream out = response.getOutputStream();
            service.exportCsv(q, out);
        } catch (Exception e) {
            log.error("星表偏差修正表 CSV 导出失败", e);
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
