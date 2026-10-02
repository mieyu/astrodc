package mie.astronomy.controller;

import mie.astronomy.service.ObservationReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

class ObservationReportControllerTest {
    @TempDir Path root;

    @Test void servesExactBytesWithChineseFilenameAndRejectsInvalidPaths() throws Exception {
        var service = new ObservationReportService(root.toString());
        var mvc = MockMvcBuilders.standaloneSetup(new ObservationReportController(service)).build();
        byte[] bytes = new byte[]{0, 1, 2, 99, -1};
        Files.write(root.resolve("兴隆观测基地/观测报告.docx"), bytes);
        mvc.perform(get("/api/observation-reports/stations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].name").value("兴隆观测基地"))
                .andExpect(jsonPath("$.data[1].count").value(1));
        mvc.perform(get("/api/observation-reports/xinglong/file").param("path", "观测报告.docx"))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes))
                .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''")))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/observation-reports/xinglong/file").param("path", "../云南天文台/private.pdf"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/observation-reports/xinglong/file").param("path", "missing.md"))
                .andExpect(status().isNotFound());
    }
}
