package mie.astronomy.service.Impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogBiasCorrectionServiceImplTest {

    @Test
    void normalizePageKeepsPageAtLeastOne() {
        assertEquals(1, CatalogBiasCorrectionServiceImpl.normalizePage(0));
        assertEquals(1, CatalogBiasCorrectionServiceImpl.normalizePage(-5));
        assertEquals(3, CatalogBiasCorrectionServiceImpl.normalizePage(3));
    }

    @Test
    void normalizePageSizeDefaultsAndCapsAtOneThousand() {
        assertEquals(50, CatalogBiasCorrectionServiceImpl.normalizePageSize(0));
        assertEquals(50, CatalogBiasCorrectionServiceImpl.normalizePageSize(-1));
        assertEquals(500, CatalogBiasCorrectionServiceImpl.normalizePageSize(500));
        assertEquals(1000, CatalogBiasCorrectionServiceImpl.normalizePageSize(5000));
    }

    @Test
    void resolveSortFieldFallsBackToIpixForUnknownColumns() {
        assertEquals("ipix", CatalogBiasCorrectionServiceImpl.resolveSortField(null));
        assertEquals("ipix", CatalogBiasCorrectionServiceImpl.resolveSortField("not_a_column"));
        assertEquals("gaiadr2_dra_mas", CatalogBiasCorrectionServiceImpl.resolveSortField("gaiadr2_dra_mas"));
    }

    @Test
    void csvValuesUsePlainNumbersAndRfcEscaping() {
        assertEquals("", CatalogBiasCorrectionServiceImpl.formatValue(null));
        assertEquals("0.0000123", CatalogBiasCorrectionServiceImpl.formatValue(0.0000123d));
        assertEquals("\"a,b\"", CatalogBiasCorrectionServiceImpl.escape("a,b"));
        assertEquals("\"a\"\"b\"", CatalogBiasCorrectionServiceImpl.escape("a\"b"));
    }
}
