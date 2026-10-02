package mie.astronomy.service.Impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将用户使用的中文天体名称转换为观测库 OBJECT 字段的业务编码。
 */
final class AstronomyObjectCodeResolver {

    private static final List<SystemCode> SYSTEMS = List.of(
        new SystemCode("火星", "Mars", "M", "火卫"),
        new SystemCode("木星", "Jupiter", "J", "木卫"),
        new SystemCode("土星", "Saturn", "S", "土卫"),
        new SystemCode("天王星", "Uranus", "U", "天卫"),
        new SystemCode("海王星", "Neptune", "N", "海卫"),
        new SystemCode("冥王星", "Pluto", "P", "冥卫")
    );
    private static final Pattern OBJECT_PREDICATE = Pattern.compile(
        "(?i)(?:`?[A-Za-z_][A-Za-z0-9_]*`?\\.)?`?OBJECT`?\\s*" +
            "(?:(?:=|LIKE)\\s*N?'[^']*'|IN\\s*\\([^)]*\\))");
    private static final Pattern CLAUSE_BOUNDARY = Pattern.compile(
        "(?i)\\b(?:GROUP\\s+BY|HAVING|ORDER\\s+BY|LIMIT)\\b|;");
    private static final Pattern WHERE_PATTERN = Pattern.compile("(?i)\\bWHERE\\b");

    private AstronomyObjectCodeResolver() {
    }

    static String buildPromptGuidance(String task) {
        ObjectSelection selection = resolve(task);
        String currentTaskRule = selection == null
            ? "当前任务未识别到需要转换的行星或卫星名称。"
            : "当前任务必须使用条件：" + selection.toSqlPredicate();
        return """
            OBJECT 字段使用天体编码，不保存中文名称，禁止对 OBJECT 使用“%%中文名%%”模糊匹配：
            - M0/J0/S0/U0/N0/P0 分别表示火星、木星、土星、天王星、海王星、冥王星本体。
            - Mn/Jn/Sn/Un/Nn/Pn 表示对应行星系统第 n 号卫星。
            - 查询“所有/全部某行星的数据”或“某行星系统”时，用前缀匹配，例如所有土星数据用 OBJECT LIKE 'S%%'。
            - 只查询行星本体时用 0 号编码，例如土星本体用 OBJECT = 'S0'。
            - 查询指定卫星时用准确编码，例如土卫1用 OBJECT = 'S1'、木卫6用 OBJECT = 'J6'。
            - 当前数据库实际出现的天体编码包括 M0/M1、J0/J2/J4/J6-J11、S0/S6-S9 及联合目标、U0、N1/N2、P0。
            - BIAS、FLAT、DARK 是校准帧，不是天体名称。
            %s
            """.formatted(currentTaskRule);
    }

    static String applyResolvedObjectFilter(String sql, String task) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        ObjectSelection selection = resolve(task);
        if (selection == null) {
            return sql;
        }

        String predicate = selection.toSqlPredicate();
        Matcher predicateMatcher = OBJECT_PREDICATE.matcher(sql);
        if (predicateMatcher.find()) {
            return predicateMatcher.replaceAll(Matcher.quoteReplacement(predicate));
        }

        Matcher boundaryMatcher = CLAUSE_BOUNDARY.matcher(sql);
        int insertionPoint = boundaryMatcher.find() ? boundaryMatcher.start() : sql.length();
        String head = sql.substring(0, insertionPoint).stripTrailing();
        String tail = sql.substring(insertionPoint);
        String conjunction = WHERE_PATTERN.matcher(head).find() ? " AND " : " WHERE ";
        return head + conjunction + predicate + (tail.isEmpty() ? "" : " ") + tail;
    }

    static ObjectSelection resolve(String task) {
        if (task == null || task.isBlank()) {
            return null;
        }

        String normalizedTask = task.replaceAll("\\s+", "");
        String lowerTask = normalizedTask.toLowerCase(Locale.ROOT);
        Set<String> prefixes = new LinkedHashSet<>();
        Set<String> exactCodes = new LinkedHashSet<>();

        for (SystemCode system : SYSTEMS) {
            Pattern satellitePattern = Pattern.compile(
                Pattern.quote(system.satelliteChineseName()) + "([0-9]{1,2}|[一二三四五六七八九十]{1,3})");
            Matcher satelliteMatcher = satellitePattern.matcher(normalizedTask);
            while (satelliteMatcher.find()) {
                Integer satelliteNumber = parseSatelliteNumber(satelliteMatcher.group(1));
                if (satelliteNumber != null) {
                    exactCodes.add(system.prefix() + satelliteNumber);
                }
            }

            boolean containsPlanet = normalizedTask.contains(system.chineseName())
                || lowerTask.contains(system.englishName().toLowerCase(Locale.ROOT));
            if (!containsPlanet) {
                continue;
            }

            if (isWholeSystemRequest(normalizedTask, lowerTask, system)) {
                prefixes.add(system.prefix());
                exactCodes.removeIf(code -> code.startsWith(system.prefix()));
            } else {
                exactCodes.add(system.prefix() + "0");
            }
        }

        if (prefixes.isEmpty() && exactCodes.isEmpty()) {
            return null;
        }
        return new ObjectSelection(new ArrayList<>(prefixes), new ArrayList<>(exactCodes));
    }

    private static boolean isWholeSystemRequest(
            String task, String lowerTask, SystemCode system) {
        String chineseName = system.chineseName();
        String englishName = system.englishName().toLowerCase(Locale.ROOT);
        return task.contains(chineseName + "系统")
            || task.contains(chineseName + "及其")
            || task.matches(".*(?:所有|全部|全量).{0,6}" + Pattern.quote(chineseName) + ".*")
            || task.matches(".*" + Pattern.quote(chineseName) + ".{0,3}(?:所有|全部|全量).*")
            || lowerTask.matches(".*(?:all|every).{0,12}" + Pattern.quote(englishName) + ".*");
    }

    private static Integer parseSatelliteNumber(String value) {
        if (value.matches("[0-9]{1,2}")) {
            return Integer.parseInt(value);
        }
        if ("十".equals(value)) return 10;
        int tenIndex = value.indexOf('十');
        if (tenIndex >= 0) {
            int tens = tenIndex == 0 ? 1 : chineseDigit(value.charAt(0));
            int ones = tenIndex == value.length() - 1 ? 0 : chineseDigit(value.charAt(tenIndex + 1));
            return tens > 0 && ones >= 0 ? tens * 10 + ones : null;
        }
        int digit = value.length() == 1 ? chineseDigit(value.charAt(0)) : -1;
        return digit > 0 ? digit : null;
    }

    private static int chineseDigit(char value) {
        return switch (value) {
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            default -> -1;
        };
    }

    record ObjectSelection(List<String> prefixes, List<String> exactCodes) {
        String toSqlPredicate() {
            List<String> conditions = new ArrayList<>();
            for (String prefix : prefixes) {
                conditions.add("`OBJECT` LIKE '" + prefix + "%'");
            }
            if (exactCodes.size() == 1) {
                conditions.add("`OBJECT` = '" + exactCodes.get(0) + "'");
            } else if (!exactCodes.isEmpty()) {
                String values = exactCodes.stream()
                    .map(code -> "'" + code + "'")
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
                conditions.add("`OBJECT` IN (" + values + ")");
            }
            String predicate = String.join(" OR ", conditions).trim();
            return conditions.size() > 1 ? "(" + predicate + ")" : predicate;
        }
    }

    private record SystemCode(
        String chineseName,
        String englishName,
        String prefix,
        String satelliteChineseName) {
    }
}
