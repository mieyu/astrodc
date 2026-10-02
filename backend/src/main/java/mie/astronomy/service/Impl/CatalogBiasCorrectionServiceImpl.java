package mie.astronomy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import mie.astronomy.common.CatalogBiasCorrectionColumns;
import mie.astronomy.dto.CatalogBiasCorrectionQuery;
import mie.astronomy.entity.CatalogBiasCorrection;
import mie.astronomy.mapper.CatalogBiasCorrectionMapper;
import mie.astronomy.service.CatalogBiasCorrectionService;
import org.apache.ibatis.cursor.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogBiasCorrectionServiceImpl implements CatalogBiasCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(CatalogBiasCorrectionServiceImpl.class);

    @Autowired
    private CatalogBiasCorrectionMapper mapper;

    @Override
    public IPage<Map<String, Object>> search(CatalogBiasCorrectionQuery query,
                                             long page, long pageSize,
                                             String sortField, String sortOrder) {
        Page<Map<String, Object>> p = new Page<>(normalizePage(page), normalizePageSize(pageSize));
        QueryWrapper<CatalogBiasCorrection> qw = buildWrapper(query);
        qw.select(CatalogBiasCorrectionColumns.ALL_COLUMNS.toArray(String[]::new));

        String sortCol = resolveSortField(sortField);
        if ("asc".equalsIgnoreCase(sortOrder)) {
            qw.orderByAsc(sortCol);
        } else {
            qw.orderByDesc(sortCol);
        }

        return mapper.selectMapsPage(p, qw);
    }

    @Override
    public Map<String, Object> columns() {
        Map<String, Object> data = new HashMap<>(2);
        data.put("columns", CatalogBiasCorrectionColumns.ALL_COLUMNS.stream()
                .map(this::columnMeta)
                .toList());
        data.put("groups", CatalogBiasCorrectionColumns.GROUPS);
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public void exportCsv(CatalogBiasCorrectionQuery query, OutputStream out) {
        List<String> cols = CatalogBiasCorrectionColumns.sanitizeColumns(
                query == null ? null : query.getColumns()
        );

        try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             Cursor<Map<String, Object>> cursor = mapper.streamByQuery(query)) {

            w.write('\uFEFF');
            writeHeader(w, cols);

            int rowCount = 0;
            for (Map<String, Object> row : cursor) {
                for (int i = 0; i < cols.size(); i++) {
                    if (i > 0) {
                        w.write(',');
                    }
                    w.write(escape(formatValue(row.get(cols.get(i)))));
                }
                w.write("\r\n");
                rowCount++;
                if ((rowCount & 0x3FF) == 0) {
                    w.flush();
                }
            }
            w.flush();
            log.info("星表偏差修正表 CSV 导出完成: {} 行, {} 列", rowCount, cols.size());
        } catch (IOException e) {
            log.error("星表偏差修正表 CSV 导出 IO 异常", e);
            throw new IllegalStateException("CSV 导出失败: " + e.getMessage(), e);
        }
    }

    static long normalizePage(long page) {
        return page < 1 ? 1 : page;
    }

    static long normalizePageSize(long pageSize) {
        if (pageSize < 1) {
            return 50;
        }
        return Math.min(pageSize, 1000);
    }

    static String resolveSortField(String sortField) {
        return CatalogBiasCorrectionColumns.isSortableColumn(sortField) ? sortField : "ipix";
    }

    static String formatValue(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) {
                return d.toString();
            }
            return new BigDecimal(Double.toString(d)).toPlainString();
        }
        if (v instanceof Float f) {
            if (f.isNaN() || f.isInfinite()) {
                return f.toString();
            }
            return new BigDecimal(Float.toString(f)).toPlainString();
        }
        return v.toString();
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        boolean need = s.indexOf(',') >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!need) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append('"').append('"');
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private QueryWrapper<CatalogBiasCorrection> buildWrapper(CatalogBiasCorrectionQuery q) {
        QueryWrapper<CatalogBiasCorrection> qw = new QueryWrapper<>();
        if (q == null) {
            return qw;
        }
        if (q.getIpixMin() != null) {
            qw.ge("ipix", q.getIpixMin());
        }
        if (q.getIpixMax() != null) {
            qw.le("ipix", q.getIpixMax());
        }
        return qw;
    }

    private Map<String, Object> columnMeta(String prop) {
        Map<String, Object> meta = new HashMap<>(4);
        meta.put("prop", prop);
        meta.put("label", prop);
        meta.put("sortable", CatalogBiasCorrectionColumns.isSortableColumn(prop));
        meta.put("minWidth", "ipix".equals(prop) ? 100 : 150);
        return meta;
    }

    private void writeHeader(Writer w, List<String> cols) throws IOException {
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) {
                w.write(',');
            }
            w.write(escape(cols.get(i)));
        }
        w.write("\r\n");
    }
}
