package mie.astronomy.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CatalogBiasCorrectionColumns {

    public static final List<CatalogDef> CATALOGS = List.of(
            new CatalogDef("ac", "AC"),
            new CatalogDef("acrs", "ACRS"),
            new CatalogDef("act", "ACT"),
            new CatalogDef("agk1", "AGK1"),
            new CatalogDef("agk3", "AGK3"),
            new CatalogDef("fk4catalogue", "FK4 Catalogue"),
            new CatalogDef("gaiadr1", "Gaia DR1"),
            new CatalogDef("gaiadr2", "Gaia DR2"),
            new CatalogDef("gsc1_2", "GSC 1.2"),
            new CatalogDef("hipparcos", "Hipparcos"),
            new CatalogDef("ppm", "PPM"),
            new CatalogDef("sao", "SAO"),
            new CatalogDef("tycho2", "Tycho-2"),
            new CatalogDef("ucac2", "UCAC2"),
            new CatalogDef("ucac4", "UCAC4"),
            new CatalogDef("usno_a2", "USNO-A2"),
            new CatalogDef("yale", "Yale")
    );

    public static final List<ColumnGroup> GROUPS = buildGroups();
    public static final List<String> ALL_COLUMNS = buildAllColumns();
    public static final Map<String, String> COL_TO_FIELD = buildColumnToFieldMap();

    private static final Set<String> COLUMN_SET = Set.copyOf(ALL_COLUMNS);

    private CatalogBiasCorrectionColumns() {
    }

    public static boolean isKnownColumn(String column) {
        return column != null && COLUMN_SET.contains(column);
    }

    public static boolean isSortableColumn(String column) {
        return isKnownColumn(column);
    }

    public static List<String> sanitizeColumns(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            return ALL_COLUMNS;
        }
        LinkedHashSet<String> safeColumns = new LinkedHashSet<>();
        for (String column : columns) {
            if (isKnownColumn(column)) {
                safeColumns.add(column);
            }
        }
        if (safeColumns.isEmpty()) {
            return ALL_COLUMNS;
        }
        return List.copyOf(safeColumns);
    }

    private static List<ColumnGroup> buildGroups() {
        List<ColumnGroup> groups = new ArrayList<>();
        for (CatalogDef catalog : CATALOGS) {
            groups.add(new ColumnGroup(catalog.key(), catalog.label(), List.of(
                    catalog.key() + "_dra_mas",
                    catalog.key() + "_ddec_mas",
                    catalog.key() + "_pmra_masyr",
                    catalog.key() + "_pmdec_masyr"
            )));
        }
        return List.copyOf(groups);
    }

    private static List<String> buildAllColumns() {
        List<String> columns = new ArrayList<>();
        columns.add("ipix");
        for (ColumnGroup group : GROUPS) {
            columns.addAll(group.props());
        }
        return List.copyOf(columns);
    }

    private static Map<String, String> buildColumnToFieldMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String column : ALL_COLUMNS) {
            map.put(column, toJavaFieldName(column));
        }
        return Map.copyOf(map);
    }

    private static String toJavaFieldName(String column) {
        StringBuilder name = new StringBuilder();
        boolean uppercaseNext = false;
        for (int i = 0; i < column.length(); i++) {
            char c = column.charAt(i);
            if (c == '_') {
                uppercaseNext = true;
                continue;
            }
            if (uppercaseNext) {
                name.append(Character.toUpperCase(c));
                uppercaseNext = false;
            } else {
                name.append(c);
            }
        }
        return name.toString();
    }

    public record CatalogDef(String key, String label) {
    }

    public record ColumnGroup(String key, String label, List<String> props) {
    }
}
