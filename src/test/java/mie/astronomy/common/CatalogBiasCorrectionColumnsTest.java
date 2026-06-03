package mie.astronomy.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogBiasCorrectionColumnsTest {

    @Test
    void allColumnsMatchCsvShape() {
        assertEquals(69, CatalogBiasCorrectionColumns.ALL_COLUMNS.size());
        assertEquals("ipix", CatalogBiasCorrectionColumns.ALL_COLUMNS.get(0));
        assertTrue(CatalogBiasCorrectionColumns.ALL_COLUMNS.contains("gaiadr2_pmdec_masyr"));
        assertTrue(CatalogBiasCorrectionColumns.ALL_COLUMNS.contains("usno_a2_dra_mas"));
    }

    @Test
    void catalogGroupsExposeSeventeenCatalogsWithFourBiasColumnsEach() {
        assertEquals(17, CatalogBiasCorrectionColumns.GROUPS.size());

        CatalogBiasCorrectionColumns.ColumnGroup gaiadr2 = CatalogBiasCorrectionColumns.GROUPS.stream()
                .filter(group -> "gaiadr2".equals(group.key()))
                .findFirst()
                .orElseThrow();

        assertEquals("Gaia DR2", gaiadr2.label());
        assertEquals(List.of(
                "gaiadr2_dra_mas",
                "gaiadr2_ddec_mas",
                "gaiadr2_pmra_masyr",
                "gaiadr2_pmdec_masyr"
        ), gaiadr2.props());
    }

    @Test
    void sanitizeColumnsKeepsKnownColumnsInRequestedOrder() {
        List<String> columns = CatalogBiasCorrectionColumns.sanitizeColumns(List.of(
                "gaiadr2_dra_mas",
                "not_a_column",
                "ipix",
                "gaiadr2_dra_mas"
        ));

        assertEquals(List.of("gaiadr2_dra_mas", "ipix"), columns);
    }

    @Test
    void nullOrEmptyExportColumnsFallsBackToAllColumns() {
        assertEquals(CatalogBiasCorrectionColumns.ALL_COLUMNS, CatalogBiasCorrectionColumns.sanitizeColumns(null));
        assertEquals(CatalogBiasCorrectionColumns.ALL_COLUMNS, CatalogBiasCorrectionColumns.sanitizeColumns(List.of()));
    }

    @Test
    void sortableColumnsMustBeKnownColumns() {
        assertTrue(CatalogBiasCorrectionColumns.isSortableColumn("ipix"));
        assertTrue(CatalogBiasCorrectionColumns.isSortableColumn("ucac4_pmra_masyr"));
        assertFalse(CatalogBiasCorrectionColumns.isSortableColumn("not_a_column"));
        assertFalse(CatalogBiasCorrectionColumns.isSortableColumn(null));
    }
}
