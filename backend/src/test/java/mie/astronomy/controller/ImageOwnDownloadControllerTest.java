package mie.astronomy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mie.astronomy.entity.ImageOwn;
import mie.astronomy.service.ImageDownloadService;
import mie.astronomy.service.ImageOwnService;
import mie.astronomy.service.Impl.FileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ImageOwnDownloadControllerTest {
    @TempDir Path temp;

    @Test void httpWorkflowDownloadsExactFitsAndReportsPreflightErrorsAsJson() throws Exception {
        var images = mock(ImageOwnService.class);
        var root = temp.resolve("Observated_images");
        var files = new FileServiceImpl(root.toString());
        byte[] bytes = new byte[5760];
        new java.util.Random(42).nextBytes(bytes);
        Files.write(root.resolve("original.fits"), bytes);
        var image = new ImageOwn();
        image.setPwd("original.fits"); image.setObject("U0"); image.setTele("T1");
        image.setDateObs(LocalDateTime.parse("2024-11-03T23:59:59"));
        when(images.listByQuery(any())).thenReturn(List.of(image));
        when(images.listDownloadMetadata(any(), any())).thenReturn(List.of(image));
        var controller = new ImageOwnController();
        ReflectionTestUtils.setField(controller, "imageOwnService", images);
        ReflectionTestUtils.setField(controller, "downloads", new ImageDownloadService(images, files));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var json = new ObjectMapper();
        var result = mvc.perform(post("/api/image/own/downloads/groups")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.groups[0].date").value("2024-11-03"))
                .andReturn().getResponse().getContentAsString();
        var group = json.readTree(result).at("/data/groups/0/id").asText();
        result = mvc.perform(post("/api/image/own/downloads/prepare").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("query", Map.of("object", "U0"),
                        "groupIds", List.of(group), "includeCalibration", true, "matchedOnly", false))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.fileCount").value(1))
                .andExpect(jsonPath("$.data.totalBytes").value(5760))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(result).at("/data/token").asText();
        mvc.perform(get("/api/image/own/downloads/" + token + "/check"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        var archive = mvc.perform(get("/api/image/own/downloads/" + token))
                .andExpect(status().isOk()).andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Content-Disposition", containsString(".zip")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        try (var zip = new ZipInputStream(new java.io.ByteArrayInputStream(archive))) {
            assertEquals("original.fits", zip.getNextEntry().getName());
            assertArrayEquals(bytes, zip.readAllBytes());
            assertNull(zip.getNextEntry());
        }
        mvc.perform(post("/api/image/own/metadata/export").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("original.fits")));
        mvc.perform(post("/api/image/own/download").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"));
        Files.delete(root.resolve("original.fits"));
        mvc.perform(get("/api/image/own/downloads/" + token + "/check"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(containsString("重新准备")));
        mvc.perform(get("/api/image/own/downloads/unknown"))
                .andExpect(status().isGone()).andExpect(jsonPath("$.message").value(containsString("过期")));
        mvc.perform(post("/api/image/own/downloads/prepare")
                .contentType(MediaType.APPLICATION_JSON).content("{\"query\":{},\"groupIds\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("选择")));
    }
}
