package mie.astronomy.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mie.astronomy.dto.AnalyzeResponse;
import mie.astronomy.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    @Value("${deepseek.analysis-table:observation_image.total}")
    private String analysisTable;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeepSeekServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 判断是否为纯SQL语句
     */
    private boolean isPureSql(String s) {
        String sStrip = s.trim();
        if (sStrip.contains("```")) return false;
        if (sStrip.contains("根据") || sStrip.contains("如下") || 
            sStrip.contains("解释") || sStrip.contains("SQL语句")) return false;
        Pattern pattern = Pattern.compile("^(SELECT|WITH|SHOW)\\b", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(sStrip).find() && sStrip.endsWith(";");
    }

    /**
     * 从markdown中提取SQL
     */
    private String stripMarkdown(String s) {
        s = s.replaceAll("(?i)```sql|```", "").trim();
        Pattern pattern = Pattern.compile("(SELECT|WITH|SHOW)[\\s\\S]*?;", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        return s;
    }

    /**
     * 获取数据库表结构
     */
    private String getTableSchema() {
        try {
            String[] tableParts = splitQualifiedTable(analysisTable);
            String sql = "SELECT COLUMN_NAME AS Field, COLUMN_TYPE AS Type, COLUMN_COMMENT AS Comment " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                sql, tableParts[0], tableParts[1]);
            if (columns.isEmpty()) {
                throw new IllegalStateException("未找到分析表: " + analysisTable);
            }
            
            StringBuilder schema = new StringBuilder();
            for (Map<String, Object> column : columns) {
                String field = (String) column.get("Field");
                String type = (String) column.get("Type");
                String comment = column.get("Comment") != null ? (String) column.get("Comment") : "";
                schema.append(String.format("%s (%s) — %s\n", field, type, comment));
            }
            return schema.toString();
        } catch (Exception e) {
            throw new IllegalStateException("读取分析表结构失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, double temperature) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userPrompt));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode choices = rootNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.get("content") != null) {
                    return message.get("content").asText().trim();
                }
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeek(String prompt, double temperature) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "user", "content", prompt)
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode choices = rootNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null) {
                        return content.asText().trim();
                    }
                }
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 生成SQL
     */
    private String generateSql(String task) {
        String schema = getTableSchema();
        String qualifiedTable = quoteQualifiedTable(analysisTable);
        String objectCodeGuidance = AstronomyObjectCodeResolver.buildPromptGuidance(task);
        String basePrompt = String.format(
            "你是SQL生成器。严格遵守：\n" +
            "1) 只输出可执行MySQL语句；不得含说明或markdown；\n" +
            "2) 只使用表 %s，并始终使用这个完整表名。\n" +
            "3) %s\n" +
            "任务：%s\n" +
            "表结构：\n%s\n",
            qualifiedTable, objectCodeGuidance, task, schema
        );

        String sql = callDeepSeek(basePrompt, 0.0);
        if (!isPureSql(sql)) {
            sql = stripMarkdown(sql);
        }
        if (!sql.trim().endsWith(";")) {
            sql += ";";
        }
        sql = qualifyAnalysisTableReferences(sql, analysisTable);
        return AstronomyObjectCodeResolver.applyResolvedObjectFilter(sql, task);
    }

    static String[] splitQualifiedTable(String qualifiedTable) {
        if (qualifiedTable == null || !qualifiedTable.matches("^[A-Za-z0-9_]+\\.[A-Za-z0-9_]+$")) {
            throw new IllegalStateException("deepseek.analysis-table 必须使用 database.table 格式");
        }
        return qualifiedTable.split("\\.", 2);
    }

    static String quoteQualifiedTable(String qualifiedTable) {
        String[] parts = splitQualifiedTable(qualifiedTable);
        return "`" + parts[0] + "`.`" + parts[1] + "`";
    }

    /**
     * 兼容模型偶尔忽略提示、仍返回 FROM total/JOIN total 的情况。
     */
    static String qualifyAnalysisTableReferences(String sql, String qualifiedTable) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        String[] parts = splitQualifiedTable(qualifiedTable);
        Pattern unqualifiedReference = Pattern.compile(
            "(?i)\\b(FROM|JOIN)\\s+(?:`?[A-Za-z0-9_]+`?\\.)?`?" +
                Pattern.quote(parts[1]) + "`?(?=\\s|;|$)");
        Matcher matcher = unqualifiedReference.matcher(sql);
        StringBuffer result = new StringBuffer();
        String quotedTable = quoteQualifiedTable(qualifiedTable);
        while (matcher.find()) {
            matcher.appendReplacement(
                result, Matcher.quoteReplacement(matcher.group(1) + " " + quotedTable));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 执行SQL并返回结果
     */
    private List<Map<String, Object>> executeSql(String sql) {
        try {
            // 只允许SELECT和SHOW语句
            String sqlUpper = sql.trim().toUpperCase();
            if (!sqlUpper.startsWith("SELECT") && !sqlUpper.startsWith("SHOW")) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("SQL执行错误: " + e.getMessage());
        }
    }

    /**
     * 总结结果
     */
    private String summarizeResult(List<Map<String, Object>> data, String task) {
        if (data == null || data.isEmpty()) {
            return "无数据返回。";
        }

        // 生成数据预览（前20条），模拟pandas的to_string格式
        StringBuilder preview = new StringBuilder();
        int count = Math.min(20, data.size());
        
        // 获取表头（所有key）
        if (!data.isEmpty()) {
            Map<String, Object> firstRow = data.get(0);
            String header = String.join("  ", firstRow.keySet());
            preview.append(header).append("\n");
            
            // 添加分隔线
            String separator = new String(new char[Math.max(50, header.length())]).replace('\0', '-');
            preview.append(separator).append("\n");
            
            // 添加数据行
            for (int i = 0; i < count; i++) {
                Map<String, Object> row = data.get(i);
                String rowStr = String.join("  ", 
                    row.values().stream()
                       .map(v -> v == null ? "" : v.toString())
                       .toArray(String[]::new));
                preview.append(rowStr).append("\n");
            }
        }

        String prompt = String.format(
            "你是科研数据库分析助手。\n" +
            "根据任务\"%s\"和以下数据结果，请用中文总结关键发现：\n" +
            "其中BIAS、FLAT、DARK列表示图像校正类型，其他列含义请结合表结构自行理解。\n" +
            "%s\n",
            task, preview.toString()
        );

        return callDeepSeek(prompt, 0.3);
    }

    @Override
    public AnalyzeResponse analyze(String task) {
        AnalyzeResponse response = new AnalyzeResponse();
        try {
            // Step 1: 生成SQL
            String sql = generateSql(task);
            response.setSql(sql);

            // Step 2: 执行SQL
            List<Map<String, Object>> data;
            try {
                data = executeSql(sql);
            } catch (Exception e) {
                response.setError(e.getMessage());
                response.setSummary("SQL 执行错误：" + e.getMessage());
                response.setData(Collections.emptyList());
                return response;
            }

            // Step 3: 总结结果
            String summary = summarizeResult(data, task);
            response.setSummary(summary);
            response.setData(data);

            return response;
        } catch (Exception e) {
            response.setError(e.getMessage());
            response.setSql("");
            response.setSummary("处理请求时发生错误: " + e.getMessage());
            response.setData(Collections.emptyList());
            return response;
        }
    }
}
