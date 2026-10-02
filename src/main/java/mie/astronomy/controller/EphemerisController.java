package mie.astronomy.controller;

import jakarta.servlet.http.HttpServletResponse;
import mie.astronomy.dto.EphemerisCalculateRequest;
import mie.astronomy.dto.EphemerisChartRequest;
import mie.astronomy.service.EphemerisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/ephemeris")
public class EphemerisController {

    private static final Logger log = LoggerFactory.getLogger(EphemerisController.class);

    private final EphemerisService ephemerisService;

    public EphemerisController(EphemerisService ephemerisService) {
        this.ephemerisService = ephemerisService;
    }

    @PostMapping("/calculate")
    public Map<String, Object> calculate(@RequestBody EphemerisCalculateRequest req) {
        return ephemerisService.calculate(req);
    }

    @PostMapping("/download_chart")
    public Map<String, Object> downloadChart(@RequestBody EphemerisChartRequest req) {
        return ephemerisService.downloadChart(req);
    }

    @GetMapping("/get_file")
    public ResponseEntity<?> getFile(@RequestParam("path") String path,
                                     @RequestParam(value = "filename", required = false) String filename,
                                     HttpServletResponse response) {
        // 1. 拒绝包含 .. 的路径(防穿越)
        if (path == null || path.contains("..")) {
            return ResponseEntity.status(404).body("File not found");
        }

        // 2. normalize 后必须仍在白名单目录下
        Path requested;
        try {
            requested = Paths.get(path).toAbsolutePath().normalize();
        } catch (Exception e) {
            return ResponseEntity.status(404).body("File not found");
        }

        Path allowedDir = Paths.get(ephemerisService.getChartCacheDir()).toAbsolutePath().normalize();
        if (!requested.startsWith(allowedDir)) {
            log.warn("拒绝越界文件访问: {} (allowed dir: {})", requested, allowedDir);
            return ResponseEntity.status(404).body("File not found");
        }

        if (!Files.exists(requested) || !Files.isRegularFile(requested)) {
            return ResponseEntity.status(404).body("File not found");
        }

        // 3. 流式回写
        String downloadName = (filename == null || filename.isBlank())
                ? requested.getFileName().toString()
                : filename;
        String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");

        try (InputStream in = Files.newInputStream(requested)) {
            response.setContentType(MediaType.IMAGE_GIF_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + downloadName + "\"; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(Files.size(requested));
            StreamUtils.copy(in, response.getOutputStream());
            response.flushBuffer();
            return null;
        } catch (IOException e) {
            log.error("文件回写失败: {}", requested, e);
            return ResponseEntity.status(500).body("Failed to read file");
        }
    }
}
