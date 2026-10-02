package mie.astronomy.service.Impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstronomyObjectCodeResolverTest {

    @Test
    void mapsWholeSaturnSystemToPrefixQuery() {
        String sql = "SELECT * FROM `observation_image`.`total` WHERE `OBJECT` LIKE '%土星%';";

        assertEquals(
            "SELECT * FROM `observation_image`.`total` WHERE `OBJECT` LIKE 'S%';",
            AstronomyObjectCodeResolver.applyResolvedObjectFilter(
                sql, "给我所有土星的数据"));
    }

    @Test
    void distinguishesPlanetFromSpecificSatellite() {
        assertEquals("`OBJECT` = 'S0'",
            AstronomyObjectCodeResolver.resolve("查询土星本体的数据").toSqlPredicate());
        assertEquals("`OBJECT` = 'S1'",
            AstronomyObjectCodeResolver.resolve("查询土卫1的数据").toSqlPredicate());
        assertEquals("`OBJECT` = 'J6'",
            AstronomyObjectCodeResolver.resolve("木卫6有哪些观测").toSqlPredicate());
    }

    @Test
    void injectsObjectFilterWhenModelOmitsIt() {
        assertEquals(
            "SELECT COUNT(*) FROM `observation_image`.`total` WHERE `OBJECT` LIKE 'S%' ORDER BY `DATE-OBS`;",
            AstronomyObjectCodeResolver.applyResolvedObjectFilter(
                "SELECT COUNT(*) FROM `observation_image`.`total` ORDER BY `DATE-OBS`;",
                "统计全部土星数据"));
    }

    @Test
    void supportsMultipleSpecificSatellites() {
        assertEquals("`OBJECT` IN ('S1', 'S8')",
            AstronomyObjectCodeResolver.resolve("比较土卫1和土卫8").toSqlPredicate());
    }

    @Test
    void supportsEveryPlanetarySystemPresentInDatabase() {
        assertEquals("`OBJECT` LIKE 'J%'",
            AstronomyObjectCodeResolver.resolve("全部木星数据").toSqlPredicate());
        assertEquals("`OBJECT` = 'U0'",
            AstronomyObjectCodeResolver.resolve("天王星本体").toSqlPredicate());
        assertEquals("`OBJECT` = 'N2'",
            AstronomyObjectCodeResolver.resolve("海卫2观测").toSqlPredicate());
        assertEquals("`OBJECT` LIKE 'M%'",
            AstronomyObjectCodeResolver.resolve("火星系统数据").toSqlPredicate());
        assertEquals("`OBJECT` = 'M1'",
            AstronomyObjectCodeResolver.resolve("火卫一数据").toSqlPredicate());
        assertEquals("`OBJECT` = 'P0'",
            AstronomyObjectCodeResolver.resolve("冥王星本体数据").toSqlPredicate());
    }

    @Test
    void understandsChineseSatelliteNumbers() {
        assertEquals("`OBJECT` = 'S6'",
            AstronomyObjectCodeResolver.resolve("土卫六的数据").toSqlPredicate());
        assertEquals("`OBJECT` = 'J11'",
            AstronomyObjectCodeResolver.resolve("木卫十一的数据").toSqlPredicate());
    }

    @Test
    void leavesUnrelatedTasksUntouched() {
        String sql = "SELECT COUNT(*) FROM `observation_image`.`total`;";
        assertNull(AstronomyObjectCodeResolver.resolve("统计全部记录"));
        assertEquals(sql,
            AstronomyObjectCodeResolver.applyResolvedObjectFilter(sql, "统计全部记录"));
    }

    @Test
    void promptExplicitlyForbidsChineseObjectMatching() {
        String guidance = AstronomyObjectCodeResolver.buildPromptGuidance("给我所有土星的数据");
        assertTrue(guidance.contains("禁止对 OBJECT 使用"));
        assertTrue(guidance.contains("`OBJECT` LIKE 'S%'"));
    }
}
