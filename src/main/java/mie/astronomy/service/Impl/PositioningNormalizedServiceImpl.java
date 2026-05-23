package mie.astronomy.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import mie.astronomy.dto.PositioningNormalizedQuery;
import mie.astronomy.entity.PositioningNormalized;
import mie.astronomy.mapper.PositioningNormalizedMapper;
import mie.astronomy.service.PositioningNormalizedService;
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
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PositioningNormalizedServiceImpl implements PositioningNormalizedService {

    private static final Logger log = LoggerFactory.getLogger(PositioningNormalizedServiceImpl.class);

    /** 排序列白名单。前端传入不在表内列名时回落到 jd1。 */
    private static final Set<String> SORT_WHITELIST = Set.of(
            "global_id", "target_id", "jd1", "jd_tt1", "obs_type", "obs_site",
            "coord_status", "oc_horizon_icrs_w1", "oc_horizon_w2",
            "oc_nss_icrs_w1", "oc_nss_icrs_w2"
    );

    /** options 接口字段白名单。严格防 SQL 注入。 */
    private static final Set<String> OPTIONS_WHITELIST = Set.of(
            "obs_type", "coord_status", "target_id", "obs_site",
            "time_scale", "epoch", "coordinates", "reference",
            "centre_of_frame", "receptor"
    );

    /**
     * 全部 51 列(导出表头默认顺序)。与 entity 字段顺序、XML allColumns 顺序保持一致。
     * 这里是 DB 列名(snake_case 含混合大小写),CSV 表头直接输出该名。
     */
    private static final List<String> ALL_COLUMNS = List.of(
            "global_id", "target_id", "relative_to", "jd1", "jd2", "jd_tt1", "jd_tt2",
            "time_scale", "epoch", "obs_type", "w1", "w2", "w1_unit", "w2_unit",
            "coordinates", "reference", "centre_of_frame", "obs_site", "observer_pos",
            "receptor", "telescope", "reduction", "coord_route", "coord_status", "coord_note",
            "w1_icrs", "w2_icrs", "ra_A_icrs", "dec_A_icrs", "ra_B_horizons", "dec_B_horizons",
            "w1_theory", "w2_theory", "w1_nss", "w2_nss", "w1_nss_icrs", "w2_nss_icrs",
            "oc_horizon_icrs_w1", "oc_horizon_w2", "oc_nss_native_w1", "oc_nss_native_w2",
            "oc_nss_icrs_w1", "oc_nss_icrs_w2", "dRA_star_cosdec_arcsec", "dDEC_star_arcsec",
            "source_web_html", "source_web_txt", "source_local_html", "source_local_txt",
            "created_at", "updated_at"
    );

    /**
     * DB 列名 → Java 字段名(camelCase)。entity 已改为 camelCase 字段,这里给出反向映射。
     */
    private static final Map<String, String> COL_TO_FIELD = Map.<String, String>ofEntries(
            Map.entry("global_id", "globalId"),
            Map.entry("target_id", "targetId"),
            Map.entry("relative_to", "relativeTo"),
            Map.entry("jd1", "jd1"),
            Map.entry("jd2", "jd2"),
            Map.entry("jd_tt1", "jdTt1"),
            Map.entry("jd_tt2", "jdTt2"),
            Map.entry("time_scale", "timeScale"),
            Map.entry("epoch", "epoch"),
            Map.entry("obs_type", "obsType"),
            Map.entry("w1", "w1"),
            Map.entry("w2", "w2"),
            Map.entry("w1_unit", "w1Unit"),
            Map.entry("w2_unit", "w2Unit"),
            Map.entry("coordinates", "coordinates"),
            Map.entry("reference", "reference"),
            Map.entry("centre_of_frame", "centreOfFrame"),
            Map.entry("obs_site", "obsSite"),
            Map.entry("observer_pos", "observerPos"),
            Map.entry("receptor", "receptor"),
            Map.entry("telescope", "telescope"),
            Map.entry("reduction", "reduction"),
            Map.entry("coord_route", "coordRoute"),
            Map.entry("coord_status", "coordStatus"),
            Map.entry("coord_note", "coordNote"),
            Map.entry("w1_icrs", "w1Icrs"),
            Map.entry("w2_icrs", "w2Icrs"),
            Map.entry("ra_A_icrs", "raAIcrs"),
            Map.entry("dec_A_icrs", "decAIcrs"),
            Map.entry("ra_B_horizons", "raBHorizons"),
            Map.entry("dec_B_horizons", "decBHorizons"),
            Map.entry("w1_theory", "w1Theory"),
            Map.entry("w2_theory", "w2Theory"),
            Map.entry("w1_nss", "w1Nss"),
            Map.entry("w2_nss", "w2Nss"),
            Map.entry("w1_nss_icrs", "w1NssIcrs"),
            Map.entry("w2_nss_icrs", "w2NssIcrs"),
            Map.entry("oc_horizon_icrs_w1", "ocHorizonIcrsW1"),
            Map.entry("oc_horizon_w2", "ocHorizonW2"),
            Map.entry("oc_nss_native_w1", "ocNssNativeW1"),
            Map.entry("oc_nss_native_w2", "ocNssNativeW2"),
            Map.entry("oc_nss_icrs_w1", "ocNssIcrsW1"),
            Map.entry("oc_nss_icrs_w2", "ocNssIcrsW2"),
            Map.entry("dRA_star_cosdec_arcsec", "dRAStarCosdecArcsec"),
            Map.entry("dDEC_star_arcsec", "dDECStarArcsec"),
            Map.entry("source_web_html", "sourceWebHtml"),
            Map.entry("source_web_txt", "sourceWebTxt"),
            Map.entry("source_local_html", "sourceLocalHtml"),
            Map.entry("source_local_txt", "sourceLocalTxt"),
            Map.entry("created_at", "createdAt"),
            Map.entry("updated_at", "updatedAt")
    );

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PositioningNormalizedMapper mapper;

    @Override
    public IPage<PositioningNormalized> search(PositioningNormalizedQuery query,
                                               long page, long pageSize,
                                               String sortField, String sortOrder) {
        Page<PositioningNormalized> p = new Page<>(page, pageSize);

        QueryWrapper<PositioningNormalized> qw = buildWrapper(query);

        String sortCol = (sortField != null && SORT_WHITELIST.contains(sortField)) ? sortField : "jd1";
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if (asc) {
            qw.orderByAsc(sortCol);
        } else {
            qw.orderByDesc(sortCol);
        }

        return mapper.selectPage(p, qw);
    }

    @Override
    public List<String> options(String field) {
        if (field == null || !OPTIONS_WHITELIST.contains(field)) {
            return Collections.emptyList();
        }
        // SQL 中 reference 需要反引号,其他列名直接用即可。这里通过白名单校验后,
        // 在 XML 里用 ${fieldName} 拼接;reference 单独处理为 `reference`。
        String safeField = "reference".equals(field) ? "`reference`" : field;
        return mapper.selectDistinctValues(safeField);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportCsv(PositioningNormalizedQuery query, OutputStream out) {
        List<String> cols = (query.getColumns() == null || query.getColumns().isEmpty())
                ? ALL_COLUMNS
                : query.getColumns().stream()
                    .filter(ALL_COLUMNS::contains)   // 防止前端传非法列名
                    .toList();

        if (cols.isEmpty()) {
            cols = ALL_COLUMNS;
        }

        // 预取反射 Field 对象(用 DB 列名 → Java 字段名映射查找),避免循环内重复反射
        Map<String, Field> fieldMap = new HashMap<>();
        for (String c : cols) {
            String javaName = COL_TO_FIELD.get(c);
            if (javaName == null) {
                log.warn("CSV 导出: 未知 DB 列 {}, 将输出空值", c);
                continue;
            }
            try {
                Field f = PositioningNormalized.class.getDeclaredField(javaName);
                f.setAccessible(true);
                fieldMap.put(c, f);
            } catch (NoSuchFieldException e) {
                log.warn("CSV 导出: 实体类未找到字段 {} (映射自列 {}), 将输出空值", javaName, c);
            }
        }

        try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             Cursor<PositioningNormalized> cursor = mapper.streamByQuery(query)) {

            // BOM + 表头(让 Excel 用 UTF-8 打开时不乱码)
            w.write('\uFEFF');
            for (int i = 0; i < cols.size(); i++) {
                if (i > 0) w.write(',');
                w.write(escape(cols.get(i)));
            }
            w.write("\r\n");

            int rowCount = 0;
            for (PositioningNormalized row : cursor) {
                for (int i = 0; i < cols.size(); i++) {
                    if (i > 0) w.write(',');
                    Field f = fieldMap.get(cols.get(i));
                    Object v = null;
                    if (f != null) {
                        try {
                            v = f.get(row);
                        } catch (IllegalAccessException e) {
                            // 不会发生:已 setAccessible(true)
                        }
                    }
                    w.write(escape(formatValue(v)));
                }
                w.write("\r\n");
                rowCount++;
                // 每若干行 flush 一次,平衡内存与 IO
                if ((rowCount & 0x3FF) == 0) {
                    w.flush();
                }
            }
            w.flush();
            log.info("CSV 导出完成: {} 行, {} 列", rowCount, cols.size());
        } catch (IOException e) {
            log.error("CSV 导出 IO 异常", e);
            throw new IllegalStateException("CSV 导出失败: " + e.getMessage(), e);
        }
    }

    // -------- helpers --------

    private QueryWrapper<PositioningNormalized> buildWrapper(PositioningNormalizedQuery q) {
        QueryWrapper<PositioningNormalized> qw = new QueryWrapper<>();
        if (q == null) {
            return qw;
        }
        if (notBlank(q.getTarget_id())) {
            qw.eq("target_id", q.getTarget_id());
        }
        if (notBlank(q.getObs_type())) {
            qw.eq("obs_type", q.getObs_type());
        }
        if (notBlank(q.getCoord_status())) {
            qw.eq("coord_status", q.getCoord_status());
        }
        if (notBlank(q.getObs_site())) {
            qw.like("obs_site", q.getObs_site());
        }
        if (q.getJd1Min() != null) {
            qw.ge("jd1", q.getJd1Min());
        }
        if (q.getJd1Max() != null) {
            qw.le("jd1", q.getJd1Max());
        }
        return qw;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * 将值格式化为 CSV 单元格(未转义)。
     * - null  -> ""
     * - Double/Float -> BigDecimal.toPlainString,避免科学计数法,保留原精度
     * - LocalDateTime -> yyyy-MM-dd HH:mm:ss
     * - 其他 -> toString
     */
    private static String formatValue(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) return d.toString();
            return new BigDecimal(Double.toString(d)).toPlainString();
        }
        if (v instanceof Float f) {
            if (f.isNaN() || f.isInfinite()) return f.toString();
            return new BigDecimal(Float.toString(f)).toPlainString();
        }
        if (v instanceof LocalDateTime ldt) {
            return ldt.format(TS_FMT);
        }
        return v.toString();
    }

    /**
     * RFC 4180 转义:含 , " \r \n 时整体加双引号,内部 " 转 ""
     */
    private static String escape(String s) {
        if (s == null) return "";
        boolean need = s.indexOf(',') >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!need) return s;
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') sb.append('"').append('"');
            else sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }
}
