package mie.astronomy.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ObservationReportServiceTest {
    @TempDir Path root;

    @Test void categorizesDocumentsAndReadsNewFilesWithoutRestart() throws Exception {
        var service = new ObservationReportService(root.toString());
        assertEquals(2, service.stations().size());
        assertTrue(service.list("xinglong").isEmpty());
        Path year = Files.createDirectories(root.resolve("兴隆观测基地/2026"));
        Files.write(year.resolve("观测纲要 #1.DOCX"), new byte[]{1, 2, 3});
        Files.writeString(year.resolve("记录.md"), "# 天然卫星");
        Files.writeString(year.resolve("~$记录.docx"), "temporary");
        Files.writeString(year.resolve("private.yml"), "hidden");
        Files.writeString(root.resolve("云南天文台/同名.pdf"), "PDF");
        assertEquals(2, service.list("xinglong").size());
        assertEquals(1, service.list("yunnan").size());
        assertEquals(2, service.stations().get(1).count());
        var report = service.list("xinglong").stream().filter(r -> r.type().equals("docx")).findFirst().orElseThrow();
        assertEquals("2026/观测纲要 #1.DOCX", report.path());
        assertEquals(3, report.size());
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(service.file("xinglong", report.path())));
        Files.delete(year.resolve("记录.md"));
        assertEquals(1, service.list("xinglong").size());
    }

    @Test void rejectsTraversalOtherStationsUnsupportedAndMissingFiles() throws Exception {
        var service = new ObservationReportService(root.toString());
        Files.writeString(root.resolve("云南天文台/secret.md"), "private");
        Files.writeString(root.resolve("兴隆观测基地/config.yml"), "private");
        for (String path : new String[]{"../云南天文台/secret.md", "../../outside.pdf", "C:/outside.pdf",
                "..\\云南天文台\\secret.md", "/outside.md", "missing.pdf", "config.yml", "", "bad\0.md"}) {
            assertThrows(ResponseStatusException.class, () -> service.file("xinglong", path), path);
        }
        assertThrows(ResponseStatusException.class, () -> service.list("unknown"));
    }
}
