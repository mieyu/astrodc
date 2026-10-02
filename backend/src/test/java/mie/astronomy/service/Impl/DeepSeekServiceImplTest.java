package mie.astronomy.service.Impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeepSeekServiceImplTest {

    @Test
    void quotesConfiguredAnalysisTable() {
        assertEquals("`observation_image`.`total`",
            DeepSeekServiceImpl.quoteQualifiedTable("observation_image.total"));
    }

    @Test
    void qualifiesUnqualifiedFromAndJoinReferences() {
        assertEquals(
            "SELECT * FROM `observation_image`.`total`;",
            DeepSeekServiceImpl.qualifyAnalysisTableReferences(
                "SELECT * FROM total;", "observation_image.total"));
        assertEquals(
            "SELECT * FROM x JOIN `observation_image`.`total` t ON t.id = x.id;",
            DeepSeekServiceImpl.qualifyAnalysisTableReferences(
                "SELECT * FROM x JOIN `total` t ON t.id = x.id;", "observation_image.total"));
        assertEquals(
            "SELECT * FROM `observation_image`.`total`;",
            DeepSeekServiceImpl.qualifyAnalysisTableReferences(
                "SELECT * FROM satellite_data_center.total;", "observation_image.total"));
    }

    @Test
    void normalizesAlreadyQualifiedReference() {
        String sql = "SELECT * FROM observation_image.total;";
        assertEquals("SELECT * FROM `observation_image`.`total`;",
            DeepSeekServiceImpl.qualifyAnalysisTableReferences(
                sql, "observation_image.total"));
    }

    @Test
    void rejectsInvalidConfiguredAnalysisTable() {
        assertThrows(IllegalStateException.class,
            () -> DeepSeekServiceImpl.quoteQualifiedTable("total"));
    }
}
